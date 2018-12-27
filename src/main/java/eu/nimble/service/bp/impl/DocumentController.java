package eu.nimble.service.bp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.impl.persistence.util.DAOUtility;
import eu.nimble.service.bp.impl.persistence.util.DocumentDAOUtility;
import eu.nimble.service.bp.impl.persistence.util.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.serialization.Serializer;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.api.DocumentApi;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.order.ObjectFactory;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.JAXBUtility;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

import static eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class DocumentController implements DocumentApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ApiOperation(value = "",notes = "Retrieve Json content of the document with the given id")
    @RequestMapping(value = "/document/json/{documentID}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<Object> getDocumentJsonContent(@PathVariable("documentID") String documentID) {
        try {
            logger.info("Getting content of document: {}", documentID);
            Object document = DocumentDAOUtility.getUBLDocument(documentID);
            if (document == null) {
                return createResponseEntityAndLog(String.format("No document for id: %s", documentID), HttpStatus.NOT_FOUND);
            }
            try {
                String serializedDocument = Serializer.getDefaultObjectMapper().writeValueAsString(document);
                logger.info("Retrieved details of the document: {}", documentID);
                return new ResponseEntity<>(serializedDocument, HttpStatus.OK);

            } catch (JsonProcessingException e) {
                return createResponseEntityAndLog(String.format("Serialization error for document: %s", documentID), e, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            return createResponseEntityAndLog(String.format("Unexpected error while getting the content for document: %s", documentID), e, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "",notes = "Retrieve XML content of the document with the given id")
    @RequestMapping(value = "/document/xml/{documentID}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<String> getDocumentXMLContent(@PathVariable("documentID") String documentID) {
        Object document = DocumentDAOUtility.getUBLDocument(documentID);

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

    @Override
    @ApiOperation(value = "",notes = "Add a business process document metadata")
    public ResponseEntity<ModelApiResponse> addDocumentMetadata(@RequestBody ProcessDocumentMetadata body) {
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try{
            DocumentDAOUtility.addDocumentWithMetadata(businessProcessContext.getId(),body, null);
        }
        catch (Exception e){
            businessProcessContext.handleExceptions();
        }
        finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Update a business process document metadata")
    public ResponseEntity<ModelApiResponse> updateDocumentMetadata(@RequestBody ProcessDocumentMetadata body) {
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(null);
        try{
            DocumentDAOUtility.updateDocumentMetadata(businessProcessContext.getId(),body);
        }
        catch (Exception e){
            businessProcessContext.handleExceptions();
        }
        finally {
            BusinessProcessContextHandler.getBusinessProcessContextHandler().deleteBusinessProcessContext(businessProcessContext.getId());
        }
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Delete the business process document metadata together with content by id")
    public ResponseEntity<ModelApiResponse> deleteDocument(@PathVariable("documentID") String documentID) {
        logger.info(" $$$ Deleting Document for ... {}", documentID);
        DocumentDAOUtility.deleteDocumentWithMetadata(documentID);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    @ApiOperation(value = "",notes = "Get the business process document metadata")
    public ResponseEntity<List<ProcessDocumentMetadata>> getDocuments(@PathVariable("partnerID") String partnerID, @PathVariable("type") String type) {
        logger.info(" $$$ Getting Document for partner {}, type {}", partnerID, type);
        List<ProcessDocumentMetadataDAO> processDocumentsDAO = DAOUtility.getProcessDocumentMetadata(partnerID, type);
        List<ProcessDocumentMetadata> processDocuments = new ArrayList<>();
        for(ProcessDocumentMetadataDAO processDocumentDAO: processDocumentsDAO) {
            ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
            processDocuments.add(processDocument);
        }

        return new ResponseEntity<>(processDocuments, HttpStatus.OK);
    }

    @Override
    @ApiOperation(value = "",notes = "Get the business process document metadata")
    public ResponseEntity<List<ProcessDocumentMetadata>> getDocuments(@PathVariable("partnerID") String partnerID, @PathVariable("type") String type, @PathVariable("source") String source) {
        logger.info(" $$$ Getting Document for partner {}, type {}, source {}", partnerID, type, source);
        List<ProcessDocumentMetadataDAO> processDocumentsDAO = DAOUtility.getProcessDocumentMetadata(partnerID, type, source);
        List<ProcessDocumentMetadata> processDocuments = new ArrayList<>();
        for(ProcessDocumentMetadataDAO processDocumentDAO: processDocumentsDAO) {
            ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
            processDocuments.add(processDocument);
        }
        return new ResponseEntity<>(processDocuments, HttpStatus.OK);
    }

    @Override
    @ApiOperation(value = "",notes = "Get the business process document metadata")
    public ResponseEntity<List<ProcessDocumentMetadata>> getDocuments(@PathVariable("partnerID") String partnerID, @PathVariable("type") String type,
            @PathVariable("source") String source, @PathVariable("status") String status) {
        logger.info(" $$$ Getting Document for partner {}, type {}, status {}, source {}", partnerID, type, status, source);
        List<ProcessDocumentMetadataDAO> processDocumentsDAO = DAOUtility.getProcessDocumentMetadata(partnerID, type, status, source);
        List<ProcessDocumentMetadata> processDocuments = new ArrayList<>();
        for(ProcessDocumentMetadataDAO processDocumentDAO: processDocumentsDAO) {
            ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
            processDocuments.add(processDocument);
        }
        return new ResponseEntity<>(processDocuments, HttpStatus.OK);
    }

}
