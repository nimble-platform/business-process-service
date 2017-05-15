package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.swagger.api.BusinessprocessApi;
import eu.nimble.service.bp.swagger.model.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Created by yildiray on 5/15/2017.
 */
public class BusinessProcessController implements BusinessprocessApi {
    @Override
    public ResponseEntity<ModelApiResponse> addBusinessProcessDefinition(BusinessProcess body) {
        return null;
    }

    @Override
    public ResponseEntity<ModelApiResponse> addBusinessProcessPartnerApplicationPreference(BusinessProcessApplicationConfigurations body) {
        return null;
    }

    @Override
    public ResponseEntity<ModelApiResponse> addBusinessProcessPartnerPreference(BusinessProcessPreferences body) {
        return null;
    }

    @Override
    public ResponseEntity<BusinessProcessInstance> continueBusinessProcessInstance(BusinessProcessInstanceInputMessage body) {
        return null;
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteBusinessProcessDefinition(String businessProcessID) {
        return null;
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteBusinessProcessPartnerApplicationPreference(String partnerID) {
        return null;
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteBusinessProcessPartnerPreference(String partnerID) {
        return null;
    }

    @Override
    public ResponseEntity<BusinessProcess> getBusinessProcessDefinition(String businessProcessID) {
        return null;
    }

    @Override
    public ResponseEntity<List<BusinessProcess>> getBusinessProcessDefinitions() {
        return null;
    }

    @Override
    public ResponseEntity<BusinessProcessApplicationConfigurations> getBusinessProcessPartnerApplicationPreference(String partnerID) {
        return null;
    }

    @Override
    public ResponseEntity<BusinessProcessPreferences> getBusinessProcessPartnerPreference(String partnerID) {
        return null;
    }

    @Override
    public ResponseEntity<BusinessProcessDocument> getDocument(String documentID) {
        return null;
    }

    @Override
    public ResponseEntity<List<BusinessProcessDocument>> getDocuments(String partnerID, String typeID) {
        return null;
    }

    @Override
    public ResponseEntity<List<BusinessProcessDocument>> getDocuments(String partnerID, String typeID, String source) {
        return null;
    }

    @Override
    public ResponseEntity<List<BusinessProcessDocument>> getDocuments(String partnerID, String typeID, String status, String source) {
        return null;
    }

    @Override
    public ResponseEntity<BusinessProcessInstance> startBusinessProcessInstance(BusinessProcessInstanceInputMessage body) {
        return null;
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateBusinessProcessDefinition(String businessProcessID, BusinessProcess body) {
        return null;
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateBusinessProcessPartnerApplicationPreference(String partnerID, BusinessProcessApplicationConfigurations body) {
        return null;
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateBusinessProcessPartnerPreference(String partnerID, BusinessProcessPreferences body) {
        return null;
    }
}
