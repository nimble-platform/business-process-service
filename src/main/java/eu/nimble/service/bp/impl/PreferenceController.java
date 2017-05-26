package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessPreferencesDAO;
import eu.nimble.service.bp.impl.util.DAOUtility;
import eu.nimble.service.bp.impl.util.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.swagger.api.PreferenceApi;
import eu.nimble.service.bp.swagger.model.ProcessPreferences;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class PreferenceController implements PreferenceApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public ResponseEntity<ModelApiResponse> addProcessPartnerPreference(@RequestBody ProcessPreferences body) {
        logger.info(" $$$ Adding ProcessPreferences: ");
        logger.debug(" $$$ {}", body.toString());
        ProcessPreferencesDAO processPreferencesDAO = HibernateSwaggerObjectMapper.createProcessPreferences_DAO(body);
        HibernateUtility.getInstance("bp-data-model").persist(processPreferencesDAO);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteProcessPartnerPreference(@PathVariable("partnerID") String partnerID) {
        logger.info(" $$$ Deleting ProcessPreferences for ... {}", partnerID);
        ProcessPreferencesDAO processPreferencesDAO = DAOUtility.getProcessPreferencesDAOByPartnerID(partnerID);
        HibernateUtility.getInstance("bp-data-model").delete(ProcessPreferencesDAO.class, processPreferencesDAO.getHjid());
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<ProcessPreferences> getProcessPartnerPreference(@PathVariable("partnerID") String partnerID) {
        logger.info(" $$$ Getting ProcessPreferences for ... {}", partnerID);
        ProcessPreferencesDAO businessProcessPreferencesDAO = DAOUtility.getProcessPreferencesDAOByPartnerID(partnerID);
        ProcessPreferences businessProcessPreferences = HibernateSwaggerObjectMapper.createProcessPreferences(businessProcessPreferencesDAO);

        return new ResponseEntity<>(businessProcessPreferences, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateProcessPartnerPreference(@RequestBody ProcessPreferences body) {
        logger.info(" $$$ Updating ProcessPreferences: ");
        logger.debug(" $$$ {}", body.toString());
        ProcessPreferencesDAO processPreferencesDAO = DAOUtility.getProcessPreferencesDAOByPartnerID(body.getPartnerID());
        ProcessPreferencesDAO processPreferencesDAONew = HibernateSwaggerObjectMapper.createProcessPreferences_DAO(body);
        processPreferencesDAONew.setHjid(processPreferencesDAO.getHjid());
        HibernateUtility.getInstance("bp-data-model").update(processPreferencesDAONew);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }
}
