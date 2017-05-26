package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentDAO;
import eu.nimble.service.bp.impl.util.DAOUtility;
import eu.nimble.service.bp.impl.util.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.swagger.api.DocumentApi;
import eu.nimble.service.bp.swagger.model.ProcessDocument;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yildiray on 5/25/2017.
 */
@Controller
public class DocumentController implements DocumentApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public ResponseEntity<ModelApiResponse> addDocument(@RequestBody ProcessDocument body) {
        logger.info(" $$$ Adding ProcessDocument: ");
        logger.debug(" $$$ {}", body.toString());
        ProcessDocumentDAO processDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocument_DAO(body);
        HibernateUtility.getInstance("bp-data-model").persist(processDocumentDAO);
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<ModelApiResponse> deleteDocument(@PathVariable("documentID") String documentID) {
        logger.info(" $$$ Deleting Document for ... {}", documentID);
        ProcessDocumentDAO processDocumentDAO = DAOUtility.getProcessDocument(documentID);
        HibernateUtility.getInstance("bp-data-model").delete(ProcessDocumentDAO.class, processDocumentDAO.getHjid());
        return HibernateSwaggerObjectMapper.getApiResponse();
    }

    @Override
    public ResponseEntity<ProcessDocument> getDocument(@PathVariable("documentID") String documentID) {
        logger.info(" $$$ Getting Document for ... {}", documentID);
        ProcessDocumentDAO processDocumentDAO = DAOUtility.getProcessDocument(documentID);
        ProcessDocument processDocument = HibernateSwaggerObjectMapper.createProcessDocument(processDocumentDAO);
        return new ResponseEntity<>(processDocument, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<ProcessDocument>> getDocuments(@PathVariable("partnerID") String partnerID, @PathVariable("type") String type) {
        logger.info(" $$$ Getting Document for partner {}, type {}", partnerID, type);
        List<ProcessDocumentDAO> processDocumentsDAO = DAOUtility.getProcessDocuments(partnerID, type);
        List<ProcessDocument> processDocuments = new ArrayList<>();
        for(ProcessDocumentDAO processDocumentDAO: processDocumentsDAO) {
            ProcessDocument processDocument = HibernateSwaggerObjectMapper.createProcessDocument(processDocumentDAO);
            processDocuments.add(processDocument);
        }

        return new ResponseEntity<>(processDocuments, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<ProcessDocument>> getDocuments(@PathVariable("partnerID") String partnerID, @PathVariable("type") String type, @PathVariable("source") String source) {
        logger.info(" $$$ Getting Document for partner {}, type {}, source {}", partnerID, type, source);
        List<ProcessDocumentDAO> processDocumentsDAO = DAOUtility.getProcessDocuments(partnerID, type, source);
        List<ProcessDocument> processDocuments = new ArrayList<>();
        for(ProcessDocumentDAO processDocumentDAO: processDocumentsDAO) {
            ProcessDocument processDocument = HibernateSwaggerObjectMapper.createProcessDocument(processDocumentDAO);
            processDocuments.add(processDocument);
        }
        return new ResponseEntity<>(processDocuments, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<ProcessDocument>> getDocuments(@PathVariable("partnerID") String partnerID, @PathVariable("type") String type,
            @PathVariable("source") String source, @PathVariable("status") String status) {
        logger.info(" $$$ Getting Document for partner {}, type {}, status {}, source {}", partnerID, type, status, source);
        List<ProcessDocumentDAO> processDocumentsDAO = DAOUtility.getProcessDocuments(partnerID, type, status, source);
        List<ProcessDocument> processDocuments = new ArrayList<>();
        for(ProcessDocumentDAO processDocumentDAO: processDocumentsDAO) {
            ProcessDocument processDocument = HibernateSwaggerObjectMapper.createProcessDocument(processDocumentDAO);
            processDocuments.add(processDocument);
        }
        return new ResponseEntity<>(processDocuments, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ModelApiResponse> updateDocument(@RequestBody ProcessDocument body) {
        logger.info(" $$$ Updating ProcessDocument: ");
        logger.debug(" $$$ {}", body.toString());

        ProcessDocumentDAO storedDocumentDAO = DAOUtility.getProcessDocument(body.getDocumentID());

        ProcessDocumentDAO newDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocument_DAO(body);

        newDocumentDAO.setHjid(storedDocumentDAO.getHjid());

        HibernateUtility.getInstance("bp-data-model").update(newDocumentDAO);

        return HibernateSwaggerObjectMapper.getApiResponse();
    }
}
