package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.hyperjaxb.*;
import eu.nimble.service.bp.util.BusinessProcessEvent;
import eu.nimble.service.bp.util.bp.BusinessProcessUtility;
import eu.nimble.service.bp.util.camunda.CamundaEngine;
import eu.nimble.service.bp.util.email.IEmailSenderUtil;
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
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.LoggerUtils;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.validation.IValidationUtil;
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
import springfox.documentation.annotations.ApiIgnore;

import java.util.Arrays;
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
    private IEmailSenderUtil emailSenderUtil;
    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @ApiIgnore
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
            @RequestHeader(value = "Authorization", required = true) String bearerToken,
            @ApiParam(value = "" ,required=true ) @RequestHeader(value="initiatorFederationId", required=true) String initiatorFederationId,
            @ApiParam(value = "" ,required=true ) @RequestHeader(value="responderFederationId", required=true) String responderFederationId) throws NimbleException {
        // set request log of ExecutionContext
        executionContext.setRequestLog(" $$$ Continue Process with ProcessInstanceInputMessage");
        try {
            logger.debug(" $$$ Continue Process with ProcessInstanceInputMessage {}", JsonSerializationUtility.getObjectMapperWithMixIn(ProcessVariables.class, MixInIgnoreProperties.class).writeValueAsString(body));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize process instance input message: ",e);
        }
        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setProcessID(body.getVariables().getProcessID());
        processInstance.setProcessInstanceID(body.getProcessInstanceID());
        processInstance.setStatus(ProcessInstance.StatusEnum.COMPLETED);

        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_SALES)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            String processId = body.getVariables().getProcessID();
            // check the entity ids in the passed document
            Transaction.DocumentTypeEnum documentType = BusinessProcessUtility.getResponseDocumentForProcess(processId);
            Object document = DocumentPersistenceUtility.readDocument(DocumentType.valueOf(documentType.toString()), body.getVariables().getContent());

            boolean hjidsExists = resourceValidationUtil.hjidsExit(document);
            if (hjidsExists) {
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_HJID_FIELDS_FOUND.toString(),Arrays.asList(documentType.toString(), body.getVariables().getContent()));
            }

            ProcessInstanceInputMessageDAO processInstanceInputMessageDAO = HibernateSwaggerObjectMapper.createProcessInstanceInputMessage_DAO(body);
            businessProcessContext.getBpRepository().persistEntity(processInstanceInputMessageDAO);

            ProcessInstanceDAO storedInstance = ProcessInstanceDAOUtility.getById(processInstance.getProcessInstanceID(),businessProcessContext.getBpRepository());

            storedInstance.setStatus(ProcessInstanceStatus.fromValue(processInstance.getStatus().toString()));

            storedInstance = businessProcessContext.getBpRepository().updateEntity(storedInstance);

            // create process instance groups if this is the first process initializing the process group
            checkExistingGroup(businessProcessContext.getId(), gid,processInstance.getProcessInstanceID(), body, initiatorFederationId);

            CamundaEngine.continueProcessInstance(businessProcessContext.getId(), body,initiatorFederationId, responderFederationId, bearerToken);

            businessProcessContext.commitDbUpdates();
            emailSenderUtil.sendActionPendingEmail(bearerToken, body.getVariables().getContentUUID());

            //mdc logging
            Map<String,String> logParamMap = new HashMap<String, String>();
            logParamMap.put("bpId", storedInstance.getProcessInstanceID());
            logParamMap.put("bpType", storedInstance.getProcessID());
            logParamMap.put("bpStatus", storedInstance.getStatus().toString());
            logParamMap.put("activity", BusinessProcessEvent.BUSINESS_PROCESS_COMPLETE.getActivity());
            LoggerUtils.logWithMDC(logger, logParamMap, LoggerUtils.LogLevel.INFO, "Completed a business process instance with id: {}, process type: {}",
                    storedInstance.getProcessInstanceID(), storedInstance.getProcessID());
        } catch (Exception e) {
            businessProcessContext.rollbackDbUpdates();
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CONTINUE_PROCESS.toString(),Arrays.asList(body.toString()),e);
        } finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }

        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }

    private void checkExistingGroup(String businessContextId, String sourceGid, String processInstanceId, ProcessInstanceInputMessage body, String initiatorFederationId) {
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId);
        ProcessInstanceGroupDAO existingGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(sourceGid,businessProcessContext.getBpRepository());
        // check whether the group for the trading partner is still there. If not, create a new one
        ProcessInstanceGroupDAO associatedGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(body.getVariables().getInitiatorID(), initiatorFederationId,Arrays.asList(processInstanceId),businessProcessContext.getBpRepository());
        if (associatedGroup == null) {
            associatedGroup = ProcessInstanceGroupDAOUtility.createProcessInstanceGroupDAO(
                    body.getVariables().getInitiatorID(),
                    initiatorFederationId,
                    processInstanceId,
                    CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getInitiatorRole().toString(),
                    body.getVariables().getRelatedProducts(),
                    existingGroup.getDataChannelId(),
                    businessProcessContext.getBpRepository());

            CollaborationGroupDAO initiatorCollaborationGroup = CollaborationGroupDAOUtility.getCollaborationGroup(body.getVariables().getInitiatorID(),initiatorFederationId, Arrays.asList(processInstanceId),businessProcessContext.getBpRepository());
            // create a new initiator collaboration group
            if (initiatorCollaborationGroup == null) {
                initiatorCollaborationGroup = CollaborationGroupDAOUtility.createCollaborationGroupDAO(businessProcessContext.getBpRepository());
            }

            // add new group to the collaboration group
            initiatorCollaborationGroup.getAssociatedProcessInstanceGroups().add(associatedGroup);
            businessProcessContext.getBpRepository().updateEntity(initiatorCollaborationGroup);
        }
    }
}

