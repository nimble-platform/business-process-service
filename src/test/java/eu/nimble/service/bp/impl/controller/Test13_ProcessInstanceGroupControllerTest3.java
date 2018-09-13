package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder
@RunWith(SpringJUnit4ClassRunner.class)
public class Test13_ProcessInstanceGroupControllerTest3 {

    @Autowired
    private MockMvc mockMvc;

    private final int test1_expectedSize = 2;
    private final boolean test2_expectedValue = false;
    private final String test3_expectedValue = "true";

    private final String processInstanceGroupJSON = "/controller/processInstanceGroupJSON.txt";
    private ObjectMapper objectMapper = new ObjectMapper().
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
            configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

    @Test
    public void test1_getProcessInstancesOfGroup() throws Exception {
        MockHttpServletRequestBuilder request = get("/group/" + Test04_ProcessInstanceGroupControllerTest.processInstanceGroupId1 + "/process-instance");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<ProcessInstance> processInstances = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessInstance>>() {
        });

        Assert.assertSame(test1_expectedSize, processInstances.size());
    }

    @Test
    public void test2_restoreGroup() throws Exception {
        MockHttpServletRequestBuilder request = post("/group/" + Test04_ProcessInstanceGroupControllerTest.processInstanceGroupIIR1 + "/restore");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstanceGroup processInstanceGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroup.class);

        Assert.assertEquals(test2_expectedValue, processInstanceGroup.getArchived());
    }

    @Test
    public void test3_saveProcessInstanceGroup() throws Exception {
        String processInstanceGroup = IOUtils.toString(ProcessInstanceGroup.class.getResourceAsStream(processInstanceGroupJSON));

        MockHttpServletRequestBuilder request = post("/group")
                .contentType(MediaType.APPLICATION_JSON)
                .content(processInstanceGroup);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        String body = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), String.class);

        Assert.assertEquals(test3_expectedValue, body);
    }
}
