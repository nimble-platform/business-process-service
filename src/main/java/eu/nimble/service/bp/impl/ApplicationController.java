package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessApplicationConfigurationsDAO;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.swagger.api.ApplicationApi;
import eu.nimble.service.bp.swagger.model.ProcessApplicationConfigurations;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
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
    public ResponseEntity<ModelApiResponse> addProcessPartnerApplicationPreference(@RequestBody ProcessApplicationConfigurations body) {
        logger.info(" $$$ Adding ProcessApplicationConfigurations: ");
        logger.debug(" $$$ {}", body.toString());
        ProcessApplicationConfigurationsDAO processApplicationConfigurationsDAO = HibernateSwaggerObjectMapper.createProcessApplicationConfigurations_DAO(body);
        HibernateUtility.getInstance("bp-data-model").persist(processApplicationConfigurationsDAO);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteProcessPartnerApplicationPreference(@PathVariable("partnerID") String partnerID, @PathVariable("processID") String processID) {
        logger.info(" $$$ Deleting ProcessApplicationConfigurations for ... {}", partnerID);
        ProcessApplicationConfigurationsDAO processApplicationConfigurationsDAO = DAOUtility.getProcessApplicationConfigurationsDAOByPartnerID(partnerID, processID);
        HibernateUtility.getInstance("bp-data-model").delete(ProcessApplicationConfigurationsDAO.class, processApplicationConfigurationsDAO.getHjid());
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<List<ProcessApplicationConfigurations>> getProcessPartnerApplicationPreference(@PathVariable("partnerID") String partnerID) {
        logger.info(" $$$ Getting ProcessApplicationConfigurations for ... {}", partnerID);
        List<ProcessApplicationConfigurationsDAO> processApplicationConfigurationsDAO = DAOUtility.getProcessApplicationConfigurationsDAOByPartnerID(partnerID);

        List<ProcessApplicationConfigurations> processApplicationConfigurations = new ArrayList<>();

        for(ProcessApplicationConfigurationsDAO preferenceDAO : processApplicationConfigurationsDAO) {
            ProcessApplicationConfigurations configurations = HibernateSwaggerObjectMapper.createProcessApplicationConfigurations(preferenceDAO);
            processApplicationConfigurations.add(configurations);
        }

        return new ResponseEntity<>(processApplicationConfigurations, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateProcessPartnerApplicationPreference(@RequestBody ProcessApplicationConfigurations body) {
        logger.info(" $$$ Updating ProcessApplicationConfigurations: ");
        logger.debug(" $$$ {}", body.toString());
        ProcessApplicationConfigurationsDAO processApplicationConfigurationsDAO = DAOUtility.getProcessApplicationConfigurationsDAOByPartnerID(body.getPartnerID(), body.getProcessID());
        ProcessApplicationConfigurationsDAO processApplicationConfigurationsDAONew = HibernateSwaggerObjectMapper.createProcessApplicationConfigurations_DAO(body);
        processApplicationConfigurationsDAONew.setHjid(processApplicationConfigurationsDAO.getHjid());
        HibernateUtility.getInstance("bp-data-model").update(processApplicationConfigurationsDAONew);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }
}
