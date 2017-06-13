package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.impl.util.DAOUtility;
import eu.nimble.service.bp.impl.util.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.DocumentDAOUtility;
import eu.nimble.service.bp.swagger.api.DocumentApi;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.model.ubl.order.ObjectFactory;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.utility.JAXBUtility;
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

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class DocumentController implements DocumentApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/document/json/{documentID}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<Object> getDocumentJsonContent(@PathVariable("documentID") String documentID) {
        Object document = DocumentDAOUtility.getUBLDocument(documentID);
        return new ResponseEntity<>(document, HttpStatus.OK);
    }

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
        }

        return new ResponseEntity<>(documentContentXML, HttpStatus.OK);
    }
    // The above two operations are to retrieve the document contents

    @Override
    public ResponseEntity<ModelApiResponse> addDocumentMetadata(@RequestBody ProcessDocumentMetadata body) {
        DocumentDAOUtility.addDocumentWithMetadata(body, null);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateDocumentMetadata(@RequestBody ProcessDocumentMetadata body) {
        DocumentDAOUtility.updateDocumentMetadata(body);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteDocument(@PathVariable("documentID") String documentID) {
        logger.info(" $$$ Deleting Document for ... {}", documentID);
        DocumentDAOUtility.deleteDocumentWithMetadata(documentID);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    /*@Override
    public ResponseEntity<ProcessDocumentContent> getDocumentContent(@PathVariable("documentID") String documentID) {
        logger.info(" $$$ Getting Document for ... {}", documentID);
        ProcessDocumentContent processDocumentContent = new ProcessDocumentContent();
        processDocumentContent.setDocumentID(documentID);

        String documentJsonContent = DocumentDAOUtility.createJsonContent(documentID);

        processDocumentContent.setContent(documentJsonContent);

        return new ResponseEntity<>(processDocumentContent, HttpStatus.OK);
    }*/

    @Override
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
