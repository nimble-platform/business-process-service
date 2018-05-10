package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceInputMessageDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceStatus;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import eu.nimble.service.bp.impl.util.persistence.ProcessInstanceGroupDAOUtility;
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

        ProcessInstanceDAO storedInstance = DAOUtility.getProcessIntanceDAOByID(processInstance.getProcessInstanceID());
        storedInstance.setStatus(ProcessInstanceStatus.fromValue(processInstance.getStatus().toString()));

        HibernateUtilityRef.getInstance("bp-data-model").update(storedInstance);

        // create process instance groups if this is the first process initializing the process group
        if (gid != null) {
            checkExistingGroup(gid, processInstance.getProcessInstanceID(), body);
        }

        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }

    private void checkExistingGroup(String sourceGid, String processInstanceId, ProcessInstanceInputMessage body) {
        ProcessInstanceGroupDAO existingGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(sourceGid);

        // check whether the group for the trading partner is still there. If not, create a new one
        ProcessInstanceGroupDAO associatedGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(body.getVariables().getInitiatorID(), sourceGid);
        if (associatedGroup == null) {
            associatedGroup = ProcessInstanceGroupDAOUtility.createProcessInstanceGroupDAO(
                    body.getVariables().getInitiatorID(),
                    processInstanceId,
                    CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getInitiatorRole().toString(),
                    body.getVariables().getRelatedProducts().toString(),
                    sourceGid);

            // associate groups
            existingGroup.getAssociatedGroups().add(associatedGroup.getID());
            HibernateUtilityRef.getInstance("bp-data-model").update(existingGroup);
        }
    }
}
