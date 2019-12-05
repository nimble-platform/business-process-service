package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.dashboard.ExpectedOrder;
import eu.nimble.utility.JsonSerializationUtility;
import org.junit.Assert;
import org.junit.FixMethodOrder;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

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
        MockHttpServletRequestBuilder request = get("/documents/expected-orders")
                .header("Authorization", TestConfig.responderPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        List<ExpectedOrder> expectedOrders = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ExpectedOrder>>() {});
        Assert.assertEquals(0, expectedOrders.size());
    }
}
