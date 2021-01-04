package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.exception.NimbleExceptionMessageCode;
import eu.nimble.service.bp.model.export.TransactionSummary;
import eu.nimble.service.bp.model.hyperjaxb.*;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.bp.util.BusinessProcessEvent;
import eu.nimble.service.bp.util.camunda.CamundaEngine;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.TrustPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.commonbasiccomponents.TextType;
import eu.nimble.service.model.ubl.document.IDocument;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.LoggerUtils;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.serialization.JsonSerializer;
import eu.nimble.utility.serialization.ubl.IgnoreMixin;
import eu.nimble.utility.validation.IValidationUtil;
import feign.Response;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by dogukan on 09.08.2018.
 */

@Controller
public class ProcessInstanceController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ResourceValidationUtility resourceValidationUtil;
    @Autowired
    private IIdentityClientTyped iIdentityClientTyped;
    @Autowired
    private IValidationUtil validationUtil;
    @Autowired
    private ExecutionContext executionContext;


    @ApiOperation(value = "",notes = "Cancels the process instance with the given id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Cancelled the process instance successfully"),
            @ApiResponse(code = 400, message = "The process instance with the given id is already cancelled."),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist a process instance with the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while cancelling the process instance with the given id")
    })
    @RequestMapping(value = "/processInstance/{processInstanceId}/cancel",
            method = RequestMethod.POST)
    public ResponseEntity cancelProcessInstance(@ApiParam(value = "The identifier of the process instance to be cancelled", required = true) @PathVariable(value = "processInstanceId", required = true) String processInstanceId,
                                                @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) throws NimbleException {
        // set request log of ExecutionContext
        String requestLog = String.format("Cancelling process instance with id: %s",processInstanceId);
        executionContext.setRequestLog(requestLog);

        logger.debug(requestLog);

        // validate role
        if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
        }

        try {
            ProcessInstanceDAO instanceDAO = ProcessInstanceDAOUtility.getById(processInstanceId);
            // check whether the process instance with the given id exists or not
            if(instanceDAO == null){
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_INSTANCE.toString(),Arrays.asList(processInstanceId));
            }
            // check the status of process instance
            if(instanceDAO.getStatus().equals(ProcessInstanceStatus.CANCELLED)){
                throw new NimbleException(NimbleExceptionMessageCode.BAD_REQUEST_ALREADY_CANCELLED.toString(),Arrays.asList(processInstanceId));
            }

            // if the process is completed or cancelled, we should not call CamundaEngine.cancelProcessInstance method
            // since there will be no such process instance
            if(instanceDAO.getStatus().equals(ProcessInstanceStatus.STARTED)){
                // cancel the process
                CamundaEngine.cancelProcessInstance(processInstanceId);
            }

            // change status of the process
            instanceDAO.setStatus(ProcessInstanceStatus.CANCELLED);
            new JPARepositoryFactory().forBpRepository().updateEntity(instanceDAO);

            //mdc logging
            Map<String,String> logParamMap = new HashMap<String, String>();
            logParamMap.put("bpId", instanceDAO.getProcessInstanceID());
            logParamMap.put("bpType", instanceDAO.getProcessID());
            logParamMap.put("bpStatus", instanceDAO.getStatus().toString());
            logParamMap.put("activity", BusinessProcessEvent.BUSINESS_PROCESS_CANCEL.getActivity());
            LoggerUtils.logWithMDC(logger, logParamMap, LoggerUtils.LogLevel.INFO, "Cancelled a business process instance with id: {}, process type: {}",
                    instanceDAO.getProcessInstanceID(), instanceDAO.getProcessID());
        }
        catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_CANCEL_PROCESS.toString(),Arrays.asList(processInstanceId),e);
        }

        logger.debug("Cancelled process instance with id: {}",processInstanceId);
        return ResponseEntity.ok(null);
    }

    @ApiOperation(value = "",notes = "Updates the process instance with the given id by replacing the exchanged document with" +
            " given document")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Updated the process instance successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist a document metadata for the process instance with the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while updating the process instance with the given id")
    })
    @RequestMapping(value = "/processInstance",
            method = RequestMethod.PATCH)
    public ResponseEntity updateProcessInstance(@ApiParam(value = "Serialized form of the document exchanged in the updated step of the business process", required = true) @RequestBody String content,
                                                @ApiParam(value = "Type of the process instance document to be updated", required = true) @RequestParam(value = "processID") DocumentType documentType,
                                                @ApiParam(value = "Identifier of the process instance to be updated", required = true) @RequestParam(value = "processInstanceID") String processInstanceID,
                                                @ApiParam(value = "Identifier of the user who updated the process instance", required = true) @RequestParam(value = "creatorUserID") String creatorUserID,
                                                @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) throws NimbleException {
        // set request log of ExecutionContext
        String requestLog = String.format("Updating process instance with id: %s",processInstanceID);
        executionContext.setRequestLog(requestLog);

        logger.debug(requestLog);

        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);

        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            ProcessDocumentMetadata processDocumentMetadata = ProcessDocumentMetadataDAOUtility.getRequestMetadata(processInstanceID,businessProcessContext.getBpRepository());
            if(processDocumentMetadata == null){
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_DOCUMENT_METADATA.toString(),Arrays.asList(processInstanceID));
            }
            Object document = DocumentPersistenceUtility.readDocument(documentType, content);
            ProcessInstanceDAO instanceDAO = ProcessInstanceDAOUtility.getById(processInstanceID,businessProcessContext.getBpRepository());
            // update creator user id of metadata
            processDocumentMetadata.setCreatorUserID(creatorUserID);
            ProcessDocumentMetadataDAOUtility.updateDocumentMetadata(businessProcessContext.getId(),processDocumentMetadata);
            // update the corresponding document
            DocumentPersistenceUtility.updateDocument(businessProcessContext.getId(), document);

            businessProcessContext.commitDbUpdates();
            //mdc logging
            Map<String,String> logParamMap = new HashMap<String, String>();
            logParamMap.put("bpInitUserId", processDocumentMetadata.getCreatorUserID());
            logParamMap.put("bpInitCompanyId", processDocumentMetadata.getInitiatorID());
            logParamMap.put("bpRespondCompanyId", processDocumentMetadata.getResponderID());

            logParamMap.put("bpId", instanceDAO.getProcessInstanceID());
            logParamMap.put("bpType", instanceDAO.getProcessID());
            logParamMap.put("bpStatus", instanceDAO.getStatus().toString());
            logParamMap.put("activity", BusinessProcessEvent.BUSINESS_PROCESS_UPDATE.getActivity());
            LoggerUtils.logWithMDC(logger, logParamMap, LoggerUtils.LogLevel.INFO, "Updated a business process instance with id: {}, process type: {}",
                    instanceDAO.getProcessInstanceID(), instanceDAO.getProcessID());
        }
        catch (Exception e) {
            businessProcessContext.rollbackDbUpdates();
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_UPDATE_INSTANCE.toString(),Arrays.asList(processInstanceID),e);
        }
        finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }

        logger.debug("Updated process instance with id: {}",processInstanceID);
        return ResponseEntity.ok(null);
    }

    @ApiOperation(value = "",notes = "Gets rating status for the specified process instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved rating status successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while getting the rating status")
    })
    @RequestMapping(value = "/processInstance/{processInstanceId}/isRated",
            produces = {MediaType.TEXT_PLAIN_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity isRated(@ApiParam(value = "Identifier of the process instance", required = true) @PathVariable(value = "processInstanceId", required = true) String processInstanceId,
                                  @ApiParam(value = "Identifier of the party (the rated) for which the existence of a rating to be checked", required = true) @RequestParam(value = "partyId", required = true) String partyId,
                                  @ApiParam(value = "", required = true) @RequestHeader(value = "federationId", required = true) String federationId,
                                  @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws NimbleException {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Getting rating status for process instance: %s, party: %s", processInstanceId, partyId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            Boolean rated = TrustPersistenceUtility.processInstanceIsRated(partyId,federationId,processInstanceId);

            logger.info("Retrieved rating status for process instance: {}, party: {}", processInstanceId, partyId);
            return ResponseEntity.ok(rated.toString());

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_IS_RATED.toString(),Arrays.asList(processInstanceId, partyId),e);
        }
    }

    @ApiOperation(value = "",notes = "Gets process instance id for the given document")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved process instance id successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist a document with the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while getting the process instance id")
    })
    @RequestMapping(value = "/processInstance/document/{documentId}",
            produces = {MediaType.TEXT_PLAIN_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity getProcessInstanceIdForDocument(@ApiParam(value = "Identifier of the document", required = true) @PathVariable(value = "documentId", required = true) String documentId,
                                  @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws NimbleException {
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Getting process instance id for document: %s", documentId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            ProcessDocumentMetadataDAO processDocumentMetadataDAO = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
            if(processDocumentMetadataDAO == null){
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_DOCUMENT_METADATA.toString(),Arrays.asList(documentId));
            }

            logger.info("Retrieved process instance id for document: {}", documentId);
            return ResponseEntity.ok(processDocumentMetadataDAO.getProcessInstanceID());

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_PROCESS_INSTANCE_ID_FOR_DOCUMENT.toString(),Arrays.asList(documentId),e);
        }
    }

    @ApiOperation(value = "",notes = "Gets details of the specified process instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved process instance details successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist a process instance with the given id"),
            @ApiResponse(code = 500, message = "Unexpected error while getting the details of process instance")
    })
    @RequestMapping(value = "/processInstance/{processInstanceId}/details",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity getDashboardProcessInstanceDetails(@ApiParam(value = "Identifier of the process instance", required = true) @PathVariable(value = "processInstanceId", required = true) String processInstanceId,
                                                             @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws NimbleException {
        ExecutorService executorService = null;
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Getting the details for process instance: %s", processInstanceId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            executorService = Executors.newCachedThreadPool();

            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // check the existence of process instance
            ProcessInstanceDAO instanceDAO = ProcessInstanceDAOUtility.getById(processInstanceId);
            if(instanceDAO == null){
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_PROCESS_INSTANCE.toString(),Arrays.asList(processInstanceId));
            }

            List<HistoricVariableInstance> variableInstanceList = CamundaEngine.getVariableInstances(processInstanceId);

            Future<String> variableInstances = serializeObject(variableInstanceList, executorService);
            Future<String> processInstanceState = serializeObject(CamundaEngine.getProcessInstance(processInstanceId).getState(), executorService);
            Future<String> lastActivityInstanceStartTime = serializeObject(CamundaEngine.getLastActivityInstanceStartTime(processInstanceId), executorService);


            // get request and response document
            // get request and response metadata as well
            Future<String> requestDocument = null;
            Future<String> responseDocumentStatus = null;
            ProcessDocumentMetadata requestMetadata = null;
            ProcessDocumentMetadata responseMetadata = null;
            for(HistoricVariableInstance variableInstance:variableInstanceList){
                // request document
                if(variableInstance.getName().contentEquals("initialDocumentID")){
                    String documentId =  variableInstance.getValue().toString();
                    // request document
                    requestDocument = getRequestDocument(documentId,bearerToken, executorService);
                    // request metadata
                    requestMetadata = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId));
                }
                // response document
                else if(variableInstance.getName().contentEquals("responseDocumentID")){
                    String documentId =  variableInstance.getValue().toString();
                    // response document
                    responseDocumentStatus = getResponseDocumentStatus(documentId, executorService);
                    // response metadata
                    responseMetadata = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId));
                }
            }

            // get the date of request and response
            Future<String> requestDateFuture = getRequestDate(processInstanceId,executorService);
            Future<String> responseDateFuture = getResponseDate(processInstanceId,executorService);;
            // get cancellation reason for the collaboration
            Future<String> cancellationReasonFuture = getCancellationReason(processInstanceId,executorService);
            // get completion date for the collaboration
            Future<String> completionDateFuture = getCompletionDate(processInstanceId,executorService);

            String cancellationReason = cancellationReasonFuture.get();
            String completionDate = completionDateFuture.get();
            String requestDate = requestDateFuture.get();
            String responseDate = responseDateFuture.get();

            JsonSerializer jsonSerializer = new JsonSerializer();
            jsonSerializer.put("requestDocument",requestDocument == null ? null: requestDocument.get());
            jsonSerializer.put("responseDocumentStatus",responseDocumentStatus == null ? null : responseDocumentStatus.get());
            jsonSerializer.put("variableInstance",variableInstances.get());
            jsonSerializer.put("lastActivityInstanceStartTime",lastActivityInstanceStartTime.get());
            jsonSerializer.put("processInstanceState",processInstanceState.get());
            jsonSerializer.put("requestCreatorUserId",requestMetadata == null ? null : "\""+requestMetadata.getCreatorUserID()+"\"");
            jsonSerializer.put("responseCreatorUserId",responseMetadata == null ? null : "\""+ responseMetadata.getCreatorUserID()+"\"");
            jsonSerializer.put("cancellationReason",cancellationReason == null ? null : "\""+cancellationReason+"\"");
            jsonSerializer.put("completionDate",completionDate == null ? null : "\""+completionDate+"\"");
            jsonSerializer.put("requestDate",requestDate == null ? null : "\""+requestDate+"\"");
            jsonSerializer.put("responseDate",responseDate == null ? null : "\""+responseDate+"\"");
            logger.info("Retrieved the details for process instance: {}", processInstanceId);
            return ResponseEntity.ok(jsonSerializer.toString());
        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_DASHBOARD_PROCESS_INSTANCE_DETAILS.toString(),Arrays.asList(processInstanceId),e);
        } finally {
            if(executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
    }

    private Future<String> getRequestDocument(String documentId,String bearerToken, ExecutorService threadPool){
        return threadPool.submit(() -> {
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            IDocument iDocument = (IDocument) DocumentPersistenceUtility.getUBLDocument(documentId);
            if(iDocument == null){
                return null;
            }

            // product details which will be included in the response
            List<String> catalogIds = new ArrayList<>();
            List<String> lineIds = new ArrayList<>();
            List<List<TextType>> productNames = new ArrayList<>();

            for (ItemType item : iDocument.getItemTypes()) {
                catalogIds.add(item.getCatalogueDocumentReference().getID());
                lineIds.add(item.getManufacturersItemIdentification().getID());
                productNames.add(item.getName());
            }

            return  "{\"items\":"+ "{\"catalogIds\":"+objectMapper.writeValueAsString(catalogIds) +
                        ",\"lineIds\":" + objectMapper.writeValueAsString(lineIds) +
                        ",\"productNames\":" + objectMapper.writeValueAsString(productNames) + "}" +
                    ",\"buyerPartyId\":\""+ iDocument.getBuyerPartyId() +
                    "\",\"buyerPartyFederationId\":\""+ iDocument.getBuyerParty().getFederationInstanceID() +
                    "\",\"sellerPartyId\":\""+ iDocument.getSellerPartyId()+
                    "\",\"sellerPartyFederationId\":\""+ iDocument.getSellerParty().getFederationInstanceID() +"\"}";
        });
    }

    private Future<String> getResponseDocumentStatus(String documentId, ExecutorService threadPool){
        return threadPool.submit(() -> {
            IDocument iDocument = (IDocument) DocumentPersistenceUtility.getUBLDocument(documentId);
            if(iDocument == null){
                return null;
            }

            String documentStatus = iDocument.getDocumentStatus();
            return "{\"documentStatus\":\""+documentStatus+"\"}";
        });
    }

    private Future<String> getCancellationReason(String processInstanceId, ExecutorService threadPool){
        return threadPool.submit(() -> {
            return TrustPersistenceUtility.getCancellationReasonForCollaboration(processInstanceId);
        });
    }

    private Future<String> getCompletionDate(String processInstanceId, ExecutorService threadPool){
        return threadPool.submit(() -> {
            return TrustPersistenceUtility.getCompletionDateForCollaboration(processInstanceId);
        });
    }

    private Future<String> getRequestDate(String processInstanceId, ExecutorService threadPool){
        return threadPool.submit(() -> {
            ProcessDocumentMetadata processDocumentMetadata = ProcessDocumentMetadataDAOUtility.getRequestMetadata(processInstanceId);
            return processDocumentMetadata == null ? null : processDocumentMetadata.getSubmissionDate();
        });
    }

    private Future<String> getResponseDate(String processInstanceId, ExecutorService threadPool){
        return threadPool.submit(() -> {
            ProcessDocumentMetadata processDocumentMetadata = ProcessDocumentMetadataDAOUtility.getResponseMetadata(processInstanceId);
            return processDocumentMetadata == null ? null : processDocumentMetadata.getSubmissionDate();
        });
    }

    private Future<String> serializeObject(Object object, ExecutorService threadPool){
        return threadPool.submit(() -> {
            return JsonSerializationUtility.getObjectMapper().writeValueAsString(object);
        });
    }

    @ApiOperation(value = "",notes = "Gets CollaborationGroup containing the specified process instance. The party information is derived from the given bearer token and " +
            "the service returns CollaborationGroup belonging to this party.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the CollaborationGroup successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No CollaborationGroup found for the given identifier"),
            @ApiResponse(code = 500, message = "Unexpected error while getting the CollaborationGroup")
    })
    @RequestMapping(value = "/processInstance/{processInstanceId}/collaboration-group",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity getAssociatedCollaborationGroup(@ApiParam(value = "Identifier of the process instance", required = true) @PathVariable(value = "processInstanceId", required = true) String processInstanceId,
                                                          @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) throws NimbleException {

        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to get associated collaboration groups for process instance id:%s",processInstanceId);
            executionContext.setRequestLog(requestLog);

            logger.info(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken,executionContext.getUserRoles(), RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString());
            }

            // get person using the given bearer token
            PartyType party;
            try {
                if(executionContext.getOriginalBearerToken() == null){
                    PersonType person = iIdentityClientTyped.getPerson(bearerToken);
                    // get party for the person
                    party = iIdentityClientTyped.getPartyByPersonID(person.getID()).get(0);
                } else{
                    Response response = SpringBridge.getInstance().getDelegateClient().getPersonViaToken(bearerToken, executionContext.getOriginalBearerToken(),executionContext.getClientFederationId());
                    PersonType person = JsonSerializationUtility.getObjectMapper().readValue(HttpResponseUtil.extractBodyFromFeignClientResponse(response),PersonType.class);

                    response = SpringBridge.getInstance().getDelegateClient().getPartyByPersonID(bearerToken, person.getID(),executionContext.getClientFederationId());
                    List<PartyType> partyTypes = JsonSerializationUtility.getObjectMapper().readValue(HttpResponseUtil.extractBodyFromFeignClientResponse(response),new TypeReference<List<PartyType>>() {
                    });
                    party = partyTypes.get(0);
                }

            } catch (IOException e) {
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_GET_PARTY.toString(),Arrays.asList(bearerToken),e);
            }

            CollaborationGroupDAO collaborationGroup = CollaborationGroupDAOUtility.getCollaborationGroupByProcessInstanceIdAndPartyId(processInstanceId, party.getPartyIdentification().get(0).getID(), party.getFederationInstanceID());
            if(collaborationGroup == null) {
                throw new NimbleException(NimbleExceptionMessageCode.NOT_FOUND_NO_COLLABORATION_GROUP_FOR_PROCESS.toString(),Arrays.asList(processInstanceId));
            }

            logger.info("Completed the request to get associated collaboration groups for process instance id:{}",processInstanceId);
            return ResponseEntity.status(HttpStatus.OK).body(HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroup));

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_GET_ASSOCIATED_COLLABORATION_GROUP.toString(),Arrays.asList(bearerToken),e);
        }
    }

    @ApiOperation(value = "", notes = "Exports transaction data according to the specified parameters.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Exported transactions successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while exporting transactions")
    })
    @RequestMapping(value = "/processInstance/export",
            produces = {"application/zip"},
            method = RequestMethod.GET)
    public void exportProcessInstanceData(@ApiParam(value = "Identifier the party as the subject of incoming or outgoing transactions.", required = true) @RequestParam(value = "partyId", required = true) String partyId,
                                          @ApiParam(value = "Identifier of the user who initiated the transactions. This parameter is considered only for the outgoing transactions.", required = false) @RequestParam(value = "userId", required = false) String userId,
                                          @ApiParam(value = "Direction of the transaction. It can be incoming/outgoing. If not provided, all transactions are considered.", required = false) @RequestParam(value = "direction", required = false) String direction,
                                          @ApiParam(value = "Archived status of the CollaborationGroup including the transaction.", required = false) @RequestParam(value = "archived", required = false) Boolean archived,
                                          @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken,
                                          @ApiParam(value = "", required = true) @RequestHeader(value = "federationId", required = true) String federationId,
                                          HttpServletResponse response) throws NimbleException {

        ZipOutputStream zos = null;
        ByteArrayOutputStream tempOutputStream;
        
        try {
            // set request log of ExecutionContext
            String requestLog = String.format("Incoming request to export transactions. party id: %s, user id: %s, direction: %s", partyId, userId, direction);
            executionContext.setRequestLog(requestLog);
            // validate role
            if(!validationUtil.validateRole(bearerToken, executionContext.getUserRoles(),RoleConfig.REQUIRED_ROLES_TO_EXPORT_PROCESS_INSTANCE_DATA)) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_INVALID_ROLE.toString(),true);
            }


            logger.info(requestLog);
            List<TransactionSummary> transactions = ProcessDocumentMetadataDAOUtility.getTransactionSummaries(partyId, federationId,userId, direction, archived, bearerToken);
            ZipEntry zipEntry;
            tempOutputStream = null;
            try {
                zos = new ZipOutputStream(response.getOutputStream());
                // write transaction summary file to the zip
                zipEntry = new ZipEntry("transactions.json");
                zos.putNextEntry(zipEntry);
                tempOutputStream = new ByteArrayOutputStream();
                JsonSerializationUtility.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(tempOutputStream, transactions);
                tempOutputStream.writeTo(zos);
                zos.closeEntry();

            } catch (IOException e) {
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_WRITE_TRANSACTION_SUMMARY.toString(),Arrays.asList(partyId, userId, direction),e,true);
            } finally {
                 if(tempOutputStream != null) {
                     try {
                         tempOutputStream.close();
                     } catch (IOException e) {
                         logger.warn("Failed to close temp output stream", e);
                     }
                 }
            }


            // write the exchanged document itself and associated auxiliary files to the zip
            for (TransactionSummary transaction : transactions) {
                // write the document
                try {
                    zipEntry = new ZipEntry(transaction.getExchangedDocumentId() + ".json");
                    zos.putNextEntry(zipEntry);
                    tempOutputStream = new ByteArrayOutputStream();
                    JsonSerializationUtility.getObjectMapperWithMixIn(BinaryObjectType.class, IgnoreMixin.class).writeValue(tempOutputStream, transaction.getExchangedDocument());
                    tempOutputStream.writeTo(zos);
                    tempOutputStream.close();
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_WRITE_DOCUMENT_TO_ZIP.toString(),Arrays.asList(transaction.getExchangedDocumentId(), partyId, userId, direction),e,true);
                } finally {
                    if(tempOutputStream != null) {
                        try {
                            tempOutputStream.close();
                        } catch (IOException e) {
                            logger.warn("Failed to close temp output stream", e);
                        }
                    }
                }

                List<DocumentReferenceType> additionalDocuments = transaction.getAuxiliaryFiles();
                for (DocumentReferenceType docRef : additionalDocuments) {
                    String fileName = "";
                    try {
                        fileName = transaction.getExchangedDocumentId() + "-" + docRef.getAttachment().getEmbeddedDocumentBinaryObject().getFileName();
                        fileName = fileName.substring(0, fileName.length() < 256 ? fileName.length() : 256);
                        zipEntry = new ZipEntry(fileName);
                        zos.putNextEntry(zipEntry);
                        tempOutputStream = new ByteArrayOutputStream();
                        IOUtils.write(docRef.getAttachment().getEmbeddedDocumentBinaryObject().getValue(), tempOutputStream);
                        tempOutputStream.writeTo(zos);
                        tempOutputStream.close();
                        zos.closeEntry();

                    } catch (IOException e) {
                        throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_WRITE_AUXILIARY_FILE_TO_ZIP.toString(),Arrays.asList(fileName, partyId, userId, direction),e,true);
                    } finally {
                        if(tempOutputStream != null ) {
                            try {
                                tempOutputStream.close();
                            } catch (IOException e) {
                                logger.warn("Failed to close temp output stream", e);
                            }
                        }
                    }
                }
            }

            response.flushBuffer();
            logger.info("Completed request to export transactions. party id: {}, user id: {}, direction: {}", partyId, userId, direction);

        } catch (Exception e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_EXPORT_TRANSACTION.toString(),Arrays.asList(partyId, userId, direction),e,true);
        } finally {
            if(zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    logger.warn("Failed to close zip output stream", e);
                }
            }
        }
    }
}