package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceInputMessageDAO;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import eu.nimble.service.bp.swagger.api.ContinueApi;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.utility.HibernateUtility;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class ContinueController implements ContinueApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public ResponseEntity<ProcessInstance> continueProcessInstance(@ApiParam(value = "", required = true) @RequestBody ProcessInstanceInputMessage body, @ApiParam(value = "The UUID of the process instance group owned by the party continuing the process") @RequestParam(value = "gid", required = false) String gid) {
        logger.debug(" $$$ Continue Process with ProcessInstanceInputMessage {}", body.toString());
        ProcessInstanceInputMessageDAO processInstanceInputMessageDAO = HibernateSwaggerObjectMapper.createProcessInstanceInputMessage_DAO(body);
        HibernateUtilityRef.getInstance("bp-data-model").persist(processInstanceInputMessageDAO);

        ProcessInstance processInstance = CamundaEngine.continueProcessInstance(body);

        ProcessInstanceDAO processInstanceDAO = HibernateSwaggerObjectMapper.createProcessInstance_DAO(processInstance);
        ProcessInstanceDAO storedInstance = DAOUtility.getProcessIntanceDAOByID(processInstance.getProcessInstanceID());

        processInstanceDAO.setHjid(storedInstance.getHjid());
        processInstanceDAO.setCreationDate(storedInstance.getCreationDate());
        processInstance.setCreationDate(storedInstance.getCreationDate());

        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceDAO);

        // create process instance groups if this is the first process initializing the process group
        if (gid != null) {
            checkExistingGroup(gid, processInstance.getProcessInstanceID(), body);
        }

        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }

    private void checkExistingGroup(String sourceGid, String processInstanceId, ProcessInstanceInputMessage body) {
        ProcessInstanceGroupDAO existingGroup = DAOUtility.getProcessInstanceGroupDAO(sourceGid);

        // check whether the group for the trading partner is still there. If not, create a new one
        ProcessInstanceGroupDAO associatedGroup = DAOUtility.getProcessInstanceGroupDAO(body.getVariables().getInitiatorID(), sourceGid);
        if(associatedGroup == null) {
            associatedGroup = DAOUtility.createProcessInstanceGroupDAO(
                    body.getVariables().getInitiatorID(),
                    processInstanceId,
                    CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getInitiatorRole().toString(),
                    body.getVariables().getRelatedProducts().toString());

            // associate groups
            List<ProcessInstanceGroupDAO> associatedGroups = new ArrayList<>();
            associatedGroups.add(associatedGroup);
            existingGroup.setAssociatedGroups(associatedGroups);
            existingGroup = (ProcessInstanceGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").update(existingGroup);

            associatedGroups = new ArrayList<>();
            associatedGroups.add(existingGroup);
            associatedGroup.setAssociatedGroups(associatedGroups);
            HibernateUtilityRef.getInstance("bp-data-model").update(associatedGroup);
        }
    }
}
