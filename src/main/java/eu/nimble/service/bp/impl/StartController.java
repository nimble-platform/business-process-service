package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.util.bp.BusinessProcessUtility;
import eu.nimble.service.bp.impl.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessInstanceDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.api.StartApi;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.swagger.model.Transaction;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.ResourceValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class StartController implements StartApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ResourceValidationUtil resourceValidationUtil;
    @Autowired
    private JPARepositoryFactory repoFactory;

    @Override
    @ApiOperation(value = "", notes = "Start an instance of a business process", response = ProcessInstance.class, tags={  })
    public ResponseEntity<ProcessInstance> startProcessInstance(@ApiParam(value = "", required = true) @RequestBody ProcessInstanceInputMessage body,
                                                                @ApiParam(value = "The id of the process instance group owned by the party initiating the process") @RequestParam(value = "gid", required = false) String gid,
                                                                @ApiParam(value = "The id of the preceding process instance") @RequestParam(value = "precedingPid", required = false) String precedingPid,
                                                                @ApiParam(value = "The UUID of the previous process instance group") @RequestParam(value = "precedingGid", required = false) String precedingGid,
                                                                @ApiParam(value = "The id of the collaboration group which the process instance group belongs to") @RequestParam(value = "collaborationGID", required = false) String collaborationGID) {
        logger.debug(" $$$ Start Process with ProcessInstanceInputMessage {}", body.toString());
        GenericJPARepository repo = repoFactory.forBpRepository();
        ProcessInstance processInstance = null;
        // get BusinessProcessContext
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try {
            // check the entity ids in the passed document
            Transaction.DocumentTypeEnum documentType = BusinessProcessUtility.getInitialDocumentForProcess(body.getVariables().getProcessID());
            Object document = DocumentPersistenceUtility.readDocument(DocumentType.valueOf(documentType.toString()), body.getVariables().getContent());

            boolean hjidsExists = resourceValidationUtil.hjidsExit(document);
            if(hjidsExists) {
                String msg = String.format("Entity IDs (hjid fields) found in the passed document. document type: %s, content: %s", documentType.toString(), body.getVariables().getContent());
                logger.warn(msg);
                throw new RuntimeException(msg);
            }

            ProcessInstanceInputMessageDAO processInstanceInputMessageDAO = HibernateSwaggerObjectMapper.createProcessInstanceInputMessage_DAO(body);
            repo.persistEntity(processInstanceInputMessageDAO);

            // save ProcessInstanceInputMessageDAO
            businessProcessContext.setMessageDAO(processInstanceInputMessageDAO);

            processInstance = CamundaEngine.startProcessInstance(businessProcessContext.getId(),body);

            ProcessInstanceDAO processInstanceDAO = HibernateSwaggerObjectMapper.createProcessInstance_DAO(processInstance);
            processInstanceDAO = repo.updateEntity(processInstanceDAO);

            // save ProcessInstanceDAO
            businessProcessContext.setProcessInstanceDAO(processInstanceDAO);

            // get the process previous process instance if and only if precedingGid is null
            if(precedingPid != null && precedingGid == null) {
                ProcessInstanceDAO precedingInstance = ProcessInstanceDAOUtility.getById(precedingPid);
                if (precedingInstance == null) {
                    String msg = "Invalid preceding process instance ID: %s";
                    logger.warn(String.format(msg, precedingPid));
                    return ResponseEntity.badRequest().body(null);
                }
                processInstanceDAO.setPrecedingProcess(precedingInstance);
                processInstanceDAO = repo.updateEntity(processInstanceDAO);

                // update ProcessInstanceDAO
                businessProcessContext.setProcessInstanceDAO(processInstanceDAO);
            }

            CollaborationGroupDAO initiatorCollaborationGroupDAO;
            CollaborationGroupDAO responderCollaborationGroupDAO;

            // create collaboration group if this is the first process initializing the collaboration group
            if(collaborationGID == null){
                initiatorCollaborationGroupDAO = ProcessInstanceGroupDAOUtility.createCollaborationGroupDAO();
                responderCollaborationGroupDAO = ProcessInstanceGroupDAOUtility.createCollaborationGroupDAO();

                // set association between collaboration groups
                initiatorCollaborationGroupDAO.getAssociatedCollaborationGroups().add(responderCollaborationGroupDAO.getHjid());
                responderCollaborationGroupDAO.getAssociatedCollaborationGroups().add(initiatorCollaborationGroupDAO.getHjid());

                initiatorCollaborationGroupDAO = repo.updateEntity(initiatorCollaborationGroupDAO);
                responderCollaborationGroupDAO = repo.updateEntity(responderCollaborationGroupDAO);
            }
            else {
                initiatorCollaborationGroupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(collaborationGID));
                // get responder collaboration group
                responderCollaborationGroupDAO = ProcessInstanceGroupDAOUtility.getCollaborationGroupDAO(body.getVariables().getResponderID(),initiatorCollaborationGroupDAO.getHjid());
                // check whether the responder collaboration group is null or not
                if(responderCollaborationGroupDAO == null){
                    responderCollaborationGroupDAO = ProcessInstanceGroupDAOUtility.createCollaborationGroupDAO();
                    // set association between collaboration groups
                    initiatorCollaborationGroupDAO.getAssociatedCollaborationGroups().add(responderCollaborationGroupDAO.getHjid());
                    responderCollaborationGroupDAO.getAssociatedCollaborationGroups().add(initiatorCollaborationGroupDAO.getHjid());
                    initiatorCollaborationGroupDAO = repo.updateEntity(initiatorCollaborationGroupDAO);
                    responderCollaborationGroupDAO = repo.updateEntity(responderCollaborationGroupDAO);
                }
            }

            // create process instance groups if this is the first process initializing the process group
            if (gid == null) {
                createProcessInstanceGroups(businessProcessContext.getId(),body, processInstance,initiatorCollaborationGroupDAO,responderCollaborationGroupDAO,precedingGid,precedingPid);
                // the group exists for the initiator but the trading partner is a new one
                // so, a new group should be created for the new party
            } else {
                addNewProcessInstanceToGroup(businessProcessContext.getId(),gid, processInstance.getProcessInstanceID(), body,responderCollaborationGroupDAO);
            }
        }
        catch (Exception e){
            logger.error(" $$$ Failed to start process with ProcessInstanceInputMessage {}", body.toString(),e);
            businessProcessContext.handleExceptions();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }
        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }

    private void createProcessInstanceGroups(String businessContextId,ProcessInstanceInputMessage body, ProcessInstance processInstance,CollaborationGroupDAO initiatorCollaborationGroupDAO,CollaborationGroupDAO responderCollaborationGroupDAO, String precedingGid,String precedingPid) {
        GenericJPARepository repo = repoFactory.forBpRepository();
        // create group for initiating party
        ProcessInstanceGroupDAO processInstanceGroupDAO1 = ProcessInstanceGroupDAOUtility.createProcessInstanceGroupDAO(
                body.getVariables().getInitiatorID(),
                processInstance.getProcessInstanceID(),
                CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getInitiatorRole().toString(),
                body.getVariables().getRelatedProducts());

        // create group for responder party
        ProcessInstanceGroupDAO processInstanceGroupDAO2 = ProcessInstanceGroupDAOUtility.createProcessInstanceGroupDAO(
                body.getVariables().getResponderID(),
                processInstance.getProcessInstanceID(),
                CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(1).getInitiatorRole().toString(),
                body.getVariables().getRelatedProducts());

        // associate groups
        List<String> associatedGroups = new ArrayList<>();
        associatedGroups.add(processInstanceGroupDAO2.getID());
        processInstanceGroupDAO1.setAssociatedGroups(associatedGroups);

        // below assignment fetches the hjids from the
        processInstanceGroupDAO1 = repo.updateEntity(processInstanceGroupDAO1);

        // add this group to initiator collaboration group
        initiatorCollaborationGroupDAO.getAssociatedProcessInstanceGroups().add(processInstanceGroupDAO1);
        // update collaboration group
        repo.updateEntity(initiatorCollaborationGroupDAO);
        associatedGroups = new ArrayList<>();
        associatedGroups.add(processInstanceGroupDAO1.getID());
        processInstanceGroupDAO2.setAssociatedGroups(associatedGroups);
        processInstanceGroupDAO2 = repo.updateEntity(processInstanceGroupDAO2);

        // add this group to responder collaboration group
        responderCollaborationGroupDAO.getAssociatedProcessInstanceGroups().add(processInstanceGroupDAO2);
        // update collaboration group
        repo.updateEntity(responderCollaborationGroupDAO);
        // when a negotiation is started for a transport service after an order
        if(precedingGid != null){
            processInstanceGroupDAO1.setPrecedingProcessInstanceGroup(ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(precedingGid));
            processInstanceGroupDAO1.setPrecedingProcess(ProcessInstanceDAOUtility.getById(precedingPid));
            processInstanceGroupDAO1 = repo.updateEntity(processInstanceGroupDAO1);
        }

        // save ProcessInstanceGroupDAOs
        BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).setProcessInstanceGroupDAO1(processInstanceGroupDAO1);
        BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).setProcessInstanceGroupDAO2(processInstanceGroupDAO2);
    }

    private void addNewProcessInstanceToGroup(String businessContextId,String sourceGid, String processInstanceId, ProcessInstanceInputMessage body, CollaborationGroupDAO responderCollaborationGroupDAO) {
        GenericJPARepository repo = repoFactory.forBpRepository();

        ProcessInstanceGroupDAO sourceGroup;
        sourceGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(sourceGid);
        if(body.getVariables().getProcessID().equals("Fulfilment") && sourceGroup.getPrecedingProcessInstanceGroup() != null){
            sourceGroup = sourceGroup.getPrecedingProcessInstanceGroup();
        }
        sourceGroup.getProcessInstanceIDs().add(processInstanceId);
        sourceGroup = repo.updateEntity(sourceGroup);

        // save sourceGroup
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId);
        businessProcessContext.setSourceGroup(sourceGroup);

        // add the new process instance to the recipient's group
        // if such a group exists add into it otherwise create a new group
        ProcessInstanceGroupDAO associatedGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(body.getVariables().getResponderID(), sourceGroup.getID());
        if (associatedGroup == null) {
            ProcessInstanceGroupDAO targetGroup = ProcessInstanceGroupDAOUtility.createProcessInstanceGroupDAO(
                    body.getVariables().getResponderID(),
                    processInstanceId,
                    CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getResponderRole().toString(),
                    body.getVariables().getRelatedProducts(),
                    sourceGroup.getID());

            sourceGroup.getAssociatedGroups().add(targetGroup.getID());
            sourceGroup = repo.updateEntity(sourceGroup);

            // add new group to responder collaboration group
            responderCollaborationGroupDAO.getAssociatedProcessInstanceGroups().add(targetGroup);
            repo.updateEntity(responderCollaborationGroupDAO);
            // save targetGroup and sourceGroup
            businessProcessContext.setTargetGroup(targetGroup);
            businessProcessContext.setSourceGroup(sourceGroup);
        } else {
            associatedGroup.getProcessInstanceIDs().add(processInstanceId);
            associatedGroup = repo.updateEntity(associatedGroup);

            // save associatedGroup
            businessProcessContext.setAssociatedGroup(associatedGroup);
        }
    }
}
