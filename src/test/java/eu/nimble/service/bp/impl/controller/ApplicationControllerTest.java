package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by dogukan on 16.07.2018.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class ApplicationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

    private final String processConfigJSON = "/controller/processConfigurationJSON1.txt";
    private final String processConfigJSON2 = "/controller/processConfigurationJSON2.txt";

    private final String expectedType = "SUCCESS";
    private final String test3_partnerId = "706";
    private final String test5_partnerId = "874";
    private final String test4_processId = "87490";
    private final String test4_roleType = "BUYER";
    private final String test4_transactionId = "72252";

    private final int test1_expectedSize = 1;
    private final String test5_expectedType = "SUCCESS";
    private final String test6_partnerId = "874";
    private final String test6_processId = "87490";
    private final String test6_roleType = "BUYER";

    private final String test6_expectedType = "SUCCESS";

    @Test
    public void test1_addProcessConfiguration() throws Exception {
        String processConfig = IOUtils.toString(ProcessConfiguration.class.getResourceAsStream(processConfigJSON));
        MockHttpServletRequestBuilder request = post("/application")
                .header("Authorization", TestConfig.initiatorPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(processConfig);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response.getType());
    }

    @Test
    public void test2_addProcessConfiguration() throws Exception {
        String processConfig = IOUtils.toString(ProcessConfiguration.class.getResourceAsStream(processConfigJSON2));
        MockHttpServletRequestBuilder request = post("/application")
                .header("Authorization", TestConfig.initiatorPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(processConfig);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response.getType());
    }

    @Test
    public void test3_getProcessConfiguration() throws Exception {
        MockHttpServletRequestBuilder request = get("/application/" + test3_partnerId)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<ProcessConfiguration> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessConfiguration>>() {
        });

        Assert.assertSame(test1_expectedSize, response.size());
    }

    @Test
    public void test4_getProcessConfigurationByProcessID() throws Exception {
        MockHttpServletRequestBuilder request = get("/application/" + test5_partnerId + "/" + test4_processId + "/" + test4_roleType)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ProcessConfiguration response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessConfiguration.class);

        Assert.assertEquals(test4_transactionId, response.getTransactionConfigurations().get(0).getTransactionID());
    }

    @Test
    public void test5_updateProcessConfiguration() throws Exception {
        // get process configuration
        MockHttpServletRequestBuilder request = get("/application/" + test5_partnerId)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<ProcessConfiguration> processConfigurations = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessConfiguration>>() {
        });
        ProcessConfiguration processConfiguration = processConfigurations.get(0);

        processConfiguration.setRoleType(ProcessConfiguration.RoleTypeEnum.LOGISTICSPROVIDER);
        // update process configuration
        request = put("/application")
                .header("Authorization", TestConfig.initiatorPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(processConfiguration));
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(test5_expectedType, response.getType());
    }

    @Test
    public void test6_deleteProcessConfiguration() throws Exception {
        MockHttpServletRequestBuilder request = delete("/application/" + test6_partnerId + "/" + test6_processId + "/" + test6_roleType)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(test6_expectedType, response.getType());
    }
}
