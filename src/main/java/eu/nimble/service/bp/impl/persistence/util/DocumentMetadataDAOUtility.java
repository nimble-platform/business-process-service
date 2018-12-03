package eu.nimble.service.bp.impl.persistence.util;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;

import java.util.List;

/**
 * Created by suat on 16-Oct-18.
 */
public class DocumentMetadataDAOUtility {
    public static ProcessDocumentMetadataDAO getDocumentOfTheOtherParty(String processInstanceId, String thisPartyId) {
        List<ProcessDocumentMetadataDAO> documentMetadataDAOs = DAOUtility.getProcessDocumentMetadataByProcessInstanceID(processInstanceId);
        // if this party is the initiator party, return the second document metadata
        if(documentMetadataDAOs.get(0).getInitiatorID().contentEquals(thisPartyId)) {
            if(documentMetadataDAOs.size() > 1) {
                return documentMetadataDAOs.get(1);
            } else {
                return null;
            }
        } else {
            return documentMetadataDAOs.get(0);
        }
    }

    public static String getTradingPartnerId(String processInstanceId, String thisPartyId) {
        ProcessDocumentMetadataDAO firstDocumentMetadataDAO = DAOUtility.getProcessDocumentMetadataByProcessInstanceID(processInstanceId).get(0);
        return getTradingPartnerId(firstDocumentMetadataDAO, thisPartyId);
    }

    public static String getTradingPartnerId(ProcessDocumentMetadataDAO documentMetadata, String thisPartyId) {
        if(documentMetadata.getInitiatorID().contentEquals(thisPartyId)) {
            return documentMetadata.getResponderID();
        } else {
            return documentMetadata.getInitiatorID();
        }
    }
}
