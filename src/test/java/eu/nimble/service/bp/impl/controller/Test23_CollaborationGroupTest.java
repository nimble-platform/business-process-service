package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.CollaborationGroupResponse;
import org.junit.Assert;
import org.junit.FixMethodOrder;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test23_CollaborationGroupTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;
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

    @Test
    public void test1_updateCollaborationGroupName() throws Exception {
        // get the collaboration group
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"))
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
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"))
                .param("groupName",groupName);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

    }

    @Test
    public void test2_archiveCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = post("/collaboration-groups/"+collaborationGroupID+"/archive")
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"));
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test3_cancelCollaboration() throws Exception{
        // get the collaboration group
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", environment.getProperty("nimble.test-initiator-token"))
                .param("collaborationRole", collaborationRoleSeller)
                .param("relatedProducts",productName)
                .param("partyId", partyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        Assert.assertSame(1, collaborationGroupResponse.getSize());
        collaborationGroupToBeDeletedId = collaborationGroupResponse.getCollaborationGroups().get(0).getID();

        String groupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getID();
        // cancel collaboration group
        request = post("/process-instance-groups/"+ groupID +"/cancel")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"));
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }
}
