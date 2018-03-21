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
        if(gid == null) {
            createProcessInstanceGroups(body, processInstance);
        } else {
            addNewProcessInstanceToGroup(gid, processInstance.getProcessInstanceID(), body);
        }

        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }

    private void createProcessInstanceGroups(ProcessInstanceInputMessage body, ProcessInstance processInstance) {
        // create group for initiating party
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();

        ProcessInstanceGroupDAO processInstanceGroupDAO1 = new ProcessInstanceGroupDAO();
        processInstanceGroupDAO1.setArchived(false);
        processInstanceGroupDAO1.setID(uuid1);
        processInstanceGroupDAO1.setName(body.getVariables().getRelatedProducts().toString());
        processInstanceGroupDAO1.setPartyID(body.getVariables().getInitiatorID());
        processInstanceGroupDAO1.setCollaborationRole(CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getInitiatorRole().toString());
        List<String> associatedGroups = new ArrayList<>();
        associatedGroups.add(uuid2);
        processInstanceGroupDAO1.setAssociatedGroups(associatedGroups);
        List<String> processInstanceIds = new ArrayList<>();
        processInstanceIds.add(processInstance.getProcessInstanceID());
        processInstanceGroupDAO1.setProcessInstanceIDs(processInstanceIds);

        // craete group for responder party
        ProcessInstanceGroupDAO processInstanceGroupDAO2 = new ProcessInstanceGroupDAO();
        processInstanceGroupDAO2.setArchived(false);
        processInstanceGroupDAO2.setID(uuid2);
        processInstanceGroupDAO2.setName(body.getVariables().getRelatedProducts().toString());
        processInstanceGroupDAO2.setPartyID(body.getVariables().getResponderID());
        processInstanceGroupDAO2.setCollaborationRole(CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(1).getInitiatorRole().toString());
        associatedGroups = new ArrayList<>();
        associatedGroups.add(uuid1);
        processInstanceGroupDAO2.setAssociatedGroups(associatedGroups);
        processInstanceIds = new ArrayList<>();
        processInstanceIds.add(processInstance.getProcessInstanceID());
        processInstanceGroupDAO2.setProcessInstanceIDs(processInstanceIds);

        HibernateUtilityRef.getInstance("bp-data-model").persist(processInstanceGroupDAO1);
        HibernateUtilityRef.getInstance("bp-data-model").persist(processInstanceGroupDAO2);
    }

    private void addNewProcessInstanceToGroup(String gid, String processInstanceId, ProcessInstanceInputMessage body) {
        ProcessInstanceGroupDAO group = DAOUtility.getProcessInstanceGroupDAO(gid);
        group.getProcessInstanceIDs().add(processInstanceId);
        HibernateUtilityRef.getInstance("bp-data-model").update(group);

        // add the new process instance to the recipient's group
        // if such a group exists add into it otherwise create a new group
        ProcessInstanceGroupDAO associatedGroup = DAOUtility.getProcessInstanceGroupDAO(body.getVariables().getResponderID(), gid);
        if(associatedGroup == null) {
            String uuid = UUID.randomUUID().toString();
            ProcessInstanceGroupDAO processInstanceGroupDAO = new ProcessInstanceGroupDAO();
            processInstanceGroupDAO.setArchived(false);
            processInstanceGroupDAO.setID(uuid);
            processInstanceGroupDAO.setName(body.getVariables().getRelatedProducts().toString());
            processInstanceGroupDAO.setPartyID(body.getVariables().getInitiatorID());
            processInstanceGroupDAO.setCollaborationRole(CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getInitiatorRole().toString());
            List<String> associatedGroups = new ArrayList<>();
            associatedGroups.add(gid);
            processInstanceGroupDAO.setAssociatedGroups(associatedGroups);
            List<String> processInstanceIds = new ArrayList<>();
            processInstanceIds.add(processInstanceId);
            processInstanceGroupDAO.setProcessInstanceIDs(processInstanceIds);
            HibernateUtilityRef.getInstance("bp-data-model").persist(processInstanceGroupDAO);

        } else {
            associatedGroup.getProcessInstanceIDs().add(processInstanceId);
            HibernateUtilityRef.getInstance("bp-data-model").update(associatedGroup);
        }
    }
}
