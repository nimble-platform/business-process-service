package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class Test11_DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
    private final String documentMetadataJSON = "/controller/documentMetaDataJSON1.txt";
    private final String documentMetadataJSON2 = "/controller/documentMetaDataJSON2.txt";
    private final String documentMetadataJSON3 = "/controller/documentMetaDataJSON3.txt";
    private final String expectedType = "SUCCESS";

    @Test
    public void test1_addDocumentMetadata() throws Exception {
        String documentMetadata = IOUtils.toString(ProcessDocumentMetadata.class.getResourceAsStream(documentMetadataJSON));
        MockHttpServletRequestBuilder request = post("/document")
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(documentMetadata);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response.getType());
    }

    @Test
    public void test2_addDocumentMetadata() throws Exception {
        String documentMetadata = IOUtils.toString(ProcessDocumentMetadata.class.getResourceAsStream(documentMetadataJSON2));
        MockHttpServletRequestBuilder request = post("/document")
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(documentMetadata);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response.getType());
    }

    @Test
    public void test3_addDocumentMetadata() throws Exception {
        String documentMetadata = IOUtils.toString(ProcessDocumentMetadata.class.getResourceAsStream(documentMetadataJSON3));
        MockHttpServletRequestBuilder request = post("/document")
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(documentMetadata);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response.getType());
    }
}
