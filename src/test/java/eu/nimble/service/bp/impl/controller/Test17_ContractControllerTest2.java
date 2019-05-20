package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ContractType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentClauseType;
import eu.nimble.utility.JsonSerializationUtility;
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

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test17_ContractControllerTest2 {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

    private final int test1_expectedSize = 1;
    private final int test2_expectedSize = 2;
    private final int test3_expectedSize = 2;
    private final int test4_expectedSize = 2;
    private final String expectedType = "DOCUMENT";

    @Test
    public void test1_getClauseDetails() throws Exception {
        MockHttpServletRequestBuilder request = get("/documents/" + Test01_StartControllerTest.orderId1 + "/clauses")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("clauseType", "DOCUMENT");

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<ClauseType> clauseTypes = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ClauseType>>() {
        });

        Assert.assertSame(test1_expectedSize, clauseTypes.size());
    }

    @Test
    public void test2_getClausesOfContract() throws Exception {
        MockHttpServletRequestBuilder request = get("/contracts/" + Test16_ContractControllerTest.contractId + "/clauses")
                .header("Authorization", TestConfig.initiatorPersonId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<DocumentClauseType> clauseTypes = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<DocumentClauseType>>() {
        });

        Assert.assertSame(test2_expectedSize, clauseTypes.size());
    }

    @Test
    public void test3_constructContractForProcessInstances() throws Exception {
        MockHttpServletRequestBuilder request = get("/contracts")
                .param("processInstanceId", Test01_StartControllerTest.processInstanceIdOrder1)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ContractType contract = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ContractType.class);

        Assert.assertSame(test3_expectedSize, contract.getClause().size());
    }

//    @Test
//    public void test4_getClauseDetailsAndUpdate() throws Exception {
//        // getClauseDetails
//        MockHttpServletRequestBuilder request = get("/clauses/" + Test16_ContractControllerTest.clauseId)
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
//        request = put("/clauses/" + Test16_ContractControllerTest.clauseId)
//                .header("Authorization", TestConfig.initiatorPersonId)
//                .content(objectMapper.writeValueAsString(clause));
//        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
//
//        clause = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ClauseType.class);
//
//        Assert.assertSame(test4_expectedSize, clause.getNote().size());
//    }


}
