package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * API tests for DefaultApi
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@Ignore
public class DocumentControllerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Add a business process docuent
     */
    @Test
    public void addDocumentMetadataTest() {
        ProcessDocumentMetadata body = TestObjectFactory.createBusinessDocumentMetadata();
        String url = "http://localhost:" + port +"/document";

        ResponseEntity<ModelApiResponse> response = restTemplate.postForEntity(url, body, ModelApiResponse.class);

        logger.info(" $$$ Test response {} ", response.toString());

        assertEquals(200, response.getBody().getCode().intValue());
    }

    /**
     * Deletes the business process document by id
     */
    @Test
    public void z_deleteDocumentTest() {
        String documentID = TestObjectFactory.getDocumentID();
        String url = "http://localhost:" + port +"/document/{documentID}";

        Map<String, String> params = new HashMap<String, String>();
        params.put("documentID", documentID);

        restTemplate.delete(url, params);
    }

    /**
     * Get the business process documents
     */
    @Test
    public void getDocumentsTest() {
        String partnerID = TestObjectFactory.getPartnerID();
        String type = TestObjectFactory.getDocumentType();
        String source = TestObjectFactory.getDocumentSource();
        String status = TestObjectFactory.getDocumentStatus();
        String url = "http://localhost:" + port +"/document/{partnerID}/{type}/{source}/{status}";

        Map<String, String> params = new HashMap<String, String>();
        params.put("partnerID", partnerID);
        params.put("type", type);
        params.put("source", source);
        params.put("status", status);

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class, params);

        logger.info(" $$$ Test response {} ", response.toString());

        assertNotNull(response);
    }

    /**
     * Get the business process documents
     */
    @Test
    public void getDocuments_0Test() {
        String partnerID = TestObjectFactory.getPartnerID();
        String type = TestObjectFactory.getDocumentType();
        String source = TestObjectFactory.getDocumentSource();
        String url = "http://localhost:" + port +"/document/{partnerID}/{type}/{source}";

        Map<String, String> params = new HashMap<String, String>();
        params.put("partnerID", partnerID);
        params.put("type", type);
        params.put("source", source);

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class, params);

        logger.info(" $$$ Test response {} ", response.toString());

        assertNotNull(response);
    }

    /**
     * Get the business process documents
     */
    @Test
    public void getDocuments_1Test() {
        String partnerID = TestObjectFactory.getPartnerID();
        String type = TestObjectFactory.getDocumentType();
        String url = "http://localhost:" + port +"/document/{partnerID}/{type}";

        Map<String, String> params = new HashMap<String, String>();
        params.put("partnerID", partnerID);
        params.put("type", type);

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class, params);

        logger.info(" $$$ Test response {} ", response.toString());

        assertNotNull(response);
    }

    /**
     * Update a business process document
     */
    @Test
    public void updateDocumentMetadataTest() {
        ProcessDocumentMetadata body = TestObjectFactory.updateBusinessDocumentMetadata();
        restTemplate.put("http://localhost:" + port +"/document", body);
    }
}
