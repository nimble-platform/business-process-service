package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class Test08_DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
    private final String documentMetadataJSON = "/controller/documentMetaDataJSON1.txt";
    private final String documentMetadataJSON2 = "/controller/documentMetaDataJSON2.txt";
    private final String documentMetadataJSON3 = "/controller/documentMetaDataJSON3.txt";
    private final String expectedType = "SUCCESS";
    private final int expectedSize = 3;
    private final String partnerID = "706";
    private final String partnerID2 = "1339";
    private final String type = "ORDER";

    private final int numberOfDocuments1 = 5;
    private final int numberOfDocuments2 = 1;
    private final int numberOfDocuments3 = 1;
    private final List<String> listOfProducts = Arrays.asList("Product1", "Product2", "Product3");
    private final String source = "SENT";
    private final String status = "WAITINGRESPONSE";

    private static String documentId;

    @Test
    public void test1_addDocumentMetadata() throws Exception {
        String documentMetadata = IOUtils.toString(ProcessDocumentMetadata.class.getResourceAsStream(documentMetadataJSON));
        MockHttpServletRequestBuilder request = post("/document")
                .header("Authorization", TestConfig.initiatorPersonId)
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
                .header("Authorization", TestConfig.initiatorPersonId)
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
                .header("Authorization", TestConfig.initiatorPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(documentMetadata);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response.getType());
    }

    @Test
    public void test4_getDocuments() throws Exception {
        MockHttpServletRequestBuilder request = get("/document/" + partnerID + "/" + type)
                .header("Authorization", TestConfig.initiatorPersonId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<ProcessDocumentMetadata> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessDocumentMetadata>>() {
        });

        Assert.assertSame(numberOfDocuments1, response.size());
    }

    @Test
    public void test5_getDocuments() throws Exception {
        MockHttpServletRequestBuilder request = get("/document/" + partnerID + "/" + type + "/" + source)
                .header("Authorization", TestConfig.initiatorPersonId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<ProcessDocumentMetadata> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessDocumentMetadata>>() {
        });

        Assert.assertSame(numberOfDocuments2, response.size());
    }

    @Test
    public void test6_getDocuments() throws Exception {
        MockHttpServletRequestBuilder request = get("/document/" + partnerID2 + "/" + type + "/" + source + "/" + status)
                .header("Authorization", TestConfig.initiatorPersonId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<ProcessDocumentMetadata> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessDocumentMetadata>>() {
        });

        Assert.assertSame(numberOfDocuments3, response.size());
    }

    @Test
    public void test7_updateDocumentMetadata() throws Exception {
        // get document
        MockHttpServletRequestBuilder request = get("/document/" + partnerID + "/" + type)
                .header("Authorization", TestConfig.initiatorPersonId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<ProcessDocumentMetadata> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessDocumentMetadata>>() {
        });

        ProcessDocumentMetadata processDocumentMetadata = response.get(0);
        processDocumentMetadata.setRelatedProducts(listOfProducts);

        documentId = response.get(0).getDocumentID();

        // update the document
        request = put("/document")
                .header("Authorization", TestConfig.initiatorPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(processDocumentMetadata));
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ModelApiResponse response1 = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response1.getType());

    }

    @Test
    public void test8_deleteDocument() throws Exception {
        // get the document
        MockHttpServletRequestBuilder request = get("/document/" + partnerID + "/" + type)
                .header("Authorization", TestConfig.initiatorPersonId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<ProcessDocumentMetadata> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessDocumentMetadata>>() {
        });

        // find the correct ProcessDocumentMetadata
        for(ProcessDocumentMetadata pdm : response){
            if(pdm.getDocumentID().equals(documentId)){
                Assert.assertSame(expectedSize, pdm.getRelatedProducts().size());
            }
        }

        // delete the document
        request = delete("/document/" + response.get(0).getDocumentID())
                .header("Authorization", TestConfig.initiatorPersonId);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ModelApiResponse response1 = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response1.getType());
    }
}
