package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceInputMessageDAO;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import eu.nimble.service.bp.swagger.api.StartApi;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
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
public class StartController implements StartApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public ResponseEntity<ProcessInstance> startProcessInstance(@ApiParam(value = "", required = true) @RequestBody ProcessInstanceInputMessage body,
                                                                @ApiParam(value = "The UUID of the process instance group owned by the party initiating the process") @RequestParam(value = "gid", required = false) String gid) {
        logger.debug(" $$$ Start Process with ProcessInstanceInputMessage {}", body.toString());
        ProcessInstanceInputMessageDAO processInstanceInputMessageDAO = HibernateSwaggerObjectMapper.createProcessInstanceInputMessage_DAO(body);
        HibernateUtilityRef.getInstance("bp-data-model").persist(processInstanceInputMessageDAO);

        ProcessInstance processInstance = CamundaEngine.startProcessInstance(body);

        ProcessInstanceDAO processInstanceDAO = HibernateSwaggerObjectMapper.createProcessInstance_DAO(processInstance);
        HibernateUtilityRef.getInstance("bp-data-model").persist(processInstanceDAO);

        // create process instance groups if this is the first process initializing the process group
        if (gid == null) {
            createProcessInstanceGroups(body, processInstance);

            // the group exists for the initiator but the trading partner is a new one
            // so, a new group should be created for the new party
        } else {
            addNewProcessInstanceToGroup(gid, processInstance.getProcessInstanceID(), body);
        }

        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }

    private void createProcessInstanceGroups(ProcessInstanceInputMessage body, ProcessInstance processInstance) {
        // create group for initiating party
        ProcessInstanceGroupDAO processInstanceGroupDAO1 = DAOUtility.createProcessInstanceGroupDAO(
                body.getVariables().getInitiatorID(),
                processInstance.getProcessInstanceID(),
                CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getInitiatorRole().toString(),
                body.getVariables().getRelatedProducts().toString());

        // craete group for responder party
        ProcessInstanceGroupDAO processInstanceGroupDAO2 = DAOUtility.createProcessInstanceGroupDAO(
                body.getVariables().getResponderID(),
                processInstance.getProcessInstanceID(),
                CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(1).getInitiatorRole().toString(),
                body.getVariables().getRelatedProducts().toString());

        // associate groups
        List<ProcessInstanceGroupDAO> associatedGroups = new ArrayList<>();
        associatedGroups.add(processInstanceGroupDAO2);
        processInstanceGroupDAO1.setAssociatedGroups(associatedGroups);
        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO1);

        associatedGroups = new ArrayList<>();
        associatedGroups.add(processInstanceGroupDAO1);
        processInstanceGroupDAO2.setAssociatedGroups(associatedGroups);
        HibernateUtilityRef.getInstance("bp-data-model").update(processInstanceGroupDAO2);
    }

    private void addNewProcessInstanceToGroup(String sourceGid, String processInstanceId, ProcessInstanceInputMessage body) {
        ProcessInstanceGroupDAO sourceGroup = DAOUtility.getProcessInstanceGroupDAO(sourceGid);
        sourceGroup.getProcessInstanceIDs().add(processInstanceId);
        sourceGroup = (ProcessInstanceGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").update(sourceGroup);


        // add the new process instance to the recipient's group
        // if such a group exists add into it otherwise create a new group
        ProcessInstanceGroupDAO associatedGroup = DAOUtility.getProcessInstanceGroupDAO(body.getVariables().getResponderID(), sourceGid);
        if (associatedGroup == null) {
            ProcessInstanceGroupDAO targetGroup = DAOUtility.createProcessInstanceGroupDAO(
                    body.getVariables().getResponderID(),
                    processInstanceId,
                    CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getResponderRole().toString(),
                    body.getVariables().getRelatedProducts().toString());

            // associate groups
            List<ProcessInstanceGroupDAO> associatedGroups = new ArrayList<>();
            associatedGroups.add(sourceGroup);
            targetGroup.setAssociatedGroups(associatedGroups);
            targetGroup = (ProcessInstanceGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").update(targetGroup);

            associatedGroups = new ArrayList<>();
            associatedGroups.add(targetGroup);
            sourceGroup.setAssociatedGroups(associatedGroups);
            HibernateUtilityRef.getInstance("bp-data-model").update(sourceGroup);

        } else {
            associatedGroup.getProcessInstanceIDs().add(processInstanceId);
            HibernateUtilityRef.getInstance("bp-data-model").update(associatedGroup);
        }
    }
}
