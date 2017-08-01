package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceInputMessageDAO;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.swagger.api.ContinueApi;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class ContinueController implements ContinueApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public ResponseEntity<ProcessInstance> continueProcessInstance(@RequestBody ProcessInstanceInputMessage body) {
        logger.debug(" $$$ Continue Process with ProcessInstanceInputMessage {}", body.toString());
        ProcessInstanceInputMessageDAO processInstanceInputMessageDAO = HibernateSwaggerObjectMapper.createProcessInstanceInputMessage_DAO(body);
        HibernateUtility.getInstance("bp-data-model").persist(processInstanceInputMessageDAO);

        ProcessInstance processInstance = CamundaEngine.continueProcessInstance(body);

        ProcessInstanceDAO processInstanceDAO = HibernateSwaggerObjectMapper.createProcessInstance_DAO(processInstance);
        ProcessInstanceDAO storedInstance = DAOUtility.getProcessIntanceDAOByID(processInstance.getProcessInstanceID());

        processInstanceDAO.setHjid(storedInstance.getHjid());

        HibernateUtility.getInstance("bp-data-model").update(processInstanceDAO);

        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }
}
