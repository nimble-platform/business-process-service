/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.persistence.util;

import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.model.statistics.BusinessProcessCount;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yildiray
 */
@Component
public class DAOUtility {

    private static final Logger logger = LoggerFactory.getLogger(DAOUtility.class);

    private static IdentityClientTyped identityClient;

    @Autowired
    public void setIdentityClient(IdentityClientTyped identityClient){
        DAOUtility.identityClient = identityClient;
    }

    public static List<ProcessConfigurationDAO> getProcessConfigurationDAOByPartnerID(String partnerID) {
//        String query = "select conf from ProcessConfigurationDAO conf where ( conf.partnerID ='" + partnerID + "') ";
//        List<ProcessConfigurationDAO> resultSet = (List<ProcessConfigurationDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<ProcessConfigurationDAO> resultSet = SpringBridge.getInstance().getBusinessProcessRepository().getProcessConfigurations(partnerID);
        return resultSet;
    }

    public static ProcessPreferencesDAO getProcessPreferencesDAOByPartnerID(String partnerID) {
//        String query = "select conf from ProcessPreferencesDAO conf where ( conf.partnerID ='" + partnerID + "') ";
//        List<ProcessPreferencesDAO> resultSet = (List<ProcessPreferencesDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<ProcessPreferencesDAO> resultSet = SpringBridge.getInstance().getBusinessProcessRepository().getProcessPreferences(partnerID);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }

    public static ProcessDocumentMetadataDAO getProcessDocumentMetadata(String documentID) {

//        String query = "select document from ProcessDocumentMetadataDAO document where ( ";
//        query += " document.documentID ='" + documentID + "' ";
//        query += " ) ";
//        List<ProcessDocumentMetadataDAO> resultSet = (List<ProcessDocumentMetadataDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<ProcessDocumentMetadataDAO> resultSet = SpringBridge.getInstance().getProcessDocumentMetadataDAORepository().findByDocumentID(documentID);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadataByProcessInstanceID(String processInstanceID) {
//        String query = "select document from ProcessDocumentMetadataDAO document where ( ";
//        query += " document.processInstanceID ='" + processInstanceID + "' ";
//        query += " ) ORDER BY document.submissionDate ASC";
//        List<ProcessDocumentMetadataDAO> resultSet = (List<ProcessDocumentMetadataDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<ProcessDocumentMetadataDAO> resultSet = SpringBridge.getInstance().getProcessDocumentMetadataDAORepository().findByProcessInstanceIDOrderBySubmissionDateAsc(processInstanceID);
        return resultSet;
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadata(String partnerID, String type) {
        return getProcessDocumentMetadata(partnerID, type, null, null);
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadata(String partnerID, String type, String source) {
        return getProcessDocumentMetadata(partnerID, type, null, source);
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadata(String partnerID, String type, String status, String source) {
        List<String> parameterNames = new ArrayList<>();
        List<String> parameterValues = new ArrayList<>();
        String query = "select document from ProcessDocumentMetadataDAO document where ( ";

        if (source != null && partnerID != null) {
            String attribute = source.equals("SENT") ? "initiatorID" : "responderID";
            query += " document." + attribute + " = :partnerId ";
            parameterNames.add("partnerId");
            parameterValues.add(partnerID);

        } else if (source == null && partnerID != null) {
            query += " (document.initiatorID = :partnerId or document.responderID = :partnerId) ";
            parameterNames.add("partnerId");
            parameterValues.add(partnerID);
        }

        if (type != null) {
            query += " and document.type = '" + DocumentType.valueOf(type).toString() + "' ";
        }

        if (status != null) {
            query += " and document.status = '" + ProcessDocumentStatus.valueOf(status).toString() + "'";
        }
        query += " ) ";
//        List<ProcessDocumentMetadataDAO> resultSet = (List<ProcessDocumentMetadataDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<ProcessDocumentMetadataDAO> resultSet = SpringBridge.getInstance().getBusinessProcessRepository().getEntities(query, parameterNames.toArray(new String[parameterNames.size()]), parameterValues.toArray(new String[parameterValues.size()]));
        return resultSet;
    }


    public static List<String> getDocumentIds(Integer partyId, List<String> documentTypes, String role, String startDateStr, String endDateStr, String status) {
        DocumentMetadataQuery query = getDocumentMetadataQuery(partyId, documentTypes, role, startDateStr, endDateStr, status, DocumentMetadataQueryType.DOCUMENT_IDS);
//        List<String> documentIds = (List<String>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<String> documentIds = SpringBridge.getInstance().getBusinessProcessRepository().getEntities(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray());
        return documentIds;
    }

    public static int getTransactionCount(Integer partyId, List<String> documentTypes, String role, String startDateStr, String endDateStr, String status) {
        DocumentMetadataQuery query = getDocumentMetadataQuery(partyId, documentTypes, role, startDateStr, endDateStr, status, DocumentMetadataQueryType.TOTAL_TRANSACTION_COUNT);
//        int count = ((Long) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query)).intValue();
        int count = ((Long) SpringBridge.getInstance().getBusinessProcessRepository().getSingleEntity(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray())).intValue();
        return count;
    }

    public static BusinessProcessCount getGroupTransactionCounts(Integer partyId, String startDateStr, String endDateStr,String role,String bearerToken) {
        DocumentMetadataQuery query = getDocumentMetadataQuery(partyId, new ArrayList<>(), role, startDateStr, endDateStr, null, DocumentMetadataQueryType.GROUPED_TRANSACTION_COUNT);
//        List<Object> results = (List<Object>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<Object> results = SpringBridge.getInstance().getBusinessProcessRepository().getEntities(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray());

        BusinessProcessCount counts = new BusinessProcessCount();
        for(Object result : results) {
            Object[] resultItems = (Object[]) result;
            PartyType partyType = null;
            try {
                partyType = identityClient.getParty(bearerToken,(String) resultItems[0]);
            } catch (IOException e) {
                String msg = String.format("Failed to get transaction counts for party: %s, role: %s", partyId, role);
                logger.error("msg");
                throw new RuntimeException(msg, e);
            }
            counts.addCount((String) resultItems[0], resultItems[1].toString(), resultItems[2].toString(), (Long) resultItems[3],partyType.getPartyName().get(0).getName().getValue());
        }
        return counts;
    }

    public static DocumentMetadataQuery getDocumentMetadataQuery(Integer partyId, List<String> documentTypes, String role, String startDateStr, String endDateStr, String status, DocumentMetadataQueryType queryType) {
        DocumentMetadataQuery query = new DocumentMetadataQuery();
        List<String> parameterNames =query.parameterNames;
        List<Object> parameterValues = query.parameterValues;

        String queryStr = null;
        if(queryType.equals(DocumentMetadataQueryType.TOTAL_TRANSACTION_COUNT)) {
            queryStr = "select count(*) from ProcessDocumentMetadataDAO documentMetadata ";
        } else if(queryType.equals(DocumentMetadataQueryType.DOCUMENT_IDS)){
            queryStr = "select documentMetadata.documentID from ProcessDocumentMetadataDAO documentMetadata ";
        } else if(queryType.equals(DocumentMetadataQueryType.GROUPED_TRANSACTION_COUNT)) {
            queryStr = "select documentMetadata.initiatorID, documentMetadata.type, documentMetadata.status, count(*) from ProcessDocumentMetadataDAO documentMetadata ";
        }

        DateTimeFormatter sourceFormatter = DateTimeFormat.forPattern("dd-MM-yyyy");
        DateTimeFormatter bpFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

        boolean filterExists = false;

        if (partyId != null) {
            String attribute = role.equals(RoleType.BUYER.toString()) ? "initiatorID" : "responderID";
            queryStr += " where documentMetadata." + attribute + " = :partyId ";
            filterExists = true;

            parameterNames.add("partyId");
            parameterValues.add(partyId.toString());
        }

        if(startDateStr != null || endDateStr != null){
            if(!filterExists){
                queryStr += " where";
            }
            else {
                queryStr += " and";
            }

            if(startDateStr != null && endDateStr != null){
                DateTime startDate = sourceFormatter.parseDateTime(startDateStr);
                DateTime endDate = sourceFormatter.parseDateTime(endDateStr);
                endDate = endDate.plusDays(1).minusMillis(1);
                queryStr += " documentMetadata.submissionDate between :startTime and :endTime";

                parameterNames.add("startTime");
                parameterValues.add(bpFormatter.print(startDate));
                parameterNames.add("endTime");
                parameterValues.add(bpFormatter.print(endDate));
            }
            else if(startDateStr != null) {
                DateTime startDate = sourceFormatter.parseDateTime(startDateStr);
                queryStr += " documentMetadata.submissionDate >= :startTime";

                parameterNames.add("startTime");
                parameterValues.add(bpFormatter.print(startDate));
            }
            else {
                DateTime endDate = sourceFormatter.parseDateTime(endDateStr);
                endDate = endDate.plusDays(1).minusMillis(1);
                queryStr += " documentMetadata.submissionDate <= :endTime";

                parameterNames.add("endTime");
                parameterValues.add(bpFormatter.print(endDate));
            }
            filterExists = true;
        }

        if(documentTypes.size() > 0) {
            if (!filterExists) {
                queryStr += " where (";
            } else {
                queryStr += " and(";
            }
            for (int i = 0; i < documentTypes.size() - 1; i++) {
                queryStr += " documentMetadata.type = '" + DocumentType.valueOf(documentTypes.get(i)).toString() + "' or";
            }
            queryStr += " documentMetadata.type = '" + DocumentType.valueOf(documentTypes.get(documentTypes.size() - 1)).toString() + "')";
            filterExists = true;
        }

        if(status != null) {
            if (!filterExists) {
                queryStr += " where ";
            } else {
                queryStr += " and ";
            }
            queryStr += " documentMetadata.status = '" + ProcessDocumentStatus.valueOf(status).toString() + "'";
        }

        if(queryType.equals(DocumentMetadataQueryType.GROUPED_TRANSACTION_COUNT)) {
            queryStr += " group by documentMetadata.initiatorID, documentMetadata.type, documentMetadata.status";
        }

        query.query = queryStr;
        return query;
    }

    public static ProcessInstanceDAO getProcessInstanceDAOByID(String processInstanceID) {
//        String query = "select processinstance from ProcessInstanceDAO processinstance where ( processinstance.processInstanceID ='" + processInstanceID + "') ";
//        List<ProcessInstanceDAO> resultSet = (List<ProcessInstanceDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<ProcessInstanceDAO> resultSet = SpringBridge.getInstance().getProcessInstanceDAORepository().findByProcessInstanceID(processInstanceID);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }

    public static ProcessDAO getProcessDAOByID(String processID) {
//        String query = "select process from ProcessDAO process where ( process.processID ='" + processID + "') ";
//        List<ProcessDAO> resultSet = (List<ProcessDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<ProcessDAO> resultSet = SpringBridge.getInstance().getProcessDAORepository().findByProcessID(processID);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }

    public static List<ProcessDAO> getProcessDAOs() {
//        String query = "select process from ProcessDAO process ";
//        List<ProcessDAO> resultSet = (List<ProcessDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<ProcessDAO> resultSet = SpringBridge.getInstance().getProcessDAORepository().findAll();
        return resultSet;
    }

    public static ProcessConfigurationDAO getProcessConfiguration(String partnerID, String processID, ProcessConfiguration.RoleTypeEnum roleType) {
//        String query = "select conf from ProcessConfigurationDAO conf where ( conf.partnerID ='" + partnerID + "' and conf.processID ='" + processID + "' ) ";
//        List<ProcessConfigurationDAO> resultSet = (List<ProcessConfigurationDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        List<ProcessConfigurationDAO> resultSet = SpringBridge.getInstance().getBusinessProcessRepository().getProcessConfigurations(partnerID, processID);
        if(resultSet.size() == 0) {
            return null;
        }
        for(ProcessConfigurationDAO processConfigurationDAO : resultSet) {
            if(processConfigurationDAO.getRoleType().value().equals(roleType.toString())) {
                return processConfigurationDAO;
            }
        }
        return null;
    }

    public static List<String> getAllProcessInstanceIDs(String processInstanceID){
        List<String> processInstanceIDs = new ArrayList<>();
//        String query = "FROM ProcessInstanceDAO piDAO WHERE piDAO.processInstanceID = ?";
//        ProcessInstanceDAO processInstanceDAO = GenericJPARepositoryImpl.getInstance("bp-data-model").load(query,processInstanceID);
        ProcessInstanceDAO processInstanceDAO = SpringBridge.getInstance().getProcessInstanceDAORepository().findByProcessInstanceID(processInstanceID).get(0);
        processInstanceIDs.add(0,processInstanceID);
        while (processInstanceDAO.getPrecedingProcess() != null){
//            processInstanceDAO = GenericJPARepositoryImpl.getInstance("bp-data-model").load(query,processInstanceDAO.getPrecedingProcess().getProcessInstanceID());
            processInstanceDAO = SpringBridge.getInstance().getProcessInstanceDAORepository().findByProcessInstanceID(processInstanceDAO.getPrecedingProcess().getProcessInstanceID()).get(0);
            processInstanceIDs.add(0,processInstanceDAO.getProcessInstanceID());
        }
        return processInstanceIDs;
    }

    private enum DocumentMetadataQueryType {
        DOCUMENT_IDS, TOTAL_TRANSACTION_COUNT, GROUPED_TRANSACTION_COUNT
    }

    private static class DocumentMetadataQuery {
        private String query;
        private List<String> parameterNames = new ArrayList<>();
        private List<Object> parameterValues = new ArrayList<>();
    }
}
