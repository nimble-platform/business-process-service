/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.utility.HibernateUtility;

import java.util.List;

/**
 * @author yildiray
 */
public class DAOUtility {

    public static List<ProcessApplicationConfigurationsDAO> getProcessApplicationConfigurationsDAOByPartnerID(String partnerID) {
        String query = "select conf from ProcessApplicationConfigurationsDAO conf where ( conf.partnerID ='" + partnerID + "') ";
        List<ProcessApplicationConfigurationsDAO> resultSet = (List<ProcessApplicationConfigurationsDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);

        return resultSet;
    }

    public static ProcessApplicationConfigurationsDAO getProcessApplicationConfigurationsDAOByPartnerID(String partnerID, String processID) {
        String query = "select conf from ProcessApplicationConfigurationsDAO conf where ( conf.partnerID ='" + partnerID + "' and conf.processID ='" + processID + "' ) ";
        List<ProcessApplicationConfigurationsDAO> resultSet = (List<ProcessApplicationConfigurationsDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }

    public static ProcessPreferencesDAO getProcessPreferencesDAOByPartnerID(String partnerID) {
        String query = "select conf from ProcessPreferencesDAO conf where ( conf.partnerID ='" + partnerID + "') ";
        List<ProcessPreferencesDAO> resultSet = (List<ProcessPreferencesDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }

    public static ProcessDocumentMetadataDAO getProcessDocumentMetadata(String documentID) {
        String query = "select document from ProcessDocumentMetadataDAO document where ( ";
        query += " document.documentID ='" + documentID + "' ";
        query += " ) ";
        List<ProcessDocumentMetadataDAO> resultSet = (List<ProcessDocumentMetadataDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
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
        List<ProcessDocumentMetadataDAO> resultSet = (List<ProcessDocumentMetadataDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet;
    }

    public static ProcessInstanceDAO getProcessIntanceDAOByID(String processInstanceID) {
        String query = "select processinstance from ProcessInstanceDAO processinstance where ( processinstance.processInstanceID ='" + processInstanceID + "') ";
        List<ProcessInstanceDAO> resultSet = (List<ProcessInstanceDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        if(resultSet.size() == 0) {
            return null;
        }
        return resultSet.get(0);
    }
}
