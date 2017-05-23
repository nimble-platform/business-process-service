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

    public static BusinessProcessDAO getBusinessProcessDAOByID(String businessProcessID) {
        String query = "select businessprocess from BusinessProcessDAO where ( businessprocess.businessProcessID ='" + businessProcessID + "') ";
        List<BusinessProcessDAO> resultSet = (List<BusinessProcessDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet.get(0);
    }

    public static BusinessProcessApplicationConfigurationsDAO getBusinessProcessApplicationConfigurationsDAOByPartnerID(String partnerID) {
        String query = "select conf from BusinessProcessApplicationConfigurationsDAO where ( conf.partnerID ='" + partnerID + "') ";
        List<BusinessProcessApplicationConfigurationsDAO> resultSet = (List<BusinessProcessApplicationConfigurationsDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet.get(0);
    }

    public static BusinessProcessPreferencesDAO getBusinessProcessPreferencesDAOByPartnerID(String partnerID) {
        String query = "select conf from BusinessProcessPreferencesDAO where ( conf.partnerID ='" + partnerID + "') ";
        List<BusinessProcessPreferencesDAO> resultSet = (List<BusinessProcessPreferencesDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet.get(0);
    }

    public static List<BusinessProcessDAO> getBusinessProcessDAOs() {
        String query = "select businessprocess from BusinessProcessDAO ";
        List<BusinessProcessDAO> resultSet = (List<BusinessProcessDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet;
    }

    public static BusinessProcessDocumentDAO getBusinessProcessDocument(String documentID) {
        String query = "select document from BusinessProcessDocumentDAO where ( ";
        query += " document.documentID ='" + documentID + "' ";
        query += " ) ";
        List<BusinessProcessDocumentDAO> resultSet = (List<BusinessProcessDocumentDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet.get(0);
    }

    public static List<BusinessProcessDocumentDAO> getBusinessProcessDocuments(String partnerID, String typeID) {
        return getBusinessProcessDocuments(partnerID, typeID, null, null);
    }

    public static List<BusinessProcessDocumentDAO> getBusinessProcessDocuments(String partnerID, String typeID, String source) {
        return getBusinessProcessDocuments(partnerID, typeID, null, source);
    }

    public static List<BusinessProcessDocumentDAO> getBusinessProcessDocuments(String partnerID, String typeID, String status, String source) {
        String query = "select document from BusinessProcessDocumentDAO where ( ";

        if (source != null && partnerID != null) {
            String attribute = source.equals("SENT") ? "initiatorID" : "responderID";
            query += " document." + attribute + " ='" + partnerID + "' ";
        } else if (source == null && partnerID != null) query += " (document.initiatorID ='" + partnerID + "' or document.responderID ='" + partnerID + "') ";

        if (typeID != null) query += " and document.typeID ='" + typeID + "' ";

        if (status != null) query += " and document.status ='" + status + "' ";
        query += " ) ";
        List<BusinessProcessDocumentDAO> resultSet = (List<BusinessProcessDocumentDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet;
    }

    public static BusinessProcessInstanceDAO getBusinessProcessIntanceDAOByID(String businessProcessInstanceID) {
        String query = "select processinstance from BusinessProcessInstanceDAO where ( processinstance.businessProcessInstanceID ='" + businessProcessInstanceID + "') ";
        List<BusinessProcessInstanceDAO> resultSet = (List<BusinessProcessInstanceDAO>) HibernateUtility.getInstance("bp-data-model").loadAll(query);
        return resultSet.get(0);
    }
}
