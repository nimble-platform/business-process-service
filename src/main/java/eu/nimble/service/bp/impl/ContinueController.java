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
import eu.nimble.service.bp.swagger.api.ContinueApi;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.swagger.model.Transaction;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class ContinueController implements ContinueApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ResourceValidationUtil resourceValidationUtil;
    @Autowired
    private JPARepositoryFactory repoFactory;

    @Override
    @ApiOperation(value = "",notes = "Send input to a waiting process instance (because of a human task)")
    public ResponseEntity<ProcessInstance> continueProcessInstance(@ApiParam(value = "", required = true) @RequestBody ProcessInstanceInputMessage body,
                                                                   @ApiParam(value = "The id of the process instance group owned by the party continuing the process") @RequestParam(value = "gid", required = false) String gid,
                                                                   @ApiParam(value = "" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                                                   @ApiParam(value = "The id of the collaboration group which the process instance group belongs to") @RequestParam(value = "collaborationGID", required = false) String collaborationGID) {
        logger.debug(" $$$ Continue Process with ProcessInstanceInputMessage {}", body.toString());
        ProcessInstance processInstance = null;
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try {
            // check the entity ids in the passed document
            Transaction.DocumentTypeEnum documentType = BusinessProcessUtility.getResponseDocumentForProcess(body.getVariables().getProcessID());
            Object document = DocumentPersistenceUtility.readDocument(DocumentType.valueOf(documentType.toString()), body.getVariables().getContent());

            boolean hjidsExists = resourceValidationUtil.hjidsExit(document);
            if(hjidsExists) {
                String msg = String.format("Entity IDs (hjid fields) found in the passed document. document type: %s, content: %s", documentType.toString(), body.getVariables().getContent());
                logger.warn(msg);
                throw new RuntimeException(msg);
            }

            ProcessInstanceInputMessageDAO processInstanceInputMessageDAO = HibernateSwaggerObjectMapper.createProcessInstanceInputMessage_DAO(body);
            new JPARepositoryFactory().forBpRepository().persistEntity(processInstanceInputMessageDAO);

            // save ProcessInstanceInputMessageDAO
            businessProcessContext.setMessageDAO(processInstanceInputMessageDAO);

            processInstance = CamundaEngine.continueProcessInstance(businessProcessContext.getId(),body, bearerToken);

            ProcessInstanceDAO storedInstance = ProcessInstanceDAOUtility.getById(processInstance.getProcessInstanceID());

            // save previous status
            businessProcessContext.setPreviousStatus(storedInstance.getStatus());

            storedInstance.setStatus(ProcessInstanceStatus.fromValue(processInstance.getStatus().toString()));

            storedInstance = new JPARepositoryFactory().forBpRepository().updateEntity(storedInstance);

            // save ProcessInstanceDAO
            businessProcessContext.setProcessInstanceDAO(storedInstance);

            // create process instance groups if this is the first process initializing the process group
            if (gid != null) {
                checkExistingGroup(businessProcessContext.getId(),gid, processInstance.getProcessInstanceID(), body,collaborationGID);
            }
        }
        catch (Exception e){
            logger.error(" $$$ Failed to continue process with ProcessInstanceInputMessage {}", body.toString(),e);
            businessProcessContext.handleExceptions();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }

        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }

    private void checkExistingGroup(String businessContextId,String sourceGid, String processInstanceId, ProcessInstanceInputMessage body,String responderCollaborationGID){
        ProcessInstanceGroupDAO existingGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(sourceGid);

        // check whether the group for the trading partner is still there. If not, create a new one
        ProcessInstanceGroupDAO associatedGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(body.getVariables().getInitiatorID(), sourceGid);
        if (associatedGroup == null) {
            associatedGroup = ProcessInstanceGroupDAOUtility.createProcessInstanceGroupDAO(
                    body.getVariables().getInitiatorID(),
                    processInstanceId,
                    CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getInitiatorRole().toString(),
                    body.getVariables().getRelatedProducts(),
                    sourceGid);

            CollaborationGroupDAO initiatorCollaborationGroup = ProcessInstanceGroupDAOUtility.getCollaborationGroupDAO(body.getVariables().getInitiatorID(),Long.parseLong(responderCollaborationGID));
            // create a new initiator collaboration group
            if(initiatorCollaborationGroup == null){
                CollaborationGroupDAO responderCollaborationGroup = repoFactory.forBpRepository().getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(responderCollaborationGID));
                initiatorCollaborationGroup = ProcessInstanceGroupDAOUtility.createCollaborationGroupDAO();

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
