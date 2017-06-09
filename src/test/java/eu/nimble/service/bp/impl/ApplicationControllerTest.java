package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessApplicationConfigurations;
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
public class ApplicationControllerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Add a new partner business process application preference
     */
    @Test
    public void addBusinessProcessPartnerApplicationPreferenceTest() {
        ProcessApplicationConfigurations body = TestObjectFactory.createProcessApplicationConfigurations();

        String url = "http://localhost:" + port +"/application";

        ResponseEntity<ModelApiResponse> response = restTemplate.postForEntity(url, body, ModelApiResponse.class);

        logger.info(" $$$ Test response {} ", response.toString());

        assertEquals(200, response.getBody().getCode().intValue());
    }

    /**
     * Deletes the business process application preference of a partner
     */
    @Test
    public void z_deleteBusinessProcessPartnerApplicationPreferenceTest() {
        String partnerID = TestObjectFactory.getPartnerID();
        String processID = TestObjectFactory.getProcessID();
        String url = "http://localhost:" + port +"/application/{partnerID}";

        Map<String, String> params = new HashMap<String, String>();
        params.put("partnerID", partnerID);
        params.put("processID", processID);

        restTemplate.delete(url, params);
    }

    /**
     * Get the business process application preference of a partner
     */
    @Test
    public void getBusinessProcessPartnerApplicationPreferenceTest() {
        String partnerID = TestObjectFactory.getPartnerID();
        String url = "http://localhost:" + port +"/application/{partnerID}";

        Map<String, String> params = new HashMap<String, String>();
        params.put("partnerID", partnerID);

        ResponseEntity<List> response = restTemplate.getForEntity(url, List.class, params);

        logger.info(" $$$ Test response {} ", response.toString());

        assertNotNull(response);
    }

    /**
     * Update the business process application preference of a partner
     */
    @Test
    public void updateBusinessProcessPartnerApplicationPreferenceTest() {
        ProcessApplicationConfigurations body = TestObjectFactory.updateProcessApplicationConfigurations();
        restTemplate.put("http://localhost:" + port +"/application", body);
    }
}
