/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util;

import eu.nimble.service.bp.hyperjaxb.model.BusinessProcessApplicationConfigurationsDAO;
import eu.nimble.service.bp.hyperjaxb.model.BusinessProcessDAO;
import eu.nimble.service.bp.hyperjaxb.model.BusinessProcessDocumentDAO;
import eu.nimble.service.bp.hyperjaxb.model.BusinessProcessPreferencesDAO;
import java.util.List;

/**
 *
 * @author yildiray
 */
public class DAOUtility {

	public static BusinessProcessDAO getBusinessProcessDAOByID(String businessProcessID) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public static BusinessProcessApplicationConfigurationsDAO getBusinessProcessApplicationConfigurationsDAOByPartnerID(String partnerID) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public static BusinessProcessPreferencesDAO getBusinessProcessPreferencesDAOByPartnerID(String partnerID) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public static List<BusinessProcessDAO> getBusinessProcessDAOs() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public static BusinessProcessDocumentDAO getBusinessProcessDocument(String documentID) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public static List<BusinessProcessDocumentDAO> getBusinessProcessDocuments(String partnerID, String typeID) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public static List<BusinessProcessDocumentDAO> getBusinessProcessDocuments(String partnerID, String typeID, String source) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	public static List<BusinessProcessDocumentDAO> getBusinessProcessDocuments(String partnerID, String typeID, String status, String source) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
