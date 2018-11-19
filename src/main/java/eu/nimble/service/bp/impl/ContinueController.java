package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.persistence.DAOUtility;
import eu.nimble.service.bp.impl.util.persistence.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import eu.nimble.service.bp.impl.util.persistence.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.api.ContinueApi;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            ProcessInstanceInputMessageDAO processInstanceInputMessageDAO = HibernateSwaggerObjectMapper.createProcessInstanceInputMessage_DAO(body);
            HibernateUtilityRef.getInstance("bp-data-model").persist(processInstanceInputMessageDAO);

            // save ProcessInstanceInputMessageDAO
            businessProcessContext.setMessageDAO(processInstanceInputMessageDAO);

            processInstance = CamundaEngine.continueProcessInstance(businessProcessContext.getId(),body, bearerToken);

            ProcessInstanceDAO storedInstance = DAOUtility.getProcessInstanceDAOByID(processInstance.getProcessInstanceID());

            // save previous status
            businessProcessContext.setPreviousStatus(storedInstance.getStatus());

            storedInstance.setStatus(ProcessInstanceStatus.fromValue(processInstance.getStatus().toString()));

            HibernateUtilityRef.getInstance("bp-data-model").update(storedInstance);

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
                    body.getVariables().getRelatedProducts().toString(),
                    sourceGid);

            CollaborationGroupDAO initiatorCollaborationGroup = ProcessInstanceGroupDAOUtility.getCollaborationGroupDAO(body.getVariables().getInitiatorID(),Long.parseLong(responderCollaborationGID));
            // create a new initiator collaboration group
            if(initiatorCollaborationGroup == null){
                CollaborationGroupDAO responderCollaborationGroup = (CollaborationGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").load(CollaborationGroupDAO.class,Long.parseLong(responderCollaborationGID));
                initiatorCollaborationGroup = ProcessInstanceGroupDAOUtility.createCollaborationGroupDAO(body.getVariables().getRelatedProducts());

                // set association between collaboration groups
                initiatorCollaborationGroup.getAssociatedCollaborationGroups().add(responderCollaborationGroup.getHjid());
                responderCollaborationGroup.getAssociatedCollaborationGroups().add(initiatorCollaborationGroup.getHjid());
                HibernateUtilityRef.getInstance("bp-data-model").update(responderCollaborationGroup);
            }

            // add new group to the collaboration group
            initiatorCollaborationGroup.getAssociatedProcessInstanceGroups().add(associatedGroup);
            HibernateUtilityRef.getInstance("bp-data-model").update(initiatorCollaborationGroup);

            BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).setUpdatedAssociatedGroup(associatedGroup);

            // associate groups
            existingGroup.getAssociatedGroups().add(associatedGroup.getID());
            HibernateUtilityRef.getInstance("bp-data-model").update(existingGroup);
        }
    }
}
