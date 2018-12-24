package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by dogukan on 16.07.2018.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test22_DocumentControllerTest3 {
    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
            configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    private final String partnerID = "706";
    private final String type = "ORDER";

    private final String expectedType = "SUCCESS";
    private final int expectedSize = 3;

    @Test
    public void deleteDocument() throws Exception {
        // get the document
        MockHttpServletRequestBuilder request = get("/document/" + partnerID + "/" + type);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<ProcessDocumentMetadata> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessDocumentMetadata>>() {
        });

        // find the correct ProcessDocumentMetadata
        for(ProcessDocumentMetadata pdm : response){
            if(pdm.getDocumentID().equals(Test12_DocumentControllerTest2.documentId)){
                Assert.assertSame(expectedSize, pdm.getRelatedProducts().size());
            }
        }

        // delete the document
        request = delete("/document/" + response.get(0).getDocumentID());
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ModelApiResponse response1 = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response1.getType());
    }
}
