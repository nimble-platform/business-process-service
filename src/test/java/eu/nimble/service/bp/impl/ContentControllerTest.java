package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.swagger.model.Process;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
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
public class ContentControllerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Add a new business process
     */
    @Test
    public void addProcessDefinitionTest() {
        Process body = TestObjectFactory.createProcess();
        String url = "http://localhost:" + port +"/content";

        ResponseEntity<ModelApiResponse> response = restTemplate.postForEntity(url, body, ModelApiResponse.class);

        logger.info(" $$$ Test response {} ", response.toString());

        assertEquals(200, response.getBody().getCode().intValue());
    }

    /**
     * Get the business process definitions
     */
    @Test
    public void getProcessDefinitionTest() {
        String processID = TestObjectFactory.getProcessID();
        String url = "http://localhost:" + port +"/content/{processID}";

        Map<String, String> params = new HashMap<String, String>();
        params.put("processID", processID);

        ResponseEntity<Process> response = restTemplate.getForEntity(url, Process.class, params);

        logger.info(" $$$ Test response {} ", response.toString());

        assertNotNull(response);
    }

    /**
     * Get the business process definitions
     */
    @Test
    public void getProcessDefinitionsTest() {
        ResponseEntity<List> response = restTemplate.getForEntity("http://localhost:" + port +"/content", List.class);

        logger.info(" $$$ Test response {} ", response.toString());
        assertNotNull(response);
    }

    /**
     * Update a business process
     */
    @Test
    public void updateProcessDefinitionTest() {
        Process body = TestObjectFactory.updateProcess();

        restTemplate.put("http://localhost:" + port +"/content", body);
    }

    /**
     * Deletes a business process definition
     */
    @Test
    public void z_deleteProcessDefinitionTest() {
        String processID = TestObjectFactory.getProcessID();
        String url = "http://localhost:" + port +"/content/{processID}";

        Map<String, String> params = new HashMap<String, String>();
        params.put("processID", processID);

        restTemplate.delete(url, params);
    }
}
