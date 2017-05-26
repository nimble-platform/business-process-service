/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.utility.HibernateUtility;

import java.util.List;

/**
 * @author yildiray
 */
public class DAOUtility {

    public static ProcessDAO getProcessDAOByID(String processID) {
        String query = "select bp from ProcessDAO bp where ( bp.processID ='" + processID + "') ";
        List<ProcessDAO> resultSet = (List<ProcessDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet.get(0);
    }

    public static ProcessApplicationConfigurationsDAO getProcessApplicationConfigurationsDAOByPartnerID(String partnerID) {
        String query = "select conf from ProcessApplicationConfigurationsDAO conf where ( conf.partnerID ='" + partnerID + "') ";
        List<ProcessApplicationConfigurationsDAO> resultSet = (List<ProcessApplicationConfigurationsDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet.get(0);
    }

    public static ProcessPreferencesDAO getProcessPreferencesDAOByPartnerID(String partnerID) {
        String query = "select conf from ProcessPreferencesDAO conf where ( conf.partnerID ='" + partnerID + "') ";
        List<ProcessPreferencesDAO> resultSet = (List<ProcessPreferencesDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet.get(0);
    }

    public static List<ProcessDAO> getProcessDAOs() {
        String query = "select bp from ProcessDAO bp ";
        List<ProcessDAO> resultSet = (List<ProcessDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet;
    }

    public static ProcessDocumentDAO getProcessDocument(String documentID) {
        String query = "select document from ProcessDocumentDAO document where ( ";
        query += " document.documentID ='" + documentID + "' ";
        query += " ) ";
        List<ProcessDocumentDAO> resultSet = (List<ProcessDocumentDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet.get(0);
    }

    public static List<ProcessDocumentDAO> getProcessDocuments(String partnerID, String type) {
        return getProcessDocuments(partnerID, type, null, null);
    }

    public static List<ProcessDocumentDAO> getProcessDocuments(String partnerID, String type, String source) {
        return getProcessDocuments(partnerID, type, null, source);
    }

    public static List<ProcessDocumentDAO> getProcessDocuments(String partnerID, String type, String status, String source) {
        String query = "select document from ProcessDocumentDAO document where ( ";

        if (source != null && partnerID != null) {
            String attribute = source.equals("SENT") ? "initiatorID" : "responderID";
            query += " document." + attribute + " ='" + partnerID + "' ";
        } else if (source == null && partnerID != null) query += " (document.initiatorID ='" + partnerID + "' or document.responderID ='" + partnerID + "') ";

        if (type != null) query += " and document.type ='" + type + "' ";

        if (status != null) query += " and document.status ='" + status + "' ";
        query += " ) ";
        List<ProcessDocumentDAO> resultSet = (List<ProcessDocumentDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet;
    }

    public static ProcessInstanceDAO getProcessIntanceDAOByID(String processInstanceID) {
        String query = "select processinstance from ProcessInstanceDAO processinstance where ( processinstance.processInstanceID ='" + processInstanceID + "') ";
        List<ProcessInstanceDAO> resultSet = (List<ProcessInstanceDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet.get(0);
    }
}
