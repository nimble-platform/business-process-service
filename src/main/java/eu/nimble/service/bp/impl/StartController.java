package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.bom.BPMessageGenerator;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.billOfMaterial.BillOfMaterial;
import eu.nimble.service.bp.model.billOfMaterial.BillOfMaterialItem;
import eu.nimble.service.bp.model.hyperjaxb.*;
import eu.nimble.service.bp.swagger.model.*;
import eu.nimble.service.bp.util.BusinessProcessEvent;
import eu.nimble.service.bp.util.bp.BusinessProcessUtility;
import eu.nimble.service.bp.util.bp.ClassProcessTypeMap;
import eu.nimble.service.bp.util.camunda.CamundaEngine;
import eu.nimble.service.bp.util.email.IEmailSenderUtil;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.CataloguePersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.util.serialization.MixInIgnoreProperties;
import eu.nimble.service.bp.swagger.api.StartApi;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.swagger.model.ProcessVariables;
import eu.nimble.service.bp.swagger.model.Transaction;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.LoggerUtils;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.validation.IValidationUtil;
import feign.Response;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.util.*;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class StartController implements StartApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private JPARepositoryFactory repoFactory;
    @Autowired
    private IEmailSenderUtil emailSenderUtil;
    @Autowired
    private CollaborationGroupsController collaborationGroupsController;
    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;

    @Override
    @ApiOperation(value = "", notes = "Creates negotiations for the given bill of materials for the given party. If there is a frame contract between parties and useFrameContract parameter is set to true, " +
            "then the service creates an order for the product using the details of frame contract.The person who starts the process is saved as the creator of process. This information is derived " +
            "from the given bearer token.")
    public ResponseEntity<String> createNegotiationsForBOM(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                           @ApiParam(value = "Serialized form of bill of materials which are used to create request for quotations.", required = true) @RequestBody BillOfMaterial billOfMaterial,
                                                           @ApiParam(value = "If this parameter is true and a valid frame contract exists between parties, then an order is started for the product using the details of frame contract") @RequestParam(value = "useFrameContract", defaultValue = "false") Boolean useFrameContract) {
        // set request log of ExecutionContext
        String requestLog = String.format("Creating negotiations for bill of materials , useFrameContract: %s", useFrameContract);
        executionContext.setRequestLog(requestLog);

        logger.info(requestLog);
        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        try {
            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);
            // get buyer party
            PartyType buyerParty = SpringBridge.getInstance().getiIdentityClientTyped().getPartyByPersonID(person.getID()).get(0);
            String buyerPartyId = buyerParty.getPartyIdentification().get(0).getID();
            String buyerFederationId = buyerParty.getFederationInstanceID();

            String hjidOfBaseGroup = null;
            List<FederatedCollaborationGroupMetadataDAO> federatedCollaborationGroupMetadataDAOS = new ArrayList<>();

            // for each product, create a RFQ
            for(BillOfMaterialItem billOfMaterialItem: billOfMaterial.getBillOfMaterialItems()){
                String responderFederationId = CataloguePersistenceUtility.getCatalogueLine(billOfMaterialItem.getCatalogueUuid(),billOfMaterialItem.getlineId()).getGoodsItem().getItem().getManufacturerParty().getFederationInstanceID();

                // create ProcessInstanceInputMessage for line item
                ProcessInstanceInputMessage processInstanceInputMessage = BPMessageGenerator.createBPMessageForBOM(billOfMaterialItem, useFrameContract, buyerParty, person.getID(),buyerFederationId,responderFederationId, bearerToken);

                // start the process and get process instance id since we need this info to find collaboration group of process
                String processInstanceId = startProcessInstance(bearerToken, buyerFederationId,responderFederationId,processInstanceInputMessage, null, null,  null,null).getBody().getProcessInstanceID();

                if (hjidOfBaseGroup == null) {
                    hjidOfBaseGroup = CollaborationGroupDAOUtility.getCollaborationGroupHjidByProcessInstanceIdAndPartyId(processInstanceId, buyerPartyId, buyerFederationId).toString();
                } else {
                    FederatedCollaborationGroupMetadataDAO federatedCollaborationGroupMetadataDAO = new FederatedCollaborationGroupMetadataDAO();
                    federatedCollaborationGroupMetadataDAO.setFederationID(SpringBridge.getInstance().getFederationId());
                    federatedCollaborationGroupMetadataDAO.setID(CollaborationGroupDAOUtility.getCollaborationGroupHjidByProcessInstanceIdAndPartyId(processInstanceId, buyerPartyId, buyerFederationId).toString());
                    federatedCollaborationGroupMetadataDAOS.add(federatedCollaborationGroupMetadataDAO);
                }
            }
            // merge groups to create a project
            if (federatedCollaborationGroupMetadataDAOS.size() > 0) {
                collaborationGroupsController.mergeCollaborationGroups(bearerToken, hjidOfBaseGroup, JsonSerializationUtility.getObjectMapper().writeValueAsString(federatedCollaborationGroupMetadataDAOS));
            }
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CREATE_NEGOTIATIONS_FOR_BOM.toString(),e);
        }

        logger.info("Created negotiations for bill of materials, useFrameContract: {}", useFrameContract);
        return ResponseEntity.ok(null);
    }

    @ApiIgnore
    @Override
    @ApiOperation(value = "", notes = "Starts a business process.", response = ProcessInstance.class, tags = {})
    public ResponseEntity<ProcessInstance> startProcessInstance(
            @ApiParam(value = "The Bearer token provided by the identity service", required = true)
            @RequestHeader(value = "Authorization", required = true) String bearerToken,
            @ApiParam(value = "" ,required=true ) @RequestHeader(value="initiatorFederationId", required=true) String initiatorFederationId,
            @ApiParam(value = "" ,required=true ) @RequestHeader(value="responderFederationId", required=true) String responderFederationId,
            @ApiParam(value = "Serialized form of the ProcessInstanceInputMessage (piim). <br>" +
                    "The piim.processInstanceID would be empty while starting a new business process. <br>" +
                    "piim.variables should contain variables that are passed to the relevant tasks of the process instance execution.<br>" +
                    "<ul><li>piim.variables.processID refers to identifier (i.e. type) of the ProcessInstance that is executed (i.e. Order)</li>" +
                    "<li>piim.variables.initiatorID refers to the company who has initiated this process</li>" +
                    "<li>piim.variables.responderID refers to the company who is supposed to respond the initial message in the process</li>" +
                    "<li>piim.variables.relatedProducts refers to the products related to this specific process instances</li>" +
                    "<li>piim.variables.relatedProductCategories refers to the categories associated to the related products</li>" +
                    "<li>piim.variables.content refers to the serialized content of the document exchanged in this (initiation) step of the business process</li>" +
                    "<li>piim.variables.creatorUserID refers to the person id issuing this continuation response</li></ul>", required = true)
            @RequestBody ProcessInstanceInputMessage body,
            @ApiParam(value = "Identifier of the process instance group owned by the party initiating the process. " +
                    "This is an optional parameter. It would be null for the first business process inside the ProcessInstanceGroup." +
                    "For the subsequent process for this group, it would contain the identifier of the ProcessInstanceGroup " +
                    "(i.e. processInstanceGroup.id)")
            @RequestParam(value = "gid", required = false) String gid,
            @ApiParam(value = "Identifier of the preceding ProcessInstanceGroup. This parameter should be set when a " +
                    "continuation relation between two ProcessInstanceGroups. For example, a transport related ProcessInstanceGroup " +
                    "needs to know the preceding ProcessInstanceGroup in order to find the corresponding Order inside the previous group.")
            @RequestParam(value = "precedingGid", required = false) String precedingGid,
            @ApiParam(value = "Identifier of the preceding Order. This parameter should be set when a " +
                    "continuation relation between two ProcessInstanceGroups. For example, a transport related ProcessInstanceGroup " +
                    "needs to know the preceding order in order to find the corresponding Order inside the previous group.")
            @RequestParam(value = "precedingOrderId", required = false) String precedingOrderId,
            @ApiParam(value = "Identifier of the Collaboration (i.e. collaborationGroup.hjid) which the ProcessInstanceGroup belongs to")
            @RequestParam(value = "collaborationGID", required = false) String collaborationGID) throws NimbleException {
        // set request log of ExecutionContext
        executionContext.setRequestLog(" $$$ Start Process with ProcessInstanceInputMessage");

        logger.debug(" $$$ Start Process with ProcessInstanceInputMessage {}", JsonSerializationUtility.serializeEntitySilentlyWithMixin(body, ProcessVariables.class, MixInIgnoreProperties.class));

        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        // check whether the process is included in the workflow of seller company
        String processId = body.getVariables().getProcessID();
        // get the identifier of party whose workflow will be checked
        String partyId = processId.contentEquals(ClassProcessTypeMap.CAMUNDA_PROCESS_ID_FULFILMENT) ? body.getVariables().getInitiatorID(): body.getVariables().getResponderID();
        PartyType sellerParty;
        try {
            sellerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,partyId);
        } catch (IOException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_PARTY_INFO.toString(),Arrays.asList(partyId),e);
        }

        // check whether the started process is supported by the seller party
        if(sellerParty.getProcessID() != null && sellerParty.getProcessID().size() > 0 && !sellerParty.getProcessID().contains(processId)){
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_NOT_INCLUDED_IN_WORKFLOW.toString(),Arrays.asList(processId,sellerParty.getPartyName().get(0).getName().getValue()));
        }

        // check the entity ids in the passed document
        Transaction.DocumentTypeEnum documentType = BusinessProcessUtility.getInitialDocumentForProcess(processId);
        Object document = DocumentPersistenceUtility.readDocument(DocumentType.valueOf(documentType.toString()), body.getVariables().getContent());

        boolean hjidsExists = resourceValidationUtil.hjidsExit(document);
        if(hjidsExists) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_HJID_FIELDS_FOUND.toString(),Arrays.asList(documentType.toString(), body.getVariables().getContent()));
        }

        // ensure that the the initiator and responder IDs are different
        if(initiatorFederationId.contentEquals(responderFederationId) && body.getVariables().getInitiatorID().equals(body.getVariables().getResponderID())) {
            throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_SAME_PARTIES_TO_START_PROCESS.toString(),Arrays.asList(JsonSerializationUtility.serializeEntitySilentlyWithMixin(body, ProcessInstanceInputMessage.class, MixInIgnoreProperties.class)));
        }

        // get BusinessProcessContext
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);

        // TODO move the business logic below, to a dedicated place
        // we want to perform all database-actions in a single transaction
        GenericJPARepository repo = businessProcessContext.getBpRepository();

        ProcessInstance processInstance = null;
        try {
            ProcessInstanceInputMessageDAO processInstanceInputMessageDAO = HibernateSwaggerObjectMapper.createProcessInstanceInputMessage_DAO(body);
            repo.persistEntity(processInstanceInputMessageDAO);

            processInstance = CamundaEngine.startProcessInstance(businessProcessContext.getId(),body,initiatorFederationId,responderFederationId);

            ProcessInstanceDAO processInstanceDAO = HibernateSwaggerObjectMapper.createProcessInstance_DAO(processInstance);
            processInstanceDAO = repo.updateEntity(processInstanceDAO);

            CollaborationGroupDAO initiatorCollaborationGroupDAO;
            CollaborationGroupDAO responderCollaborationGroupDAO;


            if(precedingGid != null){
                collaborationGID = null;
            }

            // create collaboration group if this is the first process initializing the collaboration group
            if(collaborationGID == null){
                initiatorCollaborationGroupDAO = CollaborationGroupDAOUtility.createCollaborationGroupDAO(repo);
                responderCollaborationGroupDAO = CollaborationGroupDAOUtility.createCollaborationGroupDAO(repo);
            }
            else {
                initiatorCollaborationGroupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(collaborationGID));
                // if there is a process instance group id, then get the process ids included in this group
                // otherwise, get the process ids included in the collaboration group
                List<String> processInstanceIds = new ArrayList<>();
                if(gid != null){
                    ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(gid,repo);
                    processInstanceIds = processInstanceGroupDAO.getProcessInstanceIDs();
                }
                else {
                    processInstanceIds = CollaborationGroupDAOUtility.getProcessInstanceIds(initiatorCollaborationGroupDAO);
                }
                // get responder collaboration group
                responderCollaborationGroupDAO = CollaborationGroupDAOUtility.getCollaborationGroup(body.getVariables().getResponderID(),responderFederationId,processInstanceIds,repo);
                // check whether the responder collaboration group is null or not
                if(responderCollaborationGroupDAO == null){
                    responderCollaborationGroupDAO = CollaborationGroupDAOUtility.createCollaborationGroupDAO(repo);
                }
            }

            // create process instance groups if this is the first process initializing the process group
            if (gid == null) {
                createProcessInstanceGroups(businessProcessContext.getId(),body, processInstance,initiatorCollaborationGroupDAO,responderCollaborationGroupDAO,precedingGid,initiatorFederationId,responderFederationId);
                // the group exists for the initiator but the trading partner is a new one
                // so, a new group should be created for the new party
            } else {
                addNewProcessInstanceToGroup(businessProcessContext.getId(),gid, processInstance.getProcessInstanceID(), body,responderCollaborationGroupDAO,responderFederationId);
            }

            if(precedingOrderId != null){
                String collaborationGroupId = CollaborationGroupDAOUtility.getCollaborationGroupHjidByProcessInstanceIdAndPartyId(repo,processInstance.getProcessInstanceID(),body.getVariables().getInitiatorID(),initiatorFederationId).toString();
                FederatedCollaborationGroupMetadata federatedCollaborationGroupMetadata = new FederatedCollaborationGroupMetadata();
                federatedCollaborationGroupMetadata.setID(collaborationGroupId);
                federatedCollaborationGroupMetadata.setFederationID(responderFederationId);
                try {
                    String requestBody = JsonSerializationUtility.getObjectMapper().writeValueAsString(federatedCollaborationGroupMetadata);
                    // initiator party is in this instance
                    if(initiatorFederationId.contentEquals(SpringBridge.getInstance().getFederationId())){
                        ResponseEntity responseEntity = collaborationGroupsController.addFederatedMetadataToCollaborationGroup(precedingOrderId,requestBody,body.getVariables().getInitiatorID(),initiatorFederationId,bearerToken);
                    }
                    // initiator party is in a different instance
                    else{
                        Response response = SpringBridge.getInstance().getDelegateClient().addFederatedMetadataToCollaborationGroup(bearerToken,initiatorFederationId,precedingOrderId,requestBody,body.getVariables().getInitiatorID(),initiatorFederationId);
                    }
                } catch (Exception e) {
                    throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_MERGE_TRANSPORT_GROUP_TO_ORDER_GROUP.toString(),e);
                }
            }

            businessProcessContext.commitDbUpdates();
            emailSenderUtil.sendActionPendingEmail(bearerToken, executionContext.getOriginalBearerToken(),executionContext.getClientFederationId(),body.getVariables().getContentUUID());
            //mdc logging
            Map<String,String> logParamMap = new HashMap<String, String>();
            ProcessVariablesDAO processVariables = processInstanceInputMessageDAO.getVariables();
            if(processVariables != null) {
                logParamMap.put("bpInitCompanyId", processVariables.getInitiatorID());
                logParamMap.put("bpRespondCompanyId", processVariables.getResponderID());
                logParamMap.put("bpInitUserId", processVariables.getCreatorUserID());
            }
            logParamMap.put("bpId", processInstance.getProcessInstanceID());
            logParamMap.put("bpType", processInstance.getProcessID());
            logParamMap.put("bpStatus", processInstance.getStatus().toString());
            logParamMap.put("activity", BusinessProcessEvent.BUSINESS_PROCESS_START.getActivity());
            LoggerUtils.logWithMDC(logger, logParamMap, LoggerUtils.LogLevel.INFO, "Started a business process instance with id: {}, process type: {}",
                    processInstance.getProcessInstanceID(), processInstance.getProcessID());
        }
        catch (Exception e){
            if(processInstance != null){
                businessProcessContext.setProcessInstanceId(processInstance.getProcessInstanceID());
            }
            businessProcessContext.rollbackDbUpdates();
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_START_PROCESS.toString(),Arrays.asList(body.toString()),e);
        }
        finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }
        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }

    private void createProcessInstanceGroups(String businessContextId, ProcessInstanceInputMessage body, ProcessInstance processInstance, CollaborationGroupDAO initiatorCollaborationGroupDAO, CollaborationGroupDAO responderCollaborationGroupDAO, String precedingGid, String initiatorFederationId, String responderFederationId) {
        GenericJPARepository repo = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).getBpRepository();
        // create group for initiating party
        ProcessInstanceGroupDAO processInstanceGroupDAO1 = ProcessInstanceGroupDAOUtility.createProcessInstanceGroupDAO(
                body.getVariables().getInitiatorID(),
                initiatorFederationId,
                processInstance.getProcessInstanceID(),
                CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getInitiatorRole().toString(),
                body.getVariables().getRelatedProducts(),
                repo);

        // create group for responder party
        ProcessInstanceGroupDAO processInstanceGroupDAO2 = ProcessInstanceGroupDAOUtility.createProcessInstanceGroupDAO(
                body.getVariables().getResponderID(),
                responderFederationId,
                processInstance.getProcessInstanceID(),
                CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(1).getInitiatorRole().toString(),
                body.getVariables().getRelatedProducts(),
                repo);

        // add this group to initiator collaboration group
        initiatorCollaborationGroupDAO.getAssociatedProcessInstanceGroups().add(processInstanceGroupDAO1);
        // update collaboration group
        repo.updateEntity(initiatorCollaborationGroupDAO);

        // add this group to responder collaboration group
        responderCollaborationGroupDAO.getAssociatedProcessInstanceGroups().add(processInstanceGroupDAO2);
        // update collaboration group
        repo.updateEntity(responderCollaborationGroupDAO);
        // when a negotiation is started for a transport service after an order
        if(precedingGid != null){
            FederatedCollaborationGroupMetadataDAO federatedCollaborationGroupMetadataDAO = new FederatedCollaborationGroupMetadataDAO();
            federatedCollaborationGroupMetadataDAO.setFederationID(initiatorFederationId);
            federatedCollaborationGroupMetadataDAO.setID(precedingGid);
            processInstanceGroupDAO1.setPrecedingProcessInstanceGroupMetadata(federatedCollaborationGroupMetadataDAO);
            repo.updateEntity(processInstanceGroupDAO1);
        }
    }

    private void addNewProcessInstanceToGroup(String businessContextId,String sourceGid, String processInstanceId, ProcessInstanceInputMessage body, CollaborationGroupDAO responderCollaborationGroupDAO, String responderFederationId) {
        GenericJPARepository repo = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(businessContextId).getBpRepository();

        ProcessInstanceGroupDAO sourceGroup;
        sourceGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(sourceGid,repo);
        sourceGroup.getProcessInstanceIDs().add(processInstanceId);
        sourceGroup = repo.updateEntity(sourceGroup);

        // add the new process instance to the recipient's group
        // if such a group exists add into it otherwise create a new group
        ProcessInstanceGroupDAO associatedGroup = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(body.getVariables().getResponderID(),responderFederationId, sourceGroup.getProcessInstanceIDs(), repo);
        if (associatedGroup == null) {
            ProcessInstanceGroupDAO targetGroup = ProcessInstanceGroupDAOUtility.createProcessInstanceGroupDAO(
                    body.getVariables().getResponderID(),
                    responderFederationId,
                    processInstanceId,
                    CamundaEngine.getTransactions(body.getVariables().getProcessID()).get(0).getResponderRole().toString(),
                    body.getVariables().getRelatedProducts(),
                    sourceGroup.getDataChannelId(),
                    repo);

            // add new group to responder collaboration group
            responderCollaborationGroupDAO.getAssociatedProcessInstanceGroups().add(targetGroup);
            repo.updateEntity(responderCollaborationGroupDAO);
        } else {
            associatedGroup.getProcessInstanceIDs().add(processInstanceId);
            repo.updateEntity(associatedGroup);
        }
    }
}