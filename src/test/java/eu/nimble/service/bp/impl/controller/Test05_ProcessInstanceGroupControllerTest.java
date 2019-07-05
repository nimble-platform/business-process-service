package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.swagger.model.CollaborationGroup;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import eu.nimble.utility.JsonSerializationUtility;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test05_ProcessInstanceGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final String partyId = "706";
    public static String processInstanceGroupId1;
    public static String processInstanceGroupId2;
    public static String processInstanceGroupIIR1;
    private final int test1_expectedValue = 5;
    private final int test2_expectedValue = 1;

    @Test
    public void test1_getCollaborationGroups() throws Exception {
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("partyId", partyId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);
        Assert.assertSame(test1_expectedValue, collaborationGroupResponse.getSize());
        for (CollaborationGroup cg : collaborationGroupResponse.getCollaborationGroups()) {
            if (cg.getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(0).contentEquals(Test01_StartControllerTest.processInstanceIdOrder1)) {
                processInstanceGroupId1 = cg.getAssociatedProcessInstanceGroups().get(0).getID();
            } else if (cg.getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(0).contentEquals(Test01_StartControllerTest.processInstanceIdIIR1)) {
                processInstanceGroupIIR1 = cg.getAssociatedProcessInstanceGroups().get(0).getID();
            } else if (cg.getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(0).contentEquals(Test01_StartControllerTest.processInstanceIdOrder2)) {
                processInstanceGroupId2 = cg.getAssociatedProcessInstanceGroups().get(0).getID();
            }
        }
    }

    @Test
    public void test2_getProcessInstanceGroup() throws Exception {
        MockHttpServletRequestBuilder request = get("/process-instance-groups/" + Test05_ProcessInstanceGroupControllerTest.processInstanceGroupIIR1)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
        ProcessInstanceGroup processInstanceGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroup.class);

        Assert.assertSame(test2_expectedValue, processInstanceGroup.getProcessInstanceIDs().size());
        Assert.assertEquals(Test01_StartControllerTest.processInstanceIdIIR1, processInstanceGroup.getProcessInstanceIDs().get(0));
    }

}
