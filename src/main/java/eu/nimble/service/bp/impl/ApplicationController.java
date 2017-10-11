package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessConfigurationDAO;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import eu.nimble.service.bp.swagger.api.ApplicationApi;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class ApplicationController implements ApplicationApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ResponseEntity<ModelApiResponse> addProcessConfiguration(@RequestBody ProcessConfiguration body) {
        logger.info(" $$$ Adding ProcessApplicationConfigurations: ");
        logger.debug(" $$$ {}", body.toString());
        ProcessConfigurationDAO processConfigurationDAO = HibernateSwaggerObjectMapper.createProcessConfiguration_DAO(body);
        HibernateUtilityRef.getInstance("bp-data-model").persist(processConfigurationDAO);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteProcessConfiguration(@PathVariable("partnerID") String partnerID, @PathVariable("processID") String processID, @PathVariable("roleType") String roleType) {
        logger.info(" $$$ Deleting ProcessApplicationConfigurations for ... {}", partnerID);
        ProcessConfigurationDAO processConfigurationDAO = DAOUtility.getProcessConfiguration(partnerID, processID, ProcessConfiguration.RoleTypeEnum.valueOf(roleType));
        HibernateUtilityRef.getInstance("bp-data-model").delete(ProcessConfigurationDAO.class, processConfigurationDAO.getHjid());
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<List<ProcessConfiguration>> getProcessConfiguration(@PathVariable("partnerID") String partnerID) {
        logger.info(" $$$ Getting ProcessApplicationConfigurations for ... {}", partnerID);
        List<ProcessConfigurationDAO> processApplicationConfigurationsDAO = DAOUtility.getProcessConfigurationDAOByPartnerID(partnerID);

        List<ProcessConfiguration> processApplicationConfigurations = new ArrayList<>();

        for(ProcessConfigurationDAO preferenceDAO : processApplicationConfigurationsDAO) {
            ProcessConfiguration configurations = HibernateSwaggerObjectMapper.createProcessConfiguration(preferenceDAO);
            processApplicationConfigurations.add(configurations);
        }

        return new ResponseEntity<>(processApplicationConfigurations, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ProcessConfiguration> getProcessConfigurationByProcessID(@PathVariable("partnerID") String partnerID, @PathVariable("processID") String processID, @PathVariable("roleType") String roleType) {
        logger.info(" $$$ Deleting ProcessApplicationConfigurations for ... {}", partnerID);
        ProcessConfigurationDAO processConfigurationDAO = DAOUtility.getProcessConfiguration(partnerID, processID, ProcessConfiguration.RoleTypeEnum.valueOf(roleType));
        ProcessConfiguration processConfiguration = null;
        if(processConfigurationDAO != null)
            processConfiguration = HibernateSwaggerObjectMapper.createProcessConfiguration(processConfigurationDAO);
        return new ResponseEntity<>(processConfiguration, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateProcessConfiguration(@RequestBody ProcessConfiguration body) {
        logger.info(" $$$ Updating ProcessApplicationConfigurations: ");
        logger.debug(" $$$ {}", body.toString());
        ProcessConfigurationDAO processApplicationConfigurationsDAO = DAOUtility.getProcessConfiguration(body.getPartnerID(), body.getProcessID(), body.getRoleType());
        ProcessConfigurationDAO processApplicationConfigurationsDAONew = HibernateSwaggerObjectMapper.createProcessConfiguration_DAO(body);

        if(processApplicationConfigurationsDAO != null) {
            processApplicationConfigurationsDAONew.setHjid(processApplicationConfigurationsDAO.getHjid());
            HibernateUtilityRef.getInstance("bp-data-model").update(processApplicationConfigurationsDAONew);
        } else {
            HibernateUtilityRef.getInstance("bp-data-model").persist(processApplicationConfigurationsDAONew);
        }
        return HibernateSwaggerObjectMapper.getApiResponse();
    }
}
