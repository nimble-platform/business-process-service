package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
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
public class DocumentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

    /**
     * Test scenario:
     * - Get unshipped order ids. Only the second order in {@link eu.nimble.service.bp.impl.StartController} matches 
     */

    @Test
    public void test1_getUnshippedOrderIds() throws Exception {
        MockHttpServletRequestBuilder request = get("/documents/unshipped-order-ids")
                .header("Authorization", TestConfig.responderPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        List<String> ids = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<String>>() {});
        Assert.assertEquals(1, ids.size());
        Assert.assertEquals("e636fed2-9326-4b08-8594-e2bbfe587e01", ids.get(0));
    }
}
