package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ContractType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DataMonitoringClauseType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentClauseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test09_ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
    private final String dataMonitoringJSON = "/controller/dataMonitoringJSON.txt";
    private final int test1_expectedResult = 1;
    private final int test2_expectedSize = 2;
    private final int test3_expectedSize = 1;
    private final int test4_expectedSize = 2;
    private final int test5_expectedSize = 2;
    private final int test6_expectedSize = 2;
    private final String expectedType = "DOCUMENT";
    private final int test7_expectedSize = 1;

    private static String contractId;
    private static String clauseId;

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

    @Test
    public void test3_getClauseDetails() throws Exception {
        MockHttpServletRequestBuilder request = get("/documents/" + Test01_StartControllerTest.orderId1 + "/clauses")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("clauseType", "DOCUMENT");

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<ClauseType> clauseTypes = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ClauseType>>() {
        });

        Assert.assertSame(test3_expectedSize, clauseTypes.size());
    }

    @Test
    public void test4_getClausesOfContract() throws Exception {
        MockHttpServletRequestBuilder request = get("/contracts/" + Test09_ContractControllerTest.contractId + "/clauses")
                .header("Authorization", TestConfig.initiatorPersonId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<DocumentClauseType> clauseTypes = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<DocumentClauseType>>() {
        });

        Assert.assertSame(test4_expectedSize, clauseTypes.size());
    }

    @Test
    public void test5_constructContractForProcessInstances() throws Exception {
        MockHttpServletRequestBuilder request = get("/contracts")
                .param("processInstanceId", Test01_StartControllerTest.processInstanceIdOrder1)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ContractType contract = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ContractType.class);

        Assert.assertSame(test5_expectedSize, contract.getClause().size());
    }

//    @Test
//    public void test6_getClauseDetailsAndUpdate() throws Exception {
//        // getClauseDetails
//        MockHttpServletRequestBuilder request = get("/clauses/" + Test09_ContractControllerTest.clauseId)
//                .header("Authorization", TestConfig.initiatorPersonId);
//        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
//
//        ClauseType clause = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ClauseType.class);
//
//        Assert.assertEquals(expectedType, clause.getType());
//
//        List<String> notes = new ArrayList<>();
//        notes.add("This is the note");
//        notes.add("This is the second note");
//        clause.setNote(notes);
//
//        //updateClause
//        request = put("/clauses/" + Test09_ContractControllerTest.clauseId)
//                .header("Authorization", TestConfig.initiatorPersonId)
//                .content(objectMapper.writeValueAsString(clause));
//        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
//
//        clause = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ClauseType.class);
//
//        Assert.assertSame(test6_expectedSize, clause.getNote().size());
//    }

    //    @Test
//    public void test7_deleteClauseFromContract() throws Exception {
//        MockHttpServletRequestBuilder request = delete("/contracts/" + Test09_ContractControllerTest.contractId + "/clauses/" + Test09_ContractControllerTest.clauseId)
//                .header("Authorization", TestConfig.initiatorPersonId);
//
//        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
//        ContractType contract = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ContractType.class);
//
//        Assert.assertEquals(test7_expectedSize, contract.getClause().size());
//    }
}
