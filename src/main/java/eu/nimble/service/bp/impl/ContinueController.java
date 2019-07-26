package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.nimble.service.bp.model.hyperjaxb.*;
import eu.nimble.service.bp.util.BusinessProcessEvent;
import eu.nimble.service.bp.util.HttpResponseUtil;
import eu.nimble.service.bp.util.bp.BusinessProcessUtility;
import eu.nimble.service.bp.util.camunda.CamundaEngine;
import eu.nimble.service.bp.util.email.EmailSenderUtil;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.util.serialization.MixInIgnoreProperties;
import eu.nimble.service.bp.swagger.api.ContinueApi;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.swagger.model.ProcessVariables;
import eu.nimble.service.bp.swagger.model.Transaction;
import eu.nimble.service.bp.util.persistence.catalogue.TrustPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.LoggerUtils;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class ContinueController implements ContinueApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private JPARepositoryFactory repoFactory;
    @Autowired
    private EmailSenderUtil emailSenderUtil;
    @Autowired
    private ProcessInstanceGroupController processInstanceGroupController;

    @Override
    @ApiOperation(value = "", notes = "Sends input to a waiting process instance (because of a human task)")
    public ResponseEntity<ProcessInstance> continueProcessInstance(
            @ApiParam(value = "Serialized form of the ProcessInstanceInputMessage (piim). <br>" +
                    "The piim.processInstanceID should be set to the identifier of the process instance to be continued. <br>" +
                    "piim.variables should contain variables that are passed to the relevant tasks of the process instance execution.<br>" +
                    "<ul><li>piim.variables.processID refers to identifier (i.e. type) of the business process that is executed (i.e. Order)</li>" +
                    "<li>piim.variables.initiatorID refers to the company who has initiated this process</li>" +
                    "<li>piim.variables.responderID refers to the company who is supposed to respond the initial message in the process</li>" +
                    "<li>piim.variables.relatedProducts refers to the products related to this specific process instances</li>" +
                    "<li>piim.variables.relatedProductCategories refers to the categories associated to the related products</li>" +
                    "<li>piim.variables.content refers to the serialized content of the document exchanged in this (continuation) step of the business process</li>" +
                    "<li>piim.variables.creatorUserID refers to the person id issuing this continuation response</li></ul>", required = true)
            @RequestBody ProcessInstanceInputMessage body,
            @ApiParam(value = "The id of the ProcessInstanceGroup (processInstanceGroup.id) owned by the party continuing the process.", required = true)
            @RequestParam(value = "gid", required = true) String gid,
            @ApiParam(value = "The id of the CollaborationGroup (collaborationGroup.hjid) which the process instance group belongs to", required = true)
            @RequestParam(value = "collaborationGID", required = true) String collaborationGID,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true)
            @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            logger.debug(" $$$ Continue Process with ProcessInstanceInputMessage {}", JsonSerializationUtility.getObjectMapperWithMixIn(ProcessVariables.class, MixInIgnoreProperties.class).writeValueAsString(body));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize process instance input message: ",e);
        }
        ProcessInstance processInstance = null;
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try {
            // check token
            ResponseEntity tokenCheck = HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            String processId = body.getVariables().getProcessID();
            // check the entity ids in the passed document
            Transaction.DocumentTypeEnum documentType = BusinessProcessUtility.getResponseDocumentForProcess(processId);
            Object document = DocumentPersistenceUtility.readDocument(DocumentType.valueOf(documentType.toString()), body.getVariables().getContent());

            boolean hjidsExists = resourceValidationUtil.hjidsExit(document);
            if (hjidsExists) {
                String msg = String.format("Entity IDs (hjid fields) found in the passed document. document type: %s, content: %s", documentType.toString(), body.getVariables().getContent());
                logger.warn(msg);
                throw new RuntimeException(msg);
            }

            ProcessInstanceInputMessageDAO processInstanceInputMessageDAO = HibernateSwaggerObjectMapper.createProcessInstanceInputMessage_DAO(body);
            new JPARepositoryFactory().forBpRepository().persistEntity(processInstanceInputMessageDAO);

            // save ProcessInstanceInputMessageDAO
            businessProcessContext.setMessageDAO(processInstanceInputMessageDAO);

            processInstance = CamundaEngine.continueProcessInstance(businessProcessContext.getId(), body, bearerToken);

            ProcessInstanceDAO storedInstance = ProcessInstanceDAOUtility.getById(processInstance.getProcessInstanceID());

            // save previous status
            businessProcessContext.setPreviousStatus(storedInstance.getStatus());

            storedInstance.setStatus(ProcessInstanceStatus.fromValue(processInstance.getStatus().toString()));

            storedInstance = new JPARepositoryFactory().forBpRepository().updateEntity(storedInstance);

            // save ProcessInstanceDAO
            businessProcessContext.setProcessInstanceDAO(storedInstance);

            // create process instance groups if this is the first process initializing the process group
            checkExistingGroup(businessProcessContext.getId(), gid, processInstance.getProcessInstanceID(), body, collaborationGID);

            // get the identifier of party whose workflow will be checked
            String partyId = processId.contentEquals("Fulfilment") ? body.getVariables().getInitiatorID(): body.getVariables().getResponderID();

            // get the seller party to check its workflow
            PartyType sellerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,partyId);

            // check whether the process is the last step in seller's workflow
            boolean isLastProcessInWorkflow;

            // no workflow for the seller company, use the default workflow
            if(sellerParty.getProcessID() == null || sellerParty.getProcessID().size() == 0){
                isLastProcessInWorkflow = processId.contentEquals("Fulfilment") || processId.contentEquals("Transport_Execution_Plan");
            }
            else{
                isLastProcessInWorkflow = sellerParty.getProcessID().get(sellerParty.getProcessID().size()-1).contentEquals(processId);
            }

            // if it's the last process in the seller's workflow and there is no CompletedTask for this collaboration, create completed tasks for both parties
            if(isLastProcessInWorkflow && processInstanceGroupController.checkCollaborationFinished(gid,bearerToken).getBody().contentEquals("false")){
                TrustPersistenceUtility.createCompletedTasksForBothParties(processInstance.getProcessInstanceID(),bearerToken,"Completed");
            }

            emailSenderUtil.sendActionPendingEmail(bearerToken, businessProcessContext);

            //mdc logging
            Map<String,String> logParamMap = new HashMap<String, String>();
            logParamMap.put("bpId", storedInstance.getProcessInstanceID());
            logParamMap.put("bpType", storedInstance.getProcessID());
            logParamMap.put("bpStatus", storedInstance.getStatus().toString());
            logParamMap.put("activity", BusinessProcessEvent.BUSINESS_PROCESS_COMPLETE.getActivity());
            LoggerUtils.logWithMDC(logger, logParamMap, LoggerUtils.LogLevel.INFO, "Completed a business process instance with id: {}, process type: {}",
                    storedInstance.getProcessInstanceID(), storedInstance.getProcessID());
        } catch (Exception e) {
            logger.error(" $$$ Failed to continue process with ProcessInstanceInputMessage {}", body.toString(), e);
            businessProcessContext.handleExceptions();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }

        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }

    private void checkExistingGroup(String businessContextId, String sourceGid, String processInstanceId, ProcessInstanceInputMessage body, String responderCollaborationGID) {
        ProcessInstanceGroupDAO existingGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(sourceGid);

        // check whether the group for the trading partner is still there. If not, create a new one
        ProcessInstanceGroupDAO associatedGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(body.getVariables().getInitiatorID(), sourceGid,false);
        if (associatedGroup == null) {
            associatedGroup = ProcessInstanceGroupDAOUtility.createProcessInstanceGroupDAO(
                    body.getVariables().getInitiatorID(),
                    processInstanceId,
                    CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getInitiatorRole().toString(),
                    body.getVariables().getRelatedProducts(),
                    sourceGid);

            CollaborationGroupDAO initiatorCollaborationGroup = CollaborationGroupDAOUtility.getCollaborationGroupDAO(body.getVariables().getInitiatorID(), Long.parseLong(responderCollaborationGID),responderCollaborationGID);
            // create a new initiator collaboration group
            if (initiatorCollaborationGroup == null) {
                CollaborationGroupDAO responderCollaborationGroup = repoFactory.forBpRepository(true).getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(responderCollaborationGID));
                initiatorCollaborationGroup = CollaborationGroupDAOUtility.createCollaborationGroupDAO();

                // set association between collaboration groups
                initiatorCollaborationGroup.getAssociatedCollaborationGroups().add(responderCollaborationGroup.getHjid());
                responderCollaborationGroup.getAssociatedCollaborationGroups().add(initiatorCollaborationGroup.getHjid());
                repoFactory.forBpRepository().updateEntity(responderCollaborationGroup);
            }

            // add new group to the collaboration group
            initiatorCollaborationGroup.getAssociatedProcessInstanceGroups().add(associatedGroup);
            repoFactory.forBpRepository().updateEntity(initiatorCollaborationGroup);
            BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).setUpdatedAssociatedGroup(associatedGroup);

            // associate groups
            existingGroup.getAssociatedGroups().add(associatedGroup.getID());
            repoFactory.forBpRepository().updateEntity(existingGroup);
        }
    }
}

