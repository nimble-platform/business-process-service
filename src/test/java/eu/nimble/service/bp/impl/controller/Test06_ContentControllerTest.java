package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.Process;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class Test06_ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

    private final String processId = "Transport_Execution_Plan";
    private final String processName = "Transport Execution Plan";
    private final int test1_expectedValue = 6;
    public static Process process;

    @Test
    public void test1_getProcessDefinitions() throws Exception {
        MockHttpServletRequestBuilder request = get("/content")
                .header("Authorization", environment.getProperty("nimble.test-initiator-person-id"));

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<Process> processes = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<Process>>() {
        });
        Assert.assertSame(test1_expectedValue, processes.size());
    }

    @Test
    public void test2_getProcessDefinition() throws Exception {
        MockHttpServletRequestBuilder request = get("/content/" + processId)
                .header("Authorization", environment.getProperty("nimble.test-initiator-person-id"));
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        Process process = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), Process.class);
        Assert.assertEquals(processName, process.getProcessName());

        Test06_ContentControllerTest.process = process;
    }
}
