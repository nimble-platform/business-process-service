package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder
@RunWith(SpringJUnit4ClassRunner.class)
public class Test14_ProcessInstanceGroupControllerTest4 {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    private final String test1_expectedValue = "true";
    private final int test2_expectedSize = 1;

    @Test
    public void test1_deleteProcessInstanceGroup() throws Exception {
        MockHttpServletRequestBuilder request = delete("/group/" + Test04_ProcessInstanceGroupControllerTest.processInstanceGroupIIR1);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        String body = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), String.class);

        Assert.assertEquals(test1_expectedValue, body);
    }

    @Test
    public void test2_deleteProcessInstanceFromGroup() throws Exception {
        MockHttpServletRequestBuilder request = delete("/group/" + Test04_ProcessInstanceGroupControllerTest.processInstanceGroupId1 + "/process-instance")
                .param("processInstanceID", Test01_StartControllerTest.processInstanceIdOrder3);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstanceGroup processInstanceGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroup.class);

        Assert.assertSame(test2_expectedSize, processInstanceGroup.getProcessInstanceIDs().size());
    }
}
