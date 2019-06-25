package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.impl.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.order.ObjectFactory;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.JAXBUtility;
import eu.nimble.utility.JsonSerializationUtility;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
            // check token
            ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
            if (tokenCheck != null) {
                return tokenCheck;
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
        // check token
        ResponseEntity tokenCheck = eu.nimble.service.bp.impl.util.HttpResponseUtil.checkToken(bearerToken);
        if (tokenCheck != null) {
            return tokenCheck;
        }

        Object document = DocumentPersistenceUtility.getUBLDocument(documentID);

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

//    @Override
//    @ApiOperation(value = "",notes = "Add a business process document metadata")
    public ResponseEntity<ModelApiResponse> addDocumentMetadata(@RequestBody ProcessDocumentMetadata body,
                                                                @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try{
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            DocumentPersistenceUtility.addDocumentWithMetadata(businessProcessContext.getId(),body, null);
        }
        catch (Exception e){
            businessProcessContext.handleExceptions();
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
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try{
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            ProcessDocumentMetadataDAOUtility.updateDocumentMetadata(businessProcessContext.getId(),body);
        }
        catch (Exception e){
            businessProcessContext.handleExceptions();
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
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (IOException e){
            String msg = String.format("Failed to delete document: %s",documentID);
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        DocumentPersistenceUtility.deleteDocumentsWithMetadatas(Collections.singletonList(documentID));
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

//    @Override
//    @ApiOperation(value = "",notes = "Get the business process document metadata")
    public ResponseEntity<List<ProcessDocumentMetadata>> getDocuments(@PathVariable("partnerID") String partnerID, @PathVariable("type") String type,
                                                                      @ApiParam(value = "The Bearer token provided by the identity service" ,required=true ) @RequestHeader(value="Authorization", required=true) String bearerToken) {
        logger.info(" $$$ Getting Document for partner {}, type {}", partnerID, type);
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (IOException e){
            String msg = String.format("Failed to retrieve documents for partner: %s, type: %s",partnerID,type);
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
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
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (IOException e){
            String msg = String.format("Failed to retrieve documents for partner: %s, type: %s, source: %s",partnerID,type,source);
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
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
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(bearerToken);
            if(!isValid){
                String msg = String.format("No user exists for the given token : %s",bearerToken);
                logger.error(msg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (IOException e){
            String msg = String.format("Failed to retrieve documents for partner: %s, type: %s, status: %s,source: %s",partnerID,type,status,source);
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        List<ProcessDocumentMetadataDAO> processDocumentsDAO = ProcessDocumentMetadataDAOUtility.getProcessDocumentMetadata(partnerID, type, status, source);
        List<ProcessDocumentMetadata> processDocuments = new ArrayList<>();
        for(ProcessDocumentMetadataDAO processDocumentDAO: processDocumentsDAO) {
            ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
            processDocuments.add(processDocument);
        }
        return new ResponseEntity<>(processDocuments, HttpStatus.OK);
    }

}
