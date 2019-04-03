package eu.nimble.service.bp.impl.util.persistence.catalogue;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.impl.model.statistics.NonOrderedProducts;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CompletedTaskType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by suat on 12-Jun-18.
 */
public class StatisticsPersistenceUtility {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsPersistenceUtility.class);

    public static long getActionRequiredProcessCount(String partyID,String role,Boolean archived){
        List<String> parameterNames = new ArrayList<>();
        List<Object> parameterValues = new ArrayList<>();
        String query = "SELECT metadataDAO.processInstanceID FROM " +
                "ProcessDocumentMetadataDAO metadataDAO,ProcessInstanceDAO instanceDAO,ProcessInstanceGroupDAO groupDAO join groupDAO.processInstanceIDsItems ids " +
                "WHERE groupDAO.archived = :archived AND " +
                "groupDAO.partyID = :partyId AND " +
                "metadataDAO.processInstanceID = ids.item AND " +
                "metadataDAO.processInstanceID = instanceDAO.processInstanceID AND " +
                "instanceDAO.status <> 'CANCELLED' AND instanceDAO.processID ";
        if(role.equals("seller")){
            query += " <> 'Fulfilment' AND metadataDAO.responderID = :partyId GROUP BY metadataDAO.processInstanceID HAVING count(*) = 1";
        }
        else{
            query += " = 'Fulfilment' AND metadataDAO.responderID = :partyId AND instanceDAO.status = 'STARTED' GROUP BY metadataDAO.processInstanceID HAVING count(*) = 1";
        }
        parameterNames.add("archived");
        parameterNames.add("partyId");
        parameterValues.add(archived);
        parameterValues.add(partyID);
        List<String> count = new JPARepositoryFactory().forBpRepository().getEntities(query, parameterNames.toArray(new String[parameterNames.size()]), parameterValues.toArray());
        return count.size();
    }

    public static double getTradingVolume(Integer partyId, String role, String startDate, String endDate, String status) {
        String query = "select sum(order_.anticipatedMonetaryTotal.payableAmount.value) from OrderType order_ where order_.anticipatedMonetaryTotal.payableAmount.value is not null";
        List<String> parameterNames = new ArrayList<>();
        List<Object> parameterValues = new ArrayList<>();

        if (partyId != null || role != null || startDate != null || endDate != null || status != null) {
            List<String> documentTypes = new ArrayList<>();
            documentTypes.add("ORDER");
            List<String> orderIds = ProcessDocumentMetadataDAOUtility.getDocumentIds(partyId, documentTypes, role, startDate, endDate, status);

            // no orders for the specified criteria
            if (orderIds.size() == 0) {
                logger.info("No orders for the specified criteria");
                return 0;
            }

            query += " and (";
            for (int i = 0; i < orderIds.size() - 1; i++) {
                query += " order_.ID = :id" + i + " or ";

                parameterNames.add("id" + i);
                parameterValues.add(orderIds.get(i));
            }
            query += " order_.ID = :id" + (orderIds.size()-1) + ")";

            parameterNames.add("id" + (orderIds.size()-1));
            parameterValues.add(orderIds.get(orderIds.size()-1));
        }

        double tradingVolume = ((BigDecimal) new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(query, parameterNames.toArray(new String[parameterNames.size()]), parameterValues.toArray())).doubleValue();
        return tradingVolume;
    }

    public static NonOrderedProducts getNonOrderedProducts(String bearerToken,Integer partyId) throws IOException {
        String query = "select distinct new list(partyIdentification.ID, item.manufacturersItemIdentification.ID, itemName.value) from ItemType item join item.manufacturerParty.partyIdentification partyIdentification JOIN item.name itemName" +
                " where item.transportationServiceDetails is null ";
        List<String> parameterNames = new ArrayList<>();
        List<Object> parameterValues = new ArrayList<>();

        if (partyId != null) {
            query += " and partyIdentification.ID = :partyId";
            parameterNames.add("partyId");
            parameterValues.add(partyId.toString());
        }

        query += " and item.manufacturersItemIdentification.ID not in " +
                "(select line.lineItem.item.manufacturersItemIdentification.ID from OrderType order_ join order_.orderLine line join line.lineItem.item.manufacturerParty.partyIdentification orderPartyIdentification " +
                " where orderPartyIdentification.ID = partyIdentification.ID) ";

        NonOrderedProducts nonOrderedProducts = new NonOrderedProducts();
        List<Object> results = new JPARepositoryFactory().forCatalogueRepository().getEntities(query, parameterNames.toArray(new String[parameterNames.size()]), parameterValues.toArray());
        for (Object result : results) {
            List<String> dataArray = (List<String>) result;
            // get party information from identity service
            PartyType party = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,dataArray.get(0));
            nonOrderedProducts.addProduct(dataArray.get(0), party.getPartyName().get(0).getName().getValue(), dataArray.get(1), dataArray.get(2));
        }

        return nonOrderedProducts;
    }

    public static List<PartyType> getInactiveCompanies(String bearerToken) throws IOException {
        // get active party ids
        // get parties for a process that have not completed yet. Therefore return only the initiatorID
        String query = "select docMetadata.initiatorID from ProcessDocumentMetadataDAO docMetadata where docMetadata.status = 'WAITINGRESPONSE'";

        Set<String> activePartyIds = new HashSet<>();
        List<String> results = new JPARepositoryFactory().forBpRepository().getEntities(query);

        activePartyIds.addAll(results);

        // get parties for a process that have completed already. Therefore return both the initiatorID and responderID
        query = "select distinct new list(docMetadata.initiatorID, docMetadata.responderID) from ProcessDocumentMetadataDAO docMetadata where docMetadata.status <> 'WAITINGRESPONSE'";
        List<List<String>> secondResults = new JPARepositoryFactory().forBpRepository().getEntities(query);
        for (List<String> processPartyIds : secondResults) {
            activePartyIds.add(processPartyIds.get(0));
            activePartyIds.add(processPartyIds.get(1));
        }

        // get inactive companies
        List<PartyType> inactiveParties = new ArrayList<>();
        InputStream parties = SpringBridge.getInstance().getiIdentityClientTyped().getAllPartyIds(bearerToken, new ArrayList<>()).body().asInputStream();
        ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser parser = factory.createParser(parties);
        JsonNode allParties = mapper.readTree(parser);
        Iterable<JsonNode> iterable = () -> allParties.elements();
        iterable.forEach(partyResult -> {
            JsonNode idNode = partyResult.get("identifier");
            if(idNode == null) {
                return;
            }
            String partyId = idNode.asText();
            if(!activePartyIds.contains(partyId)) {
                PartyType party = new PartyType();
                PartyIdentificationType partyIdentificationType = new PartyIdentificationType();
                partyIdentificationType.setID(partyId);
                party.setPartyIdentification(Arrays.asList(partyIdentificationType));
                TextType textType = new TextType();
                textType.setValue(partyResult.get("name").asText());
                textType.setLanguageID("en");
                PartyNameType partyNameType = new PartyNameType();
                partyNameType.setName(textType);
                party.setPartyName(Arrays.asList(partyNameType));
                inactiveParties.add(party);
            }
        });
        return inactiveParties;
    }

    public static double calculateAverageCollaborationTime(String partyID, String bearerToken){
        int numberOfCollaborations = 0;
        double totalTime = 0;
        QualifyingPartyType qualifyingParty = PartyPersistenceUtility.getQualifyingPartyType(partyID,bearerToken);
        for (CompletedTaskType completedTask:qualifyingParty.getCompletedTask()){
            if(completedTask.getPeriod().getEndDate() == null || completedTask.getPeriod().getEndTime() == null){
                continue;
            }
            Date startDate = completedTask.getPeriod().getStartDate().toGregorianCalendar().getTime();
            Date endDate = completedTask.getPeriod().getEndDate().toGregorianCalendar().getTime();
            Date startTime = completedTask.getPeriod().getStartTime().toGregorianCalendar().getTime();
            Date endTime = completedTask.getPeriod().getEndTime().toGregorianCalendar().getTime();

            numberOfCollaborations++;
            totalTime += ((endDate.getTime()-startDate.getTime())+(endTime.getTime()-startTime.getTime()))/86400000.0;
        }
        if(numberOfCollaborations == 0){
            return 0.0;
        }
        return totalTime/numberOfCollaborations;
    }

    public static double calculateAverageResponseTime(String partyID) throws Exception{

        int numberOfResponses = 0;
        double totalTime = 0;

        List<String> processInstanceIDs = ProcessDocumentMetadataDAOUtility.getProcessInstanceIds(partyID);

        for (String processInstanceID:processInstanceIDs){
                List<ProcessDocumentMetadataDAO> processDocumentMetadataDAOS = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(processInstanceID);
                if (processDocumentMetadataDAOS.size() != 2){
                    continue;
                }

                ProcessDocumentMetadataDAO docMetadata = processDocumentMetadataDAOS.get(1);
                ProcessDocumentMetadataDAO reqMetadata = processDocumentMetadataDAOS.get(0);

                Date startDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(reqMetadata.getSubmissionDate()).toGregorianCalendar().getTime();
                Date endDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(docMetadata.getSubmissionDate()).toGregorianCalendar().getTime();

                numberOfResponses++;
                totalTime += (endDate.getTime()-startDate.getTime())/86400000.0;
        }
        if(numberOfResponses == 0){
            return 0.0;
        }
        return totalTime/numberOfResponses;
    }
}