package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import org.junit.FixMethodOrder;
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

import static org.junit.Assert.assertNotNull;

/**
 * API tests for DefaultApi
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StartContinueControllerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    static String processInstanceID = "";

     /**
     * Start an instance of a business process
     */
    @Test
    public void startBusinessProcessInstanceTest() {
        ProcessInstanceInputMessage body = TestObjectFactory.createStartProcessInstanceInputMessage();
        String url = "http://localhost:" + port +"/start";

        ResponseEntity<ProcessInstance> response = restTemplate.postForEntity(url, body, ProcessInstance.class);

        logger.info(" $$$ Test response {} ", response.toString());

        processInstanceID = response.getBody().getProcessInstanceID();

        assertNotNull(response);
    }

    /**
     * Send input to a waiting process instance (because of a human task)
     */
    @Test
    public void t_continueBusinessProcessInstanceTest() {
        ProcessInstanceInputMessage body = TestObjectFactory.createContinueProcessInstanceInputMessage();

        body.setProcessInstanceID(processInstanceID);

        String url = "http://localhost:" + port +"/continue";

        ResponseEntity<ProcessInstance> response = restTemplate.postForEntity(url, body, ProcessInstance.class);

        logger.info(" $$$ Test response {} ", response.toString());

        assertNotNull(response);
    }
}
