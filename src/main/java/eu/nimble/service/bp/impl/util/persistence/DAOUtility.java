/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.utility.HibernateUtility;

import java.util.List;

/**
 * @author yildiray
 */
public class DAOUtility {

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

    public static ProcessInstanceDAO getProcessIntanceDAOByID(String processInstanceID) {
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

    public static List<ProcessInstanceGroupDAO> getProcessInstanceGroupDAOs(String partyId, int offset, int limit, boolean archived) {
        String query = "select pig from ProcessInstanceGroupDAO pig where pig.archived = " + archived;
        if(partyId != null) {
            query += " and pig.ID ='" + partyId + "'";
        }
        List<ProcessInstanceGroupDAO> groups = (List<ProcessInstanceGroupDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query, offset, limit);
        return groups;
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAOByID(String groupID) {
        String query = "select pig from ProcessInstanceGroupDAO pig where ( pig.ID ='" + groupID+ "') ";
        ProcessInstanceGroupDAO group = (ProcessInstanceGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query);
        return group;
    }

    public static void deleteProcessInstanceGroupDAOByID(String groupID) {
        String query = "select pig from ProcessInstanceGroupDAO pig where ( pig.ID ='" + groupID+ "') ";
        ProcessInstanceGroupDAO group = (ProcessInstanceGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query);
        HibernateUtilityRef.getInstance("bp-data-model").delete(group);
    }

    public static void archiveAllGroupsForParty(String partyId) {
        String query = "update ProcessInstanceGroupDAO as pig set pig.archived = true WHERE pig.partyID = '" + partyId + "'";
        HibernateUtilityRef.getInstance("bp-data-model").executeUpdate(query);
    }

    public static void deleteArchivedGroupsForParty(String partyId) {
        String query = "delete ProcessInstanceGroupDAO as pig WHERE pig.archived = true and pig.partyID = '" + partyId + "'";
        HibernateUtilityRef.getInstance("bp-data-model").executeUpdate(query);
    }
}
