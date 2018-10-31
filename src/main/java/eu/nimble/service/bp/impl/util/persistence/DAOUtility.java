/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.TrustServiceController;
import eu.nimble.service.bp.impl.model.statistics.BusinessProcessCount;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.HibernateUtility;
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
        String query = "select conf from ProcessConfigurationDAO conf where ( conf.partnerID ='" + partnerID + "') ";
        List<ProcessConfigurationDAO> resultSet = (List<ProcessConfigurationDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);

        return resultSet;
    }

    public static ProcessPreferencesDAO getProcessPreferencesDAOByPartnerID(String partnerID) {
        String query = "select conf from ProcessPreferencesDAO conf where ( conf.partnerID ='" + partnerID + "') ";
        List<ProcessPreferencesDAO> resultSet = (List<ProcessPreferencesDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }

    public static ProcessDocumentMetadataDAO getProcessDocumentMetadata(String documentID) {
        String query = "select document from ProcessDocumentMetadataDAO document where ( ";
        query += " document.documentID ='" + documentID + "' ";
        query += " ) ";
        List<ProcessDocumentMetadataDAO> resultSet = (List<ProcessDocumentMetadataDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadataByProcessInstanceID(String processInstanceID) {
        String query = "select document from ProcessDocumentMetadataDAO document where ( ";
        query += " document.processInstanceID ='" + processInstanceID + "' ";
        query += " ) ORDER BY document.submissionDate ASC";
        List<ProcessDocumentMetadataDAO> resultSet = (List<ProcessDocumentMetadataDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);

        return resultSet;
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadata(String partnerID, String type) {
        return getProcessDocumentMetadata(partnerID, type, null, null);
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadata(String partnerID, String type, String source) {
        return getProcessDocumentMetadata(partnerID, type, null, source);
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadata(String partnerID, String type, String status, String source) {
        String query = "select document from ProcessDocumentMetadataDAO document where ( ";

        if (source != null && partnerID != null) {
            String attribute = source.equals("SENT") ? "initiatorID" : "responderID";
            query += " document." + attribute + " ='" + partnerID + "' ";
        } else if (source == null && partnerID != null) query += " (document.initiatorID ='" + partnerID + "' or document.responderID ='" + partnerID + "') ";

        if (type != null) query += " and document.type ='" + type + "' ";

        if (status != null) query += " and document.status ='" + status + "' ";
        query += " ) ";
        List<ProcessDocumentMetadataDAO> resultSet = (List<ProcessDocumentMetadataDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        return resultSet;
    }


    public static List<String> getDocumentIds(Integer partyId, List<String> documentTypes, String role, String startDateStr, String endDateStr, String status) {
        String query = getDocumentMetadataQuery(partyId, documentTypes, role, startDateStr, endDateStr, status, DocumentMetadataQuery.DOCUMENT_IDS);
        List<String> documentIds = (List<String>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        return documentIds;
    }

    public static int getTransactionCount(Integer partyId, List<String> documentTypes, String role, String startDateStr, String endDateStr, String status) {
        String query = getDocumentMetadataQuery(partyId, documentTypes, role, startDateStr, endDateStr, status, DocumentMetadataQuery.TOTAL_TRANSACTION_COUNT);
        int count = ((Long) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query)).intValue();
        return count;
    }

    public static BusinessProcessCount getGroupTransactionCounts(Integer partyId, String startDateStr, String endDateStr,String role,String bearerToken) {
        String query = getDocumentMetadataQuery(partyId, new ArrayList<>(), role, startDateStr, endDateStr, null, DocumentMetadataQuery.GROUPED_TRANSACTION_COUNT);
        List<Object> results = (List<Object>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);

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
            counts.addCount((String) resultItems[0], resultItems[1].toString(), resultItems[2].toString(), (Long) resultItems[3],partyType.getName());
        }
        return counts;
    }

    public static String getDocumentMetadataQuery(Integer partyId, List<String> documentTypes, String role, String startDateStr, String endDateStr, String status, DocumentMetadataQuery queryType) {
        String query = null;
        if(queryType.equals(DocumentMetadataQuery.TOTAL_TRANSACTION_COUNT)) {
            query = "select count(*) from ProcessDocumentMetadataDAO documentMetadata ";
        } else if(queryType.equals(DocumentMetadataQuery.DOCUMENT_IDS)){
            query = "select documentMetadata.documentID from ProcessDocumentMetadataDAO documentMetadata ";
        } else if(queryType.equals(DocumentMetadataQuery.GROUPED_TRANSACTION_COUNT)) {
            query = "select documentMetadata.initiatorID, documentMetadata.type, documentMetadata.status, count(*) from ProcessDocumentMetadataDAO documentMetadata ";
        }

        DateTimeFormatter sourceFormatter = DateTimeFormat.forPattern("dd-MM-yyyy");
        DateTimeFormatter bpFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

        boolean filterExists = false;

        if (partyId != null) {
            String attribute = role.equals(RoleType.BUYER.toString()) ? "initiatorID" : "responderID";
            query += " where documentMetadata." + attribute + " ='" + partyId + "' ";
            filterExists = true;
        }

        if(startDateStr != null || endDateStr != null){
            if(!filterExists){
                query += " where";
            }
            else {
                query += " and";
            }

            if(startDateStr != null && endDateStr != null){
                DateTime startDate = sourceFormatter.parseDateTime(startDateStr);
                DateTime endDate = sourceFormatter.parseDateTime(endDateStr);
                endDate = endDate.plusDays(1).minusMillis(1);
                query += " documentMetadata.submissionDate between '" + bpFormatter.print(startDate) + "' and '" + bpFormatter.print(endDate) + "'";

            }
            else if(startDateStr != null) {
                DateTime startDate = sourceFormatter.parseDateTime(startDateStr);
                query += " documentMetadata.submissionDate >= '" + bpFormatter.print(startDate) + "'";

            }
            else {
                DateTime endDate = sourceFormatter.parseDateTime(endDateStr);
                endDate = endDate.plusDays(1).minusMillis(1);
                query += " documentMetadata.submissionDate <= '" + bpFormatter.print(endDate) + "'";

            }
            filterExists = true;
        }

        if(documentTypes.size() > 0) {
            if (!filterExists) {
                query += " where (";
            } else {
                query += " and(";
            }
            for (int i = 0; i < documentTypes.size() - 1; i++) {
                query += " documentMetadata.type ='" + documentTypes.get(i).toString() + "' or";
            }
            query += " documentMetadata.type = '" + documentTypes.get(documentTypes.size() - 1).toString() + "')";
            filterExists = true;
        }

        if(status != null) {
            if (!filterExists) {
                query += " where ";
            } else {
                query += " and ";
            }
            query += " documentMetadata.status ='" + status + "'";
        }

        if(queryType.equals(DocumentMetadataQuery.GROUPED_TRANSACTION_COUNT)) {
            query += " group by documentMetadata.initiatorID, documentMetadata.type, documentMetadata.status";
        }

        return query;
    }

    public static ProcessInstanceDAO getProcessInstanceDAOByID(String processInstanceID) {
        String query = "select processinstance from ProcessInstanceDAO processinstance where ( processinstance.processInstanceID ='" + processInstanceID + "') ";
        List<ProcessInstanceDAO> resultSet = (List<ProcessInstanceDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }

    public static ProcessDAO getProcessDAOByID(String processID) {
        String query = "select process from ProcessDAO process where ( process.processID ='" + processID + "') ";
        List<ProcessDAO> resultSet = (List<ProcessDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }

    public static List<ProcessDAO> getProcessDAOs() {
        String query = "select process from ProcessDAO process ";
        List<ProcessDAO> resultSet = (List<ProcessDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        return resultSet;
    }

    public static ProcessConfigurationDAO getProcessConfiguration(String partnerID, String processID, ProcessConfiguration.RoleTypeEnum roleType) {
        String query = "select conf from ProcessConfigurationDAO conf where ( conf.partnerID ='" + partnerID + "' and conf.processID ='" + processID + "' ) ";
        List<ProcessConfigurationDAO> resultSet = (List<ProcessConfigurationDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
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
        String query = "FROM ProcessInstanceDAO piDAO WHERE piDAO.processInstanceID = ?";
        ProcessInstanceDAO processInstanceDAO = HibernateUtility.getInstance("bp-data-model").load(query,processInstanceID);
        processInstanceIDs.add(0,processInstanceID);
        while (processInstanceDAO.getPrecedingProcess() != null){
            processInstanceDAO = HibernateUtility.getInstance("bp-data-model").load(query,processInstanceDAO.getPrecedingProcess().getProcessInstanceID());
            processInstanceIDs.add(0,processInstanceDAO.getProcessInstanceID());
        }
        return processInstanceIDs;
    }

    private enum DocumentMetadataQuery {
        DOCUMENT_IDS, TOTAL_TRANSACTION_COUNT, GROUPED_TRANSACTION_COUNT
    }
}
