package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.Process;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class BusinessProcessDefinitionTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

    private final String processId = "Transport_Execution_Plan";
    private final String processName = "Transport Execution Plan";
    private final int test1_expectedValue = 6;
    public static Process process;
    private final String expectedResult = "SUCCESS";
    private final String processDefinitionJSON = "/controller/contentJSON.txt";

    @Test
    public void test1_getProcessDefinitions() throws Exception {
        MockHttpServletRequestBuilder request = get("/content")
                .header("Authorization", TestConfig.initiatorPersonId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<Process> processes = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<Process>>() {
        });
        Assert.assertSame(test1_expectedValue, processes.size());
    }

    @Test
    public void test2_getProcessDefinition() throws Exception {
        MockHttpServletRequestBuilder request = get("/content/" + processId)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        Process process = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Process.class);
        Assert.assertEquals(processName, process.getProcessName());

        BusinessProcessDefinitionTest.process = process;
    }

    @Test
    public void test3_deleteProcessDefinition() throws Exception {
        MockHttpServletRequestBuilder request = delete("/content/" + BusinessProcessDefinitionTest.process.getProcessID())
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse apiResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedResult, apiResponse.getType());
    }

    @Test
    public void test4_addProcessDefinition() throws Exception {
        String processDefJSON = IOUtils.toString(Process.class.getResourceAsStream(processDefinitionJSON));

        MockHttpServletRequestBuilder request = post("/content")
                .header("Authorization", TestConfig.initiatorPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(processDefJSON);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse apiResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedResult, apiResponse.getType());
    }
}
