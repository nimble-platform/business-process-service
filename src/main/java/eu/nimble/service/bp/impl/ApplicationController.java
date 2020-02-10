package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.model.hyperjaxb.ProcessConfigurationDAO;
import eu.nimble.service.bp.util.HttpResponseUtil;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessConfigurationDAOUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.bp.swagger.api.ApplicationApi;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.utility.exception.NimbleException;
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
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yildiray on 5/25/2017.
 */
@ApiIgnore
public class ApplicationController implements ApplicationApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JPARepositoryFactory repositoryFactory;

    @Override
    @ApiOperation(value = "",notes = "Add a new partner business process application preference")
    public ResponseEntity<ModelApiResponse> addProcessConfiguration(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,@RequestBody ProcessConfiguration body

    ) throws NimbleException {
        logger.info(" $$$ Adding ProcessApplicationConfigurations: ");
        logger.debug(" $$$ {}", body.toString());

        HttpResponseUtil.checkToken(bearerToken);

        ProcessConfigurationDAO processConfigurationDAO = HibernateSwaggerObjectMapper.createProcessConfiguration_DAO(body);
        repositoryFactory.forBpRepository().persistEntity(processConfigurationDAO);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Delete the business process application preference of a partner for a process")
    public ResponseEntity<ModelApiResponse> deleteProcessConfiguration(@PathVariable("partnerID") String partnerID, @PathVariable("processID") String processID, @PathVariable("roleType") String roleType,
                                                                       @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) throws NimbleException {
        logger.info(" $$$ Deleting ProcessApplicationConfigurations for ... {}", partnerID);
        HttpResponseUtil.checkToken(bearerToken);

        ProcessConfigurationDAO processConfigurationDAO = ProcessConfigurationDAOUtility.getProcessConfiguration(partnerID, processID, ProcessConfiguration.RoleTypeEnum.valueOf(roleType));
        repositoryFactory.forBpRepository().deleteEntityByHjid(ProcessConfigurationDAO.class, processConfigurationDAO.getHjid());
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Get the business process application preferences of a partner for all processes")
    public ResponseEntity<List<ProcessConfiguration>> getProcessConfiguration(@PathVariable("partnerID") String partnerID,
                                                                              @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) throws NimbleException {
        logger.info(" $$$ Getting ProcessApplicationConfigurations for ... {}", partnerID);
        HttpResponseUtil.checkToken(bearerToken);

        List<ProcessConfigurationDAO> processApplicationConfigurationsDAO = ProcessConfigurationDAOUtility.getProcessConfigurations(partnerID);

        List<ProcessConfiguration> processApplicationConfigurations = new ArrayList<>();

        for(ProcessConfigurationDAO preferenceDAO : processApplicationConfigurationsDAO) {
            ProcessConfiguration configurations = HibernateSwaggerObjectMapper.createProcessConfiguration(preferenceDAO);
            processApplicationConfigurations.add(configurations);
        }

        return new ResponseEntity<>(processApplicationConfigurations, HttpStatus.OK);
    }

    @Override
    @ApiOperation(value = "",notes = "Get the business process application preferences of a partner for a specific process")
    public ResponseEntity<ProcessConfiguration> getProcessConfigurationByProcessID(@PathVariable("partnerID") String partnerID, @PathVariable("processID") String processID, @PathVariable("roleType") String roleType,
                                                                                   @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) throws NimbleException {
        logger.info(" $$$ Deleting ProcessApplicationConfigurations for ... {}", partnerID);
        HttpResponseUtil.checkToken(bearerToken);

        ProcessConfigurationDAO processConfigurationDAO = ProcessConfigurationDAOUtility.getProcessConfiguration(partnerID, processID, ProcessConfiguration.RoleTypeEnum.valueOf(roleType));
        ProcessConfiguration processConfiguration = null;
        if(processConfigurationDAO != null)
            processConfiguration = HibernateSwaggerObjectMapper.createProcessConfiguration(processConfigurationDAO);
        return new ResponseEntity<>(processConfiguration, HttpStatus.OK);
    }

    @Override
    @ApiOperation(value = "",notes = "Update the business process application preference of a partner")
    public ResponseEntity<ModelApiResponse> updateProcessConfiguration(@ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,@RequestBody ProcessConfiguration body

    ) throws NimbleException {
        logger.info(" $$$ Updating ProcessApplicationConfigurations: ");
        logger.debug(" $$$ {}", body.toString());
        HttpResponseUtil.checkToken(bearerToken);

        ProcessConfigurationDAO processApplicationConfigurationsDAO = ProcessConfigurationDAOUtility.getProcessConfiguration(body.getPartnerID(), body.getProcessID(), body.getRoleType());
        ProcessConfigurationDAO processApplicationConfigurationsDAONew = HibernateSwaggerObjectMapper.createProcessConfiguration_DAO(body);

        if(processApplicationConfigurationsDAO != null) {
            processApplicationConfigurationsDAONew.setHjid(processApplicationConfigurationsDAO.getHjid());
            repositoryFactory.forBpRepository().updateEntity(processApplicationConfigurationsDAONew);
        } else {
            repositoryFactory.forBpRepository().persistEntity(processApplicationConfigurationsDAONew);
        }
        return HibernateSwaggerObjectMapper.getApiResponse();
    }
}
