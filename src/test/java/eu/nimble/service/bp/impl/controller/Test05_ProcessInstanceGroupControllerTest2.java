package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import org.junit.Assert;
import org.junit.FixMethodOrder;
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

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test05_ProcessInstanceGroupControllerTest2 {

    @Autowired
    private MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper();
    private final int test1_expectedValue = 2;
    private final boolean test2_expectedValue = true;

    @Test
    public void test1_addProcessInstanceToGroup() throws Exception {
        MockHttpServletRequestBuilder request = post("/group/" + Test04_ProcessInstanceGroupControllerTest.processInstanceGroupId1 + "/process-instance")
                .param("processInstanceID", Test01_StartControllerTest.processInstanceIdOrder3);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstanceGroup processInstanceGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroup.class);

        Assert.assertSame(test1_expectedValue, processInstanceGroup.getProcessInstanceIDs().size());
    }

    @Test
    public void test2_archiveGroup() throws Exception {
        MockHttpServletRequestBuilder request = post("/group/" + Test04_ProcessInstanceGroupControllerTest.processInstanceGroupIIR1 + "/archive");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstanceGroup processInstanceGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroup.class);

        Assert.assertEquals(test2_expectedValue, processInstanceGroup.getArchived());
    }
}
