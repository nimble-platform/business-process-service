package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.hyperjaxb.ProcessPreferencesDAO;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessPreferencesDAOUtility;
import eu.nimble.service.bp.swagger.api.PreferenceApi;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessPreferences;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.validation.IValidationUtil;
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
import springfox.documentation.annotations.ApiIgnore;

/**
 * Created by yildiray on 5/25/2017.
 */
@ApiIgnore
public class PreferenceController implements PreferenceApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JPARepositoryFactory repositoryFactory;
    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @Override
    @ApiOperation(value = "",notes = "Add a new partner business process sequence preference")
    public ResponseEntity<ModelApiResponse> addProcessPartnerPreference(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                                                        @RequestBody ProcessPreferences body) {
        // set request log of ExecutionContext
        String requestLog = " $$$ Adding ProcessPreferences: ";
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);
        logger.debug(" $$$ {}", body.toString());
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        ProcessPreferencesDAO processPreferencesDAO = HibernateSwaggerObjectMapper.createProcessPreferences_DAO(body);
        repositoryFactory.forBpRepository().persistEntity(processPreferencesDAO);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Deletes the business process preference of a partner")
    public ResponseEntity<ModelApiResponse> deleteProcessPartnerPreference(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                                                           @PathVariable("partnerID") String partnerID) {
        // set request log of ExecutionContext
        String requestLog = String.format(" $$$ Deleting ProcessPreferences for ... %s", partnerID);
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        ProcessPreferencesDAO processPreferencesDAO = ProcessPreferencesDAOUtility.getProcessPreferences(partnerID,false);
        repositoryFactory.forBpRepository().deleteEntityByHjid(ProcessPreferencesDAO.class, processPreferencesDAO.getHjid());
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Get the business process preference of a partner")
    public ResponseEntity<ProcessPreferences> getProcessPartnerPreference(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                                                          @PathVariable("partnerID") String partnerID) {
        // set request log of ExecutionContext
        String requestLog = String.format(" $$$ Getting ProcessPreferences for ... %s", partnerID);
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
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
        // set request log of ExecutionContext
        String requestLog = " $$$ Updating ProcessPreferences: ";
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);
        logger.debug(" $$$ {}", body.toString());
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        ProcessPreferencesDAO processPreferencesDAO = ProcessPreferencesDAOUtility.getProcessPreferences(body.getPartnerID(),false);
        ProcessPreferencesDAO processPreferencesDAONew = HibernateSwaggerObjectMapper.createProcessPreferences_DAO(body);
        processPreferencesDAONew.setHjid(processPreferencesDAO.getHjid());
        repositoryFactory.forBpRepository().updateEntity(processPreferencesDAONew);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }
}
