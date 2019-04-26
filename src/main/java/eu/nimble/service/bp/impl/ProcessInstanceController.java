package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceStatus;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessInstanceDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.TrustPersistenceUtility;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.document.IDocument;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.serialization.JsonSerializer;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
/**
 * Created by dogukan on 09.08.2018.
 */

@Controller
public class ProcessInstanceController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ResourceValidationUtility resourceValidationUtil;

    @ApiOperation(value = "",notes = "Cancels the process instance with the given id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Cancelled the process instance successfully"),
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
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            ProcessInstanceDAO instanceDAO = ProcessInstanceDAOUtility.getById(processInstanceId);
            // check whether the process instance with the given id exists or not
            if(instanceDAO == null){
                logger.error("There does not exist a process instance with id:{}",processInstanceId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("There does not exist a process instance with the given id");
            }
            // cancel the process
            CamundaEngine.cancelProcessInstance(processInstanceId);
            // change status of the process
            instanceDAO.setStatus(ProcessInstanceStatus.CANCELLED);
            new JPARepositoryFactory().forBpRepository().updateEntity(instanceDAO);
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
            @ApiResponse(code = 404, message = "There does not exist a process instance with the given id"),
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
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            ProcessDocumentMetadata processDocumentMetadata = ProcessDocumentMetadataDAOUtility.getRequestMetadata(processInstanceID);
            Object document = DocumentPersistenceUtility.readDocument(documentType, content);
            // validate the entity ids
            boolean hjidsBelongToCompany = resourceValidationUtil.hjidsBelongsToParty(document, processDocumentMetadata.getInitiatorID(), Configuration.Standard.UBL.toString());
            if(!hjidsBelongToCompany) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Some of the identifiers (hjid fields) do not belong to the party in the passed catalogue: %s", content), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }

            ProcessInstanceDAO instanceDAO = ProcessInstanceDAOUtility.getById(processInstanceID);
            // check whether the process instance with the given id exists or not
            if(instanceDAO == null){
                logger.error("There does not exist a process instance with id:{}",processInstanceID);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("There does not exist a process instance with the given id");
            }
            // update creator user id of metadata
            processDocumentMetadata.setCreatorUserID(creatorUserID);
            ProcessDocumentMetadataDAOUtility.updateDocumentMetadata(businessProcessContext.getId(),processDocumentMetadata);
            // update the corresponding document
            DocumentPersistenceUtility.updateDocument(businessProcessContext.getId(), document, processDocumentMetadata.getDocumentID(), documentType, processDocumentMetadata.getInitiatorID());
        }
        catch (Exception e) {
            logger.error("Failed to update the process instance with id:{}",processInstanceID,e);
            businessProcessContext.handleExceptions();
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
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
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
            @ApiResponse(code = 500, message = "Unexpected error while getting the details of process instance")
    })
    @RequestMapping(value = "/processInstance/{processInstanceId}/details",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.GET)
    public ResponseEntity getDashboardProcessInstanceDetails(@ApiParam(value = "Identifier of the process instance", required = true) @PathVariable(value = "processInstanceId", required = true) String processInstanceId,
                                                             @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            logger.info("Getting the details for process instance: {}", processInstanceId);
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
            }

            List<HistoricVariableInstance> variableInstanceList = CamundaEngine.getVariableInstances(processInstanceId);

            Future<String> variableInstances = serializeObject(variableInstanceList);
            Future<String> processInstance = serializeObject(CamundaEngine.getProcessInstance(processInstanceId));
            Future<String> lastActivityInstance = serializeObject(CamundaEngine.getLastActivityInstance(processInstanceId));

            // get request and response document
            // get request and response metadata as well
            Future<String> requestDocument = null;
            Future<String> responseDocument = null;
            ProcessDocumentMetadata requestMetadata = null;
            ProcessDocumentMetadata responseMetadata = null;
            for(HistoricVariableInstance variableInstance:variableInstanceList){
                // request document
                if(variableInstance.getName().contentEquals("initialDocumentID")){
                    String documentId =  variableInstance.getValue().toString();
                    // request document
                    requestDocument = getRequestDocument(documentId);
                    // request metadata
                    requestMetadata = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId));
                }
                // response document
                else if(variableInstance.getName().contentEquals("responseDocumentID")){
                    String documentId =  variableInstance.getValue().toString();
                    // response document
                    responseDocument = getResponseDocument(documentId);
                    // response metadata
                    responseMetadata = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId));
                }
            }
            // get request creator and response creator user info
            Future<String> requestCreatorUser = null;
            Future<String> responseCreatorUser = null;
            if(requestMetadata != null){
                requestCreatorUser = getCreatorUser(bearerToken,requestMetadata.getCreatorUserID());
            }
            if(responseMetadata != null){
                responseCreatorUser = getCreatorUser(bearerToken,responseMetadata.getCreatorUserID());
            }

            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

            JsonSerializer jsonSerializer = new JsonSerializer();
            jsonSerializer.put("requestDocument",requestDocument == null ? null: requestDocument.get());
            jsonSerializer.put("responseDocument",responseDocument == null ? null : responseDocument.get());
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
        }
    }

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private Future<String> getRequestDocument(String documentId){
        return executorService.submit(() -> {
            ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
            IDocument iDocument = (IDocument) DocumentPersistenceUtility.getUBLDocument(documentId);
            if(iDocument == null){
                return null;
            }
            return  "{\"item\":"+objectMapper.writeValueAsString(iDocument.getItemType()) +
                    ",\"buyerPartyId\":\""+ iDocument.getBuyerPartyId() +
                    "\",\"buyerPartyName\":"+objectMapper.writeValueAsString(iDocument.getBuyerPartyName())+
                    ",\"sellerPartyId\":\""+ iDocument.getSellerPartyId()+
                    "\",\"sellerPartyName\":"+objectMapper.writeValueAsString(iDocument.getSellerPartyName())+"}";
        });
    }

    private Future<String> getResponseDocument(String documentId){
        return executorService.submit(() -> {
            IDocument iDocument = (IDocument) DocumentPersistenceUtility.getUBLDocument(documentId);
            if(iDocument == null){
                return null;
            }

            String isAccepted = iDocument.isAccepted();

            String string = "{";
            if(isAccepted != null){
                string += "\"acceptedIndicator\":\""+isAccepted+"\"";
            }
            string += "}";
            return string;
        });
    }

    private Future<String> getCreatorUser(String bearerToken,String userId){
        return executorService.submit(() -> JsonSerializationUtility.getObjectMapper().writeValueAsString(
                SpringBridge.getInstance().getiIdentityClientTyped().getPerson(bearerToken,userId)));
    }

    private Future<String> serializeObject(Object object){
        return executorService.submit(() -> JsonSerializationUtility.getObjectMapper().writeValueAsString(object));
    }
}