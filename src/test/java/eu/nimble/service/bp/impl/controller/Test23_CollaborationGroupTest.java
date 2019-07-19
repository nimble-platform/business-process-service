package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.swagger.model.CollaborationGroup;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test23_CollaborationGroupTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private final String productName = "QProduct";
    private final String serviceName = "QService";
    private final String partyID = "706";
    private final String collaborationRoleSeller = "SELLER";
    private final String collaborationRoleBuyer = "BUYER";
    private int test1_expectedCollaborationGroupNumber = 1;
    private int test1_expectedProcessInstanceGroupNumber = 2;


    public static final String groupName = "new collaboration group";
    public static String idOfTheLastProcessInstance;
    public static String collaborationGroupID;
    public static String collaborationGroupToBeDeletedId;
    public static String cancelledProcessInstanceId;

    @Test
    public void test1_updateCollaborationGroupName() throws Exception {
        // get the collaboration group
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("collaborationRole", collaborationRoleBuyer)
                .param("relatedProducts",serviceName)
                .param("partyId", partyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        Assert.assertSame(test1_expectedCollaborationGroupNumber, collaborationGroupResponse.getSize());
        Assert.assertSame(test1_expectedProcessInstanceGroupNumber,collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().size());

        collaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getID();
        int sizeOfProcessInstances = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(1).getProcessInstanceIDs().size();
        idOfTheLastProcessInstance = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(1).getProcessInstanceIDs().get(sizeOfProcessInstances - 1);

        // update collaboration group name
        request = patch("/collaboration-groups/"+collaborationGroupID)
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("groupName",groupName);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

    }

    @Test
    public void test2_archiveCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = post("/collaboration-groups/"+collaborationGroupID+"/archive")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test3_cancelCollaboration() throws Exception{
        // get the collaboration group
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("collaborationRole", collaborationRoleSeller)
                .param("relatedProducts",productName)
                .param("partyId", partyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        Assert.assertSame(1, collaborationGroupResponse.getSize());
        collaborationGroupToBeDeletedId = collaborationGroupResponse.getCollaborationGroups().get(0).getID();

        // get the id of process instance to be cancelled
        cancelledProcessInstanceId = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(0);

        String groupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getID();
        // cancel collaboration group
        request = post("/process-instance-groups/"+ groupID +"/cancel")
                .header("Authorization", TestConfig.responderPersonId);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // try to cancel collaboration group again
        request = post("/process-instance-groups/"+ groupID +"/cancel")
                .header("Authorization", TestConfig.responderPersonId);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    }

    // retrieve the collaboration group for the last process instance, then compare its id with the identifier
    // of the collaboration group owning this process
    @Test
    public void test4_getAssociatedCollaborationGroup() throws Exception{
        // get the collaboration group
        MockHttpServletRequestBuilder request = get("/processInstance/"+idOfTheLastProcessInstance+"/collaboration-group")
                .header("Authorization", TestConfig.responderPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroup collaborationGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroup.class);
        Assert.assertEquals(collaborationGroupID,collaborationGroup.getID());
    }

    // try to archive a non-existing collaboration group
    @Test
    public void test5_archiveCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = post("/collaboration-groups/99999999999/archive")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    // try to restore a non-existing collaboration group
    @Test
    public void test6_restoreCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = post("/collaboration-groups/99999999999/restore")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }
}
