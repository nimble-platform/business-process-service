package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.hyperjaxb.CollaborationGroupDAO;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceStatus;
import eu.nimble.service.bp.util.BusinessProcessEvent;
import eu.nimble.service.bp.model.export.TransactionSummary;
import eu.nimble.service.bp.util.camunda.CamundaEngine;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.CataloguePersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.PartyPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.TrustPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.document.IDocument;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.LoggerUtils;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.serialization.JsonSerializer;
import eu.nimble.utility.serialization.MixInIgnoreType;
import eu.nimble.utility.validation.IValidationUtil;
import eu.nimble.utility.validation.ValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
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
                                                @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.debug("Cancelling process instance with id: {}",processInstanceId);

        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            ProcessInstanceDAO instanceDAO = ProcessInstanceDAOUtility.getById(processInstanceId);
            // check whether the process instance with the given id exists or not
            if(instanceDAO == null){
                logger.error("There does not exist a process instance with id:{}",processInstanceId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("There does not exist a process instance with the given id");
            }
            // check the status of process instance
            if(instanceDAO.getStatus().equals(ProcessInstanceStatus.CANCELLED)){
                String msg = String.format("The process instance with id: %s is already cancelled",processInstanceId);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
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
            logger.error("Failed to cancel the process instance with id:{}",processInstanceId,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to cancel the process instance with the given id");
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
                                                @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {


        logger.debug("Updating process instance with id: {}",processInstanceID);

        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);

        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            ProcessDocumentMetadata processDocumentMetadata = ProcessDocumentMetadataDAOUtility.getRequestMetadata(processInstanceID,businessProcessContext.getBpRepository());
            if(processDocumentMetadata == null){
                logger.error("There does not exist a document metadata for the process instance with id:{}",processInstanceID);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("There does not exist a document metadata for the process instance with the given id");
            }
            Object document = DocumentPersistenceUtility.readDocument(documentType, content);
            // validate the entity ids
            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(document, processDocumentMetadata.getInitiatorID(), Configuration.Standard.UBL.toString());
            if(!hjidsBelongToCompany) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Some of the identifiers (hjid fields) do not belong to the party in the passed catalogue: %s", content), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }

            ProcessInstanceDAO instanceDAO = ProcessInstanceDAOUtility.getById(processInstanceID,businessProcessContext.getBpRepository());
            // update creator user id of metadata
            processDocumentMetadata.setCreatorUserID(creatorUserID);
            ProcessDocumentMetadataDAOUtility.updateDocumentMetadata(businessProcessContext.getId(),processDocumentMetadata);
            // update the corresponding document
            DocumentPersistenceUtility.updateDocument(businessProcessContext.getId(), document, processDocumentMetadata.getInitiatorID());

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
            logger.error("Failed to update the process instance with id:{}",processInstanceID,e);
            businessProcessContext.rollbackDbUpdates();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update the process instance with the given id");
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
                                  @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            logger.info("Getting rating status for process instance: {}, party: {}", processInstanceId, partyId);
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            Boolean rated = TrustPersistenceUtility.processInstanceIsRated(partyId,processInstanceId);

            logger.info("Retrieved rating status for process instance: {}, party: {}", processInstanceId, partyId);
            return ResponseEntity.ok(rated.toString());

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the order content for process instance id: %s, party: %s", processInstanceId, partyId), e, HttpStatus.INTERNAL_SERVER_ERROR);
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
                                                             @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        ExecutorService executorService = null;
        try {
            logger.info("Getting the details for process instance: {}", processInstanceId);
            executorService = Executors.newCachedThreadPool();

            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            // check the existence of process instance
            ProcessInstanceDAO instanceDAO = ProcessInstanceDAOUtility.getById(processInstanceId);
            if(instanceDAO == null){
                String msg = String.format("There does not exist a process instance with id: %s",processInstanceId);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }

            List<HistoricVariableInstance> variableInstanceList = CamundaEngine.getVariableInstances(processInstanceId);

            Future<String> variableInstances = serializeObject(variableInstanceList, executorService);
            Future<String> processInstance = serializeObject(CamundaEngine.getProcessInstance(processInstanceId), executorService);
            Future<String> lastActivityInstance = serializeObject(CamundaEngine.getLastActivityInstance(processInstanceId), executorService);


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
                    requestDocument = getRequestDocument(documentId, executorService);
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
            // get request creator and response creator user info
            Future<String> requestCreatorUser = null;
            Future<String> responseCreatorUser = null;
            if(requestMetadata != null){
                requestCreatorUser = getCreatorUser(bearerToken,requestMetadata.getCreatorUserID(), executorService);
            }
            if(responseMetadata != null){
                responseCreatorUser = getCreatorUser(bearerToken,responseMetadata.getCreatorUserID(), executorService);
            }

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

            JsonSerializer jsonSerializer = new JsonSerializer();
            jsonSerializer.put("requestDocument",requestDocument == null ? null: requestDocument.get());
            jsonSerializer.put("responseDocumentStatus",responseDocumentStatus == null ? null : responseDocumentStatus.get());
            jsonSerializer.put("requestMetadata",objectMapper.writeValueAsString(requestMetadata));
            jsonSerializer.put("responseMetadata",objectMapper.writeValueAsString(responseMetadata));
            jsonSerializer.put("variableInstance",variableInstances.get());
            jsonSerializer.put("lastActivityInstance",lastActivityInstance.get());
            jsonSerializer.put("processInstance",processInstance.get());
            jsonSerializer.put("requestCreatorUser",requestCreatorUser == null ? null : requestCreatorUser.get());
            jsonSerializer.put("responseCreatorUser",responseCreatorUser == null ? null : responseCreatorUser.get());

            logger.info("Retrieved the details for process instance: {}", processInstanceId);
            return ResponseEntity.ok(jsonSerializer.toString());
        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Unexpected error while getting the details for process instance id: %s", processInstanceId), e, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if(executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
    }

    private Future<String> getRequestDocument(String documentId, ExecutorService threadPool){
        return threadPool.submit(() -> {
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            IDocument iDocument = (IDocument) DocumentPersistenceUtility.getUBLDocument(documentId);
            if(iDocument == null){
                return null;
            }
            ItemType item = iDocument.getItemType();
            // get catalogue line to check whether the product is deleted or not
            CatalogueLineType catalogueLine = CataloguePersistenceUtility.getCatalogueLine(item.getCatalogueDocumentReference().getID(), item.getManufacturersItemIdentification().getID(),false);
            return  "{\"item\":"+objectMapper.writeValueAsString(item) +
                    ",\"isProductDeleted\":" + (catalogueLine == null) +
                    ",\"buyerPartyId\":\""+ iDocument.getBuyerPartyId() +
                    "\",\"buyerPartyName\":"+objectMapper.writeValueAsString(iDocument.getBuyerPartyName())+
                    ",\"sellerPartyId\":\""+ iDocument.getSellerPartyId()+
                    "\",\"sellerPartyName\":"+objectMapper.writeValueAsString(iDocument.getSellerPartyName())+"}";
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

    private Future<String> getCreatorUser(String bearerToken,String userId, ExecutorService threadPool){
        return threadPool.submit(() -> JsonSerializationUtility.getObjectMapper().writeValueAsString(
                PartyPersistenceUtility.getPerson(bearerToken,userId)
        ));
    }

    private Future<String> serializeObject(Object object, ExecutorService threadPool){
        return threadPool.submit(() -> JsonSerializationUtility.getObjectMapper().writeValueAsString(object));
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
                                                          @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {

        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            // get person using the given bearer token
            PartyType party;
            try {
                PersonType person = iIdentityClientTyped.getPerson(bearerToken);
                // get party for the person
                party = iIdentityClientTyped.getPartyByPersonID(person.getID()).get(0);

            } catch (IOException e) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to retrieve party for the token: %s", bearerToken), e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            CollaborationGroupDAO collaborationGroup = CollaborationGroupDAOUtility.getCollaborationGroupByProcessInstanceIdAndPartyId(processInstanceId, party.getPartyIdentification().get(0).getID());
            if(collaborationGroup == null) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("No CollaborationGroup for the process instance id: %s", processInstanceId), null, HttpStatus.NOT_FOUND, LogLevel.INFO);
            }

            return ResponseEntity.status(HttpStatus.OK).body(HibernateSwaggerObjectMapper.convertCollaborationGroupDAO(collaborationGroup));

        } catch (Exception e) {
            return HttpResponseUtil.createResponseEntityAndLog(String.format("Failed to retrieve party for the token: %s", bearerToken), e, HttpStatus.INTERNAL_SERVER_ERROR);
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
                                          HttpServletResponse response) {

        ZipOutputStream zos = null;
        ByteArrayOutputStream tempOutputStream;
        
        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_ADMIN)) {
                eu.nimble.utility.HttpResponseUtil.writeMessageServletResponseAndLog(response, "Invalid role", HttpStatus.UNAUTHORIZED);
                return;
            }


            logger.info("Incoming request to export transactions. party id: {}, user id: {}, direction: {}", partyId, userId, direction);
            List<TransactionSummary> transactions = ProcessDocumentMetadataDAOUtility.getTransactionSummaries(partyId, userId, direction, archived, bearerToken);
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
                HttpResponseUtil.writeMessageServletResponseAndLog(
                        response,
                        String.format("Failed to write the transaction summary to the zip file for party id: %s, user id: %s, direction: %s", partyId, userId, direction),
                        e,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        LogLevel.ERROR);
                return;

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
                    JsonSerializationUtility.getObjectMapperWithMixIn(BinaryObjectType.class, MixInIgnoreType.class).writeValue(tempOutputStream, transaction.getExchangedDocument());
                    tempOutputStream.writeTo(zos);
                    tempOutputStream.close();
                    zos.closeEntry();
                } catch (IOException e) {
                    HttpResponseUtil.writeMessageServletResponseAndLog(
                            response,
                            String.format("Failed to write document: {} to the zip file for party id: %s, user id: %s, direction: %s", transaction.getExchangedDocumentId(), partyId, userId, direction),
                            e,
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            LogLevel.ERROR);
                    return;
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
                        HttpResponseUtil.writeMessageServletResponseAndLog(
                                response,
                                String.format("Failed to write auxiliary file: {} to the zip file for party id: %s, user id: %s, direction: %s", fileName, partyId, userId, direction),
                                e,
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                LogLevel.ERROR);
                        return;

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
            HttpResponseUtil.writeMessageServletResponseAndLog(
                    response,
                    String.format("Unexpected error while exporting transactions for party id: %s, user id: %s, direction: %s", partyId, userId, direction),
                    e,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    LogLevel.ERROR);
            return;

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