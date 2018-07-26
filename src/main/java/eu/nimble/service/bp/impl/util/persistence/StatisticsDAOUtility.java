package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.service.bp.impl.model.statistics.NonOrderedProducts;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by suat on 12-Jun-18.
 */
public class StatisticsDAOUtility {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsDAOUtility.class);

    public static double getTradingVolume(Integer partyId, String role, String startDate, String endDate, String status) {
        String query = "select sum(order_.anticipatedMonetaryTotal.payableAmount.value) from OrderType order_ where order_.anticipatedMonetaryTotal.payableAmount.value is not null";
        if (partyId != null || role != null || startDate != null || endDate != null || status != null) {
            List<String> documentTypes = new ArrayList<>();
            documentTypes.add("ORDER");
            List<String> orderIds = DAOUtility.getDocumentIds(partyId, documentTypes, role, startDate, endDate, status);

            // no orders for the specified criteria
            if (orderIds.size() == 0) {
                logger.info("No orders for the specified criteria");
                return 0;
            }

            query += " and (";
            for (int i = 0; i < orderIds.size() - 1; i++) {
                query += " order_.ID = '" + orderIds.get(i) + "' or ";
            }
            query += " order_.ID = '" + orderIds.get(orderIds.size() - 1) + "')";
        }

        double tradingVolume = ((BigDecimal) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query)).doubleValue();
        return tradingVolume;
    }

    public static NonOrderedProducts getNonOrderedProducts(Integer partyId) {
        String query = "select distinct new list(item.manufacturerParty.ID, item.manufacturerParty.name, item.manufacturersItemIdentification.ID, item.name) from ItemType item " +
                " where item.transportationServiceDetails is null ";

        if (partyId != null) {
            query += " and item.manufacturerParty.ID = '" + partyId + "'";
        }

        query += " and item.manufacturersItemIdentification.ID not in " +
                "(select line.lineItem.item.manufacturersItemIdentification.ID from OrderType order_ join order_.orderLine line" +
                " where line.lineItem.item.manufacturerParty.ID = item.manufacturerParty.ID) ";

        NonOrderedProducts nonOrderedProducts = new NonOrderedProducts();
        List<Object> results = (List<Object>) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadAll(query);
        for (Object result : results) {
            List<String> dataArray = (List<String>) result;
            nonOrderedProducts.addProduct(dataArray.get(0), dataArray.get(1), dataArray.get(2), dataArray.get(3));
        }

        return nonOrderedProducts;
    }

    public static List<PartyType> getInactiveCompanies(String startdateStr, String endDateStr, String bearerToken) {
        // get active party ids
        // get parties for a process that have not completed yet. Therefore return only the initiatorID
        String query = "select docMetadata.initiatorID from ProcessDocumentMetadataDAO docMetadata where docMetadata.status = 'WAITINGRESPONSE'";
        if(startdateStr != null && endDateStr != null) {
            query = "select distinct new list(docMetadata.initiatorID, docMetadata.responderID) from ProcessDocumentMetadataDAO docMetadata where docMetadata.submissionDate ";
        }

        Set<String> activePartyIds = new HashSet<>();
        List<String> results = (List<String>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);

        activePartyIds.addAll(results);

        // get parties for a process that have completed already. Therefore return both the initiatorID and responderID
        query = "select distinct new list(docMetadata.initiatorID, docMetadata.responderID) from ProcessDocumentMetadataDAO docMetadata where docMetadata.status <> 'WAITINGRESPONSE'";
        List<List<String>> secondResults = (List<List<String>>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        for (List<String> processPartyIds : secondResults) {
            activePartyIds.add(processPartyIds.get(0));
            activePartyIds.add(processPartyIds.get(1));
        }

        // get inactive companies
        // TODO get also the names of the parties from the identity service
        Set<String> partyIds = SpringBridge.getInstance().getIdentityClient().getAllPartyIds(bearerToken, new ArrayList<>());

        List<PartyType> inactiveParties = new ArrayList<>();
        partyIds.removeAll(activePartyIds);
        partyIds.stream().forEach(partyId -> {
            PartyType party = new PartyType();
            party.setID(partyId);
            inactiveParties.add(party);
        });
        return inactiveParties;
    }
}
