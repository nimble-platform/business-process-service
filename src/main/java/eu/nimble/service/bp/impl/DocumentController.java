package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.nimble.common.rest.identity.IIdentityClientTyped;
import eu.nimble.service.bp.config.RoleConfig;
import eu.nimble.service.bp.model.hyperjaxb.CollaborationGroupDAO;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.swagger.model.GroupIdTuple;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.order.ObjectFactory;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JAXBUtility;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import eu.nimble.utility.persistence.resource.ResourceValidationUtility;
import eu.nimble.utility.validation.IValidationUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class DocumentController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ResourceValidationUtility resourceValidationUtility;
    @Autowired
    private IIdentityClientTyped identityClient;
    @Autowired
    private IValidationUtil validationUtil;

    @ApiOperation(value = "",notes = "Retrieve Json content of the document with the given id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the specified document"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No document found for the specified id"),
            @ApiResponse(code = 500, message = "Unexpected error while retrieving the document")
    })
    @RequestMapping(value = "/document/json/{documentID}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<Object> getDocumentJsonContent(@ApiParam(value = "The identifier of the document to be received", required = true) @PathVariable("documentID") String documentID,
                                                         @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken
    ) {
        try {
            logger.info("Getting content of document: {}", documentID);
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            Object document = DocumentPersistenceUtility.getUBLDocument(documentID);
            if (document == null) {
                return createResponseEntityAndLog(String.format("No document for id: %s", documentID), HttpStatus.NOT_FOUND);
            }
            try {
                String serializedDocument = JsonSerializationUtility.getObjectMapper().writeValueAsString(document);
                logger.info("Retrieved details of the document: {}", documentID);
                return new ResponseEntity<>(serializedDocument, HttpStatus.OK);

            } catch (JsonProcessingException e) {
                return createResponseEntityAndLog(String.format("Serialization error for document: %s", documentID), e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while getting the content for document: %s", documentID), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Retrieves XML content of the document with the given id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the specified document"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No document found for the specified id"),
            @ApiResponse(code = 500, message = "Unexpected error while retrieving the document")
    })
    @RequestMapping(value = "/document/xml/{documentID}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<String> getDocumentXMLContent(@ApiParam(value = "The identifier of the document to be received", required = true) @PathVariable("documentID") String documentID,
                                                 @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        Object document = DocumentPersistenceUtility.getUBLDocument(documentID);
        if (document == null) {
            return createResponseEntityAndLog(String.format("No document for id: %s", documentID), HttpStatus.NOT_FOUND);
        }

        String documentContentXML = null;
        if(document instanceof OrderType) {
            ObjectFactory factory = new ObjectFactory();
            OrderType order = (OrderType) document;
            documentContentXML = JAXBUtility.serialize(order, factory.createOrder(order));
        } else if(document instanceof OrderResponseSimpleType) {
            eu.nimble.service.model.ubl.orderresponsesimple.ObjectFactory factory = new eu.nimble.service.model.ubl.orderresponsesimple.ObjectFactory();
            OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) document;
            documentContentXML = JAXBUtility.serialize(orderResponse, factory.createOrderResponseSimple(orderResponse));
        } else if(document instanceof RequestForQuotationType) {
            eu.nimble.service.model.ubl.requestforquotation.ObjectFactory factory = new eu.nimble.service.model.ubl.requestforquotation.ObjectFactory();
            RequestForQuotationType requestForQuotation = (RequestForQuotationType) document;
            documentContentXML = JAXBUtility.serialize(requestForQuotation, factory.createRequestForQuotation(requestForQuotation));
        } else if(document instanceof QuotationType) {
            eu.nimble.service.model.ubl.quotation.ObjectFactory factory = new eu.nimble.service.model.ubl.quotation.ObjectFactory();
            QuotationType quotation = (QuotationType) document;
            documentContentXML = JAXBUtility.serialize(quotation, factory.createQuotation(quotation));
        }

        return new ResponseEntity<>(documentContentXML, HttpStatus.OK);
    }
    // The above two operations are to retrieve the document contents

    @ApiOperation(value = "",notes = "Retrieves XML content of the document with the given id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the specified document"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "No document found for the specified id"),
            @ApiResponse(code = 500, message = "Unexpected error while retrieving the document")
    })
    @RequestMapping(value = "/document/{documentID}",
            produces = "application/json",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.PATCH)
    public ResponseEntity updateDocument(@ApiParam(value = "Serialized form of the document exchanged in the updated step of the business process", required = true) @RequestBody String content,
                                         @ApiParam(value = "Identifier of the document", required = true) @PathVariable(value = "documentID", required = true) String documentID,
                                         @ApiParam(value = "Type of the process instance document to be updated", required = true) @RequestParam(value = "documentType") DocumentType documentType,
                                         @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken) {
        try {
            // validate role
            if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
            }

            Object document = DocumentPersistenceUtility.readDocument(documentType, content);

            // get party id in order to validate the hjids
            String partyId;
            try {
                // get person using the given bearer token
                PersonType person = identityClient.getPerson(bearerToken);
                // get party for the person
                PartyType party = identityClient.getPartyByPersonID(person.getID()).get(0);
                partyId = party.getPartyIdentification().get(0).getID();

            } catch (IOException e) {
                return createResponseEntityAndLog(String.format("Failed to retrieve party for token: %s", bearerToken), HttpStatus.UNAUTHORIZED);
            }

            // validate the entity ids
            boolean hjidsBelongToCompany = resourceValidationUtility.hjidsBelongsToParty(document, partyId, Configuration.Standard.UBL.toString());
            if (!hjidsBelongToCompany) {
                return HttpResponseUtil.createResponseEntityAndLog(String.format("Some of the identifiers (hjid fields) do not belong to the party in the passed catalogue: %s", content), null, HttpStatus.BAD_REQUEST, LogLevel.INFO);
            }

            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(partyId);
            document = repositoryWrapper.updateEntity(document);

            return ResponseEntity.ok(JsonSerializationUtility.getObjectMapper().writeValueAsString(document));

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while updating the document id: %s, document type: %s\n, document: %s", documentID, documentType, content), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    @Override
//    @ApiOperation(value = "",notes = "Add a business process document metadata")
    public ResponseEntity<ModelApiResponse> addDocumentMetadata(@RequestBody ProcessDocumentMetadata body,
                                                                @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_WRITE)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try{
            DocumentPersistenceUtility.addDocumentWithMetadata(businessProcessContext.getId(),body, null);
            businessProcessContext.commitDbUpdates();
        }
        catch (Exception e){
            logger.error("Failed to add document metadata",e);
            businessProcessContext.rollbackDbUpdates();
        }
        finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

//    @Override
//    @ApiOperation(value = "",notes = "Update a business process document metadata")
    public ResponseEntity<ModelApiResponse> updateDocumentMetadata(@RequestBody ProcessDocumentMetadata body,
                                                                   @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        ResponseEntity tokenCheck = eu.nimble.service.bp.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try{
            ProcessDocumentMetadataDAOUtility.updateDocumentMetadata(businessProcessContext.getId(),body);
            businessProcessContext.commitDbUpdates();
        }
        catch (Exception e){
            logger.error("Failed to update document metadata",e);
            businessProcessContext.rollbackDbUpdates();
        }
        finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

//    @Override
//    @ApiOperation(value = "",notes = "Delete the business process document metadata together with content by id")
    public ResponseEntity<ModelApiResponse> deleteDocument(@PathVariable("documentID") String documentID,
                                                           @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info(" $$$ Deleting Document for ... {}", documentID);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }
        DocumentPersistenceUtility.deleteDocumentsWithMetadatas(Collections.singletonList(documentID));
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

//    @Override
//    @ApiOperation(value = "",notes = "Get the business process document metadata")
    public ResponseEntity<List<ProcessDocumentMetadata>> getDocuments(@PathVariable("partnerID") String partnerID, @PathVariable("type") String type,
                                                                      @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info(" $$$ Getting Document for partner {}, type {}", partnerID, type);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }
        List<ProcessDocumentMetadataDAO> processDocumentsDAO = ProcessDocumentMetadataDAOUtility.getProcessDocumentMetadata(partnerID, type);
        List<ProcessDocumentMetadata> processDocuments = new ArrayList<>();
        for(ProcessDocumentMetadataDAO processDocumentDAO: processDocumentsDAO) {
            ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
            processDocuments.add(processDocument);
        }

        return new ResponseEntity<>(processDocuments, HttpStatus.OK);
    }

//    @Override
//    @ApiOperation(value = "",notes = "Get the business process document metadata")
    public ResponseEntity<List<ProcessDocumentMetadata>> getDocuments(@PathVariable("partnerID") String partnerID, @PathVariable("type") String type, @PathVariable("source") String source,
                                                                      @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info(" $$$ Getting Document for partner {}, type {}, source {}", partnerID, type, source);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }
        List<ProcessDocumentMetadataDAO> processDocumentsDAO = ProcessDocumentMetadataDAOUtility.getProcessDocumentMetadata(partnerID, type, source);
        List<ProcessDocumentMetadata> processDocuments = new ArrayList<>();
        for(ProcessDocumentMetadataDAO processDocumentDAO: processDocumentsDAO) {
            ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
            processDocuments.add(processDocument);
        }
        return new ResponseEntity<>(processDocuments, HttpStatus.OK);
    }

