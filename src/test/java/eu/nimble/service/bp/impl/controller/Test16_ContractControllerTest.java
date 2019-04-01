package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DataMonitoringClauseType;
import eu.nimble.service.model.ubl.order.OrderType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test16_ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
    private final String dataMonitoringJSON = "/controller/dataMonitoringJSON.txt";
    private final int test1_expectedResult = 1;
    private final int test2_expectedSize = 2;

    public static String contractId;
    public static String clauseId;

    @Test
    public void test1_addDocumentClauseToContract() throws Exception {
        MockHttpServletRequestBuilder request = patch("/documents/" + Test01_StartControllerTest.orderId1 + "/contract/clause/document")
                .param("clauseType", "ITEM_DETAILS")
                .param("clauseDocumentId", Test01_StartControllerTest.iirId1)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        OrderType order = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), OrderType.class);

        Assert.assertEquals(test1_expectedResult, order.getContract().get(0).getClause().size());

        contractId = order.getContract().get(0).getID();
        clauseId = order.getContract().get(0).getClause().get(0).getID();

    }

    @Test
    public void test2_addDataMonitoringClauseToContract() throws Exception {
        String dataMonitoring = IOUtils.toString(DataMonitoringClauseType.class.getResourceAsStream(dataMonitoringJSON));


        MockHttpServletRequestBuilder request = patch("/documents/" + Test01_StartControllerTest.orderId1 + "/contract/clause/data-monitoring")
                .contentType(MediaType.APPLICATION_JSON)
                .content(dataMonitoring)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        OrderType order = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), OrderType.class);

        Assert.assertSame(test2_expectedSize, order.getContract().get(0).getClause().size());

    }
}
