package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.model.hyperjaxb.ProcessPreferencesDAO;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessPreferencesDAOUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.bp.swagger.api.PreferenceApi;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessPreferences;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.IOException;

/**
 * Created by yildiray on 5/25/2017.
 */
public class PreferenceController implements PreferenceApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JPARepositoryFactory repositoryFactory;

    @Override
    @ApiOperation(value = "",notes = "Add a new partner business process sequence preference")
    public ResponseEntity<ModelApiResponse> addProcessPartnerPreference(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                                                        @RequestBody ProcessPreferences body) {
        logger.info(" $$$ Adding ProcessPreferences: ");
        logger.debug(" $$$ {}", body.toString());
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (IOException e){
            String msg = String.format("Failed to add process partner preference: %s",body.toString());
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        ProcessPreferencesDAO processPreferencesDAO = HibernateSwaggerObjectMapper.createProcessPreferences_DAO(body);
        repositoryFactory.forBpRepository().persistEntity(processPreferencesDAO);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Deletes the business process preference of a partner")
    public ResponseEntity<ModelApiResponse> deleteProcessPartnerPreference(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                                                           @PathVariable("partnerID") String partnerID) {
        logger.info(" $$$ Deleting ProcessPreferences for ... {}", partnerID);
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (IOException e){
            String msg = String.format("Failed to delete process partner preference for partner: %s",partnerID);
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        ProcessPreferencesDAO processPreferencesDAO = ProcessPreferencesDAOUtility.getProcessPreferences(partnerID,false);
        repositoryFactory.forBpRepository().deleteEntityByHjid(ProcessPreferencesDAO.class, processPreferencesDAO.getHjid());
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Get the business process preference of a partner")
    public ResponseEntity<ProcessPreferences> getProcessPartnerPreference(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                                                          @PathVariable("partnerID") String partnerID) {
        logger.info(" $$$ Getting ProcessPreferences for ... {}", partnerID);
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (IOException e){
            String msg = String.format("Failed to retrieve process partner preference for partner: %s",partnerID);
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        ProcessPreferencesDAO businessProcessPreferencesDAO = ProcessPreferencesDAOUtility.getProcessPreferences(partnerID);
        ProcessPreferences businessProcessPreferences = null;
        if(businessProcessPreferencesDAO == null) {
            businessProcessPreferences = HibernateSwaggerObjectMapper.createDefaultProcessPreferences();
        } else {
            businessProcessPreferences = HibernateSwaggerObjectMapper.createProcessPreferences(businessProcessPreferencesDAO);
        }

        return new ResponseEntity<>(businessProcessPreferences, HttpStatus.OK);
    }

    @Override
    @ApiOperation(value = "",notes = "Update the business process preference of a partner")
    public ResponseEntity<ModelApiResponse> updateProcessPartnerPreference(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                                                           @RequestBody ProcessPreferences body) {
        logger.info(" $$$ Updating ProcessPreferences: ");
        logger.debug(" $$$ {}", body.toString());
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (IOException e){
            String msg = String.format("Failed to update partner preference: %s",body.toString());
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        ProcessPreferencesDAO processPreferencesDAO = ProcessPreferencesDAOUtility.getProcessPreferences(body.getPartnerID(),false);
        ProcessPreferencesDAO processPreferencesDAONew = HibernateSwaggerObjectMapper.createProcessPreferences_DAO(body);
        processPreferencesDAONew.setHjid(processPreferencesDAO.getHjid());
        repositoryFactory.forBpRepository().updateEntity(processPreferencesDAONew);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }
}