//    @Override
//    @ApiOperation(value = "",notes = "Get the business process document metadata")
    public ResponseEntity<List<ProcessDocumentMetadata>> getDocuments(@PathVariable("partnerID") String partnerID, @PathVariable("type") String type,
            @PathVariable("source") String source, @PathVariable("status") String status,
                                                                      @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info(" $$$ Getting Document for partner {}, type {}, status {}, source {}", partnerID, type, status, source);
        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }
        List<ProcessDocumentMetadataDAO> processDocumentsDAO = ProcessDocumentMetadataDAOUtility.getProcessDocumentMetadata(partnerID, type, status, source);
        List<ProcessDocumentMetadata> processDocuments = new ArrayList<>();
        for(ProcessDocumentMetadataDAO processDocumentDAO: processDocumentsDAO) {
            ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
            processDocuments.add(processDocument);
        }
        return new ResponseEntity<>(processDocuments, HttpStatus.OK);
    }

    @ApiOperation(value = "",notes = "Retrieve group id tuple for the given document id")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved group id tuple for the given document id",response = GroupIdTuple.class),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 404, message = "There does not exist a process document metadata for the given document id")
    })
    @RequestMapping(value = "/document/{documentId}/group-id-tuple",
            method = RequestMethod.GET)
    public ResponseEntity getGroupIdTuple(@ApiParam(value = "The identifier of the process instance group to be checked",required = true) @PathVariable("documentId") String documentId,
                                                             @ApiParam(value = "The identifier of the process instance group to be checked",required = true) @QueryParam("partyId") String partyId,
                                                             @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken,
                                          @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="federationId", required=true) String federationId) {
        logger.info("Retrieving group id tuple for document: {} and party: {}",documentId,partyId);

        // validate role
        if(!validationUtil.validateRole(bearerToken, RoleConfig.REQUIRED_ROLES_PURCHASES_OR_SALES_READ)) {
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog("Invalid role", HttpStatus.UNAUTHORIZED);
        }

        ProcessDocumentMetadataDAO processDocumentMetadataDAO = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
        if(processDocumentMetadataDAO == null){
            String msg = String.format("There does not exist a process document metadata for : %s", documentId);
            logger.error(msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
        }

        GroupIdTuple groupIdTuple = new GroupIdTuple();
        // get the collaboration group containing the process instance for the party
        CollaborationGroupDAO collaborationGroup = CollaborationGroupDAOUtility.getCollaborationGroupDAO(partyId,federationId,processDocumentMetadataDAO.getProcessInstanceID());
        // get the identifier of process instance group containing the process instance
        for(ProcessInstanceGroupDAO processInstanceGroupDAO:collaborationGroup.getAssociatedProcessInstanceGroups()){
            if(processInstanceGroupDAO.getProcessInstanceIDs().contains(processDocumentMetadataDAO.getProcessInstanceID())){
                groupIdTuple.setProcessInstanceGroupId(processInstanceGroupDAO.getID());
                break;
            }
        }

        groupIdTuple.setCollaborationGroupId(collaborationGroup.getHjid().toString());

        logger.info("Retrieved group id tuple for document: {} and party: {}",documentId,partyId);
        return ResponseEntity.ok(groupIdTuple);
    }
}
