package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.nimble.common.rest.identity.model.NegotiationSettings;
import eu.nimble.service.bp.bom.BPMessageGenerator;
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
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.serialization.MixInIgnoreProperties;
import eu.nimble.service.bp.swagger.api.StartApi;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.swagger.model.ProcessVariables;
import eu.nimble.service.bp.swagger.model.Transaction;
import eu.nimble.service.model.ubl.commonaggregatecomponents.LineItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.LoggerUtils;
import eu.nimble.utility.persistence.GenericJPARepository;
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
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private EmailSenderUtil emailSenderUtil;
    @Autowired
    private CollaborationGroupsController collaborationGroupsController;

    @Override
    @ApiOperation(value = "", notes = "Creates negotiations for the given line items for the given party. If there is a frame contract between parties and useFrameContract parameter is set to true, " +
            "then the service creates an order for the product using the details of frame contract.The person who starts the process is saved as the creator of process. This information is derived " +
            "from the given bearer token.")
    public ResponseEntity<String> createNegotiationsForLineItems(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                                                 @ApiParam(value = "Serialized form of line items which are used to create request for quotations.An example line items serialization can be found in:" +
                                                                 "https://github.com/nimble-platform/catalog-service/tree/staging/catalogue-service-micro/src/main/resources/example_content/line_items.json", required = true) @RequestBody String lineItemsJson,
                                                                 @ApiParam(value = "Identifier of the party which creates negotiations for Bill of Materials", required = true) @RequestParam(value = "partyId") String partyId,
                                                                 @ApiParam(value = "If this parameter is true and a valid frame contract exists between parties, then an order is started for the product using the details of frame contract") @RequestParam(value = "useFrameContract", defaultValue = "false") Boolean useFrameContract) {
        logger.info("Creating negotiations for line items for party: {}, useFrameContract: {}", partyId, useFrameContract);
        // check token
        ResponseEntity tokenCheck = HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        try {
            // deserialize LineItems
            List<LineItemType> lineItems = JsonSerializationUtility.getObjectMapper().readValue(lineItemsJson, new TypeReference<List<LineItemType>>() {
            });

            // get buyer party and its negotiation settings
            NegotiationSettings negotiationSettings = SpringBridge.getInstance().getiIdentityClientTyped().getNegotiationSettings(partyId);
            // get person using the given bearer token
            PersonType person = SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken);

            String hjidOfBaseGroup = null;
            List<String> hjidOfGroupsToBeMerged = new ArrayList<>();

            // for each line item, create a RFQ
            for (LineItemType lineItem : lineItems) {
                // create ProcessInstanceInputMessage for line item
                ProcessInstanceInputMessage processInstanceInputMessage = BPMessageGenerator.createBPMessageForLineItem(lineItem, useFrameContract, partyId, negotiationSettings, person.getID(), bearerToken);
                // start the process and get process instance id since we need this info to find collaboration group of process
                String processInstanceId = startProcessInstance(bearerToken, processInstanceInputMessage, null, null, null, null).getBody().getProcessInstanceID();

                if (hjidOfBaseGroup == null) {
                    hjidOfBaseGroup = CollaborationGroupDAOUtility.getCollaborationGroupHjidByProcessInstanceIdAndPartyId(processInstanceId, partyId).toString();
                } else {
                    hjidOfGroupsToBeMerged.add(CollaborationGroupDAOUtility.getCollaborationGroupHjidByProcessInstanceIdAndPartyId(processInstanceId, partyId).toString());
                }
            }

            // merge groups to create a project
            if (hjidOfGroupsToBeMerged.size() > 0) {
                collaborationGroupsController.mergeCollaborationGroups(bearerToken, hjidOfBaseGroup, hjidOfGroupsToBeMerged);
            }
        } catch (Exception e) {
            String msg = String.format("Unexpected error while creating negotiations for line items for party : %s", partyId);
            logger.error(msg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }

        logger.info("Created negotiations for line items for party: {}, useFrameContract: {}", partyId, useFrameContract);
        return ResponseEntity.ok(null);
    }

    @Override
    @ApiOperation(value = "", notes = "Starts a business process.", response = ProcessInstance.class, tags = {})
    public ResponseEntity<ProcessInstance> startProcessInstance(
            @ApiParam(value = "The Bearer token provided by the identity service", required = true)
            @RequestHeader(value = "Authorization", required = true) String bearerToken,
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
            @ApiParam(value = "Identifier of the preceding process instance. If we want to start a new process (for example,PPAP) " +
                    "after a completed process (for example, item information request), then this parameter should be the identifier of the " +
                    "completed process instance, (i.e. the item information request process), i.e. processInstance.id. This " +
                    "identifier corresponds to the identifier generated by Camunda for the corresponding process instance.")
            @RequestParam(value = "precedingPid", required = false) String precedingPid,
            @ApiParam(value = "Identifier of the preceding ProcessInstanceGroup. This parameter should be set when a " +
                    "continuation relation between two ProcessInstanceGroups. For example, a transport related ProcessInstanceGroup " +
                    "needs to know the preceding ProcessInstanceGroup in order to find the corresponding Order inside the previous group.")
            @RequestParam(value = "precedingGid", required = false) String precedingGid,
            @ApiParam(value = "Identifier of the Collaboration (i.e. collaborationGroup.hjid) which the ProcessInstanceGroup belongs to")
            @RequestParam(value = "collaborationGID", required = false) String collaborationGID) {

        logger.debug(" $$$ Start Process with ProcessInstanceInputMessage {}", JsonSerializationUtility.serializeEntitySilentlyWithMixin(body, ProcessVariables.class, MixInIgnoreProperties.class));

        // check whether the process is included in the workflow of seller company
        String processId = body.getVariables().getProcessID();
        PartyType sellerParty;
        try {
            sellerParty = SpringBridge.getInstance().getiIdentityClientTyped().getParty(bearerToken,body.getVariables().getResponderID());
        } catch (IOException e) {
            String msg = String.format("Failed to retrieve party information for : %s", body.getVariables().getResponderID());
            logger.warn(msg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        if(sellerParty.getProcessID() != null && sellerParty.getProcessID().size() > 0 && !sellerParty.getProcessID().contains(processId)){
            String msg = String.format("%s is not included in the workflow of company %s", processId,sellerParty.getPartyIdentification().get(0).getID());
            logger.error(msg);
            throw new BadRequestException(msg);
        }

        // check the entity ids in the passed document
        Transaction.DocumentTypeEnum documentType = BusinessProcessUtility.getInitialDocumentForProcess(processId);
        Object document = DocumentPersistenceUtility.readDocument(DocumentType.valueOf(documentType.toString()), body.getVariables().getContent());

        boolean hjidsExists = resourceValidationUtil.hjidsExit(document);
        if(hjidsExists) {
            String msg = String.format("Entity IDs (hjid fields) found in the passed document. document type: %s, content: %s", documentType.toString(), body.getVariables().getContent());
            logger.warn(msg);
            throw new BadRequestException(msg);
        }

        // ensure that the the initiator and responder IDs are different
        if(body.getVariables().getInitiatorID().equals(body.getVariables().getResponderID())) {
            String msg = String.format("Initiator and responder party IDs are the same for the process.", JsonSerializationUtility.serializeEntitySilentlyWithMixin(body, ProcessInstanceInputMessage.class, MixInIgnoreProperties.class));
            logger.info(msg);
            throw new BadRequestException(msg);
        }

        // TODO move the business logic below, to a dedicated place
        GenericJPARepository repo = repoFactory.forBpRepository(true);
        ProcessInstance processInstance = null;
        // get BusinessProcessContext
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try {
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
                initiatorCollaborationGroupDAO = CollaborationGroupDAOUtility.createCollaborationGroupDAO();
                responderCollaborationGroupDAO = CollaborationGroupDAOUtility.createCollaborationGroupDAO();

                // set association between collaboration groups
                initiatorCollaborationGroupDAO.getAssociatedCollaborationGroups().add(responderCollaborationGroupDAO.getHjid());
                responderCollaborationGroupDAO.getAssociatedCollaborationGroups().add(initiatorCollaborationGroupDAO.getHjid());

                initiatorCollaborationGroupDAO = repo.updateEntity(initiatorCollaborationGroupDAO);
                responderCollaborationGroupDAO = repo.updateEntity(responderCollaborationGroupDAO);
            }
            else {
                initiatorCollaborationGroupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(collaborationGID));
                // get responder collaboration group
                responderCollaborationGroupDAO = CollaborationGroupDAOUtility.getCollaborationGroupDAO(body.getVariables().getResponderID(),initiatorCollaborationGroupDAO.getHjid());
                // check whether the responder collaboration group is null or not
                if(responderCollaborationGroupDAO == null){
                    responderCollaborationGroupDAO = CollaborationGroupDAOUtility.createCollaborationGroupDAO();
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
            emailSenderUtil.sendActionPendingEmail(bearerToken, businessProcessContext);
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
            logger.error(" $$$ Failed to start process with ProcessInstanceInputMessage {}", body.toString(),e);
            businessProcessContext.handleExceptions();
            throw e;
        }
        finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }
        return new ResponseEntity<>(processInstance, HttpStatus.OK);
    }

    private void createProcessInstanceGroups(String businessContextId, ProcessInstanceInputMessage body, ProcessInstance processInstance, CollaborationGroupDAO initiatorCollaborationGroupDAO, CollaborationGroupDAO responderCollaborationGroupDAO, String precedingGid, String precedingPid) {
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
