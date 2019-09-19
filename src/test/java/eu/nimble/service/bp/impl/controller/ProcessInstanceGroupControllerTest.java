package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.model.hyperjaxb.GroupStatus;
import eu.nimble.service.bp.swagger.model.CollaborationGroup;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class ProcessInstanceGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

    private final String test3_expectedValue = "true";
    private final int test5_expectedSize = 2;
    private final String partyId = "706";
    private static String processInstanceGroupId1;
    private static String processInstanceGroupIIR1;
    private final int test1_expectedValue = 8;
    private final int test2_expectedValue = 1;

    /**
     * Test scenario:
     * - Get collaboration groups for some of the processes instantiated in {@link StartControllerTest}
     * - Retrieve process instance group associated one of the processes instantiated in {@link StartControllerTest}
     * - Retrieve process instances belonging to process instance group created in {@link StartControllerTest}
     * - Delete process instance group of one of the processes instantiated in {@link StartControllerTest}
     * - Delete a non-existing process instance group (404 is expected)
     * - Retrieve a process instance group (the one created for the transport service order) and delete it
     * - Delete also the associated process instance group
     * - Filter the process instance groups based for a specific party and its role
     * - Check whether a collaboration is finished,
     * - Check the status of a Process Instance Group representing a completed collaboration
     */

    @Test
    public void test01_getCollaborationGroups() throws Exception {
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("partyId", partyId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);
        Assert.assertSame(test1_expectedValue, collaborationGroupResponse.getSize());
        for (CollaborationGroup cg : collaborationGroupResponse.getCollaborationGroups()) {
            if (cg.getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(0).contentEquals(StartControllerTest.processInstanceIdOrder1)) {
                processInstanceGroupId1 = cg.getAssociatedProcessInstanceGroups().get(0).getID();
            } else if (cg.getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(0).contentEquals(StartControllerTest.processInstanceIdIIR1)) {
                processInstanceGroupIIR1 = cg.getAssociatedProcessInstanceGroups().get(0).getID();
            }
        }
    }

    @Test
    public void test02_getProcessInstanceGroup() throws Exception {
        MockHttpServletRequestBuilder request = get("/process-instance-groups/" + ProcessInstanceGroupControllerTest.processInstanceGroupIIR1)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstanceGroup processInstanceGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroup.class);

        Assert.assertSame(test2_expectedValue, processInstanceGroup.getProcessInstanceIDs().size());
        Assert.assertEquals(StartControllerTest.processInstanceIdIIR1, processInstanceGroup.getProcessInstanceIDs().get(0));
    }

    @Test
    public void test03_getProcessInstancesIncludedInTheGroup() throws Exception {
        MockHttpServletRequestBuilder request = get("/process-instance-groups/" + ProcessInstanceGroupControllerTest.processInstanceGroupIIR1 + "/process-instances")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<ProcessInstance> processInstances = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<ProcessInstance>>() {});

        Assert.assertSame(test2_expectedValue, processInstances.size());
        Assert.assertEquals(StartControllerTest.processInstanceIdIIR1, processInstances.get(0).getProcessInstanceID());
    }

    @Test
    public void test04_deleteProcessInstanceGroup() throws Exception {
        MockHttpServletRequestBuilder request = delete("/process-instance-groups/" + ProcessInstanceGroupControllerTest.processInstanceGroupId1)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        String body = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), String.class);

        Assert.assertEquals(test3_expectedValue, body);
    }

    // try to delete a non-existing process instance group
    @Test
    public void test05_deleteNonExistingProcessInstanceGroup() throws Exception {
        MockHttpServletRequestBuilder request = delete("/process-instance-groups/999999")
                .header("Authorization", TestConfig.initiatorPersonId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    @Test
    public void test06_getProcessInstanceGroupFilters() throws Exception {
        MockHttpServletRequestBuilder request = get("/process-instance-groups/filters")
                .header("Authorization", TestConfig.responderPersonId)
                .param("collaborationRole", "SELLER")
                .param("partyId", "706");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstanceGroupFilter processInstanceGroupFilter = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroupFilter.class);

        Assert.assertSame(test5_expectedSize, processInstanceGroupFilter.getTradingPartnerIDs().size());

    }

    @Test
    public void test07_checkCollaborationFinished() throws Exception{
        MockHttpServletRequestBuilder request = get("/process-instance-groups/"+ BusinessProcessWorkflowTests.sellerProcessInstanceGroupID+"/finished")
                .header("Authorization", TestConfig.responderPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        Assert.assertEquals("true",mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void test08_checkProcessInstanceGroupStatus() throws Exception {
        MockHttpServletRequestBuilder request = get("/process-instance-groups/" + BusinessProcessExecutionTest.buyerProcessInstanceGroupID)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstanceGroup processInstanceGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroup.class);

        Assert.assertSame(GroupStatus.COMPLETED.value(), processInstanceGroup.getStatus().toString());
    }
}
