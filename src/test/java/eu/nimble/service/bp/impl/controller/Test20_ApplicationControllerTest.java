package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.utility.JsonSerializationUtility;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.Test;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
@Ignore
public class Test20_ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;
    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

    private final String partnerId = "706";
    private final String partnerId2 = "874";
    private final String processId = "87490";
    private final String roleType = "BUYER";
    private final String transactionId = "72252";

    private final int test1_expectedSize = 1;
    private final String expectedType = "SUCCESS";

    @Test
    public void test1_getProcessConfiguration() throws Exception {
        MockHttpServletRequestBuilder request = get("/application/" + partnerId)
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"));
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<ProcessConfiguration> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessConfiguration>>() {
        });

        Assert.assertSame(test1_expectedSize, response.size());
    }

    @Test
    public void test2_getProcessConfigurationByProcessID() throws Exception {
        MockHttpServletRequestBuilder request = get("/application/" + partnerId2 + "/" + processId + "/" + roleType)
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"));
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ProcessConfiguration response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessConfiguration.class);

        Assert.assertEquals(transactionId, response.getTransactionConfigurations().get(0).getTransactionID());
    }

    @Test
    public void test3_updateProcessConfiguration() throws Exception {
        // get process configuration
        MockHttpServletRequestBuilder request = get("/application/" + partnerId2)
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"));
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<ProcessConfiguration> processConfigurations = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessConfiguration>>() {
        });
        ProcessConfiguration processConfiguration = processConfigurations.get(0);

        processConfiguration.setRoleType(ProcessConfiguration.RoleTypeEnum.LOGISTICSPROVIDER);
        // update process configuration
        request = put("/application")
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(processConfiguration));
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response.getType());
    }
}
