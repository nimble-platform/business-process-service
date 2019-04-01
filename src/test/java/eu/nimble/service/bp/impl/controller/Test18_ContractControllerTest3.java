package eu.nimble.service.bp.impl.controller;

/**
 * Created by dogukan on 16.07.2018.
 */

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ContractType;
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
import org.springframework.core.env.Environment;
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
public class Test18_ContractControllerTest3 {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

    private final int expectedSize = 1;

    @Test
    public void deleteClauseFromContract() throws Exception {
        MockHttpServletRequestBuilder request = delete("/contracts/" + Test16_ContractControllerTest.contractId + "/clauses/" + Test16_ContractControllerTest.clauseId)
                .header("Authorization", environment.getProperty("nimble.test-initiator-person-id"));

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ContractType contract = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ContractType.class);

        Assert.assertEquals(expectedSize, contract.getClause().size());


    }
}
