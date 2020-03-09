package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.model.hyperjaxb.CollaborationGroupDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.swagger.model.CollaborationGroup;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.model.ubl.order.OrderType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class CollaborationGroupTest {

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
    private int test1_expectedProcessInstanceGroupNumber = 1;
    private final String test5_productName = "QDeneme";


    public static final String groupName = "new collaboration group";
    public static String idOfTheLastProcessInstance;
    public static String collaborationGroupID;
    public static String collaborationGroupToBeDeletedId;
    public static String cancelledProcessInstanceId;
    private static String processInstanceId;

    /**
     * Test scenario:
     * All business processes are executed in the {@link BusinessProcessExecutionTest}, where two process instance groups are created
     * as a result of executing all types of processes
     *
     * - Retrieve the collaboration group of seller including process instance groups related to buyer and transport service provider
     * - Archive the collaboration group
     * - Cancel the collaboration group
     * - Retry canceling the same collaboration group (bad request expected)
     * - Retrieve collaboration group given a process instance included in the second index of the groups mentioned above
     * - Archive a non-existing collaboration group (bad request expected)
     * - Restore a non-existing collaboration group (bad request expected)
     * - Update the name of the collaboration group
     * - Restore an existing collaboration group
     * - Delete a collaboration group
     *
     * - Retrieve the order document of an order process given a process instance id such that the
     corresponding process would be included in the same process instance group with the order process
     * - Cancel a completed process
     * - Cancel an already cancelled process (bad request expected)
     */

    @Test
    public void test01_updateCollaborationGroupName() throws Exception {
        // get the collaboration group
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("federationId",TestConfig.federationId)
                .param("collaborationRole", collaborationRoleBuyer)
                .param("relatedProducts",serviceName)
                .param("partyId", partyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        // there should be one collaboration group
        Assert.assertSame(test1_expectedCollaborationGroupNumber, collaborationGroupResponse.getSize());
        Assert.assertSame(test1_expectedProcessInstanceGroupNumber,collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().size());

//        collaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getID();
//        CollaborationGroupDAO collaborationGroup = CollaborationGroupDAOUtility.getCollaborationGroupDAO(Long.parseLong(collaborationGroupResponse.getCollaborationGroups().get(0).getFederatedCollaborationGroupMetadatas().get(0).getID()));
//        int sizeOfProcessInstances = collaborationGroup.getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().size();
//        idOfTheLastProcessInstance = collaborationGroup.getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(sizeOfProcessInstances - 1);
//        collaborationGroupID = collaborationGroup.getHjid().toString();

        collaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getID();
        int sizeOfProcessInstances = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().size();
        idOfTheLastProcessInstance = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(sizeOfProcessInstances - 1);
    }

    @Test
    public void test02_archiveCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = post("/collaboration-groups/"+collaborationGroupID+"/archive")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test03_cancelCollaboration() throws Exception{
        // get the collaboration group
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("federationId", TestConfig.federationId)
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
    public void test04_getAssociatedCollaborationGroup() throws Exception{
        // get the collaboration group
        MockHttpServletRequestBuilder request = get("/processInstance/"+idOfTheLastProcessInstance+"/collaboration-group")
                .header("Authorization", TestConfig.responderPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroup collaborationGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroup.class);
        Assert.assertEquals(collaborationGroupID,collaborationGroup.getID());
    }

    // try to archive a non-existing collaboration group
    @Test
    public void test05_archiveCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = post("/collaboration-groups/99999999999/archive")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    // try to restore a non-existing collaboration group
    @Test
    public void test06_restoreCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = post("/collaboration-groups/99999999999/restore")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    @Test
    public void test07_updateCollaborationGroupName() throws Exception {
        // update collaboration group name
        MockHttpServletRequestBuilder request = patch("/collaboration-groups/"+collaborationGroupID)
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("groupName",groupName);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        request = get("/collaboration-groups/"+ CollaborationGroupTest.collaborationGroupID)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroup collaborationGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroup.class);
        Assert.assertEquals(CollaborationGroupTest.groupName,collaborationGroup.getName());
    }

    @Test
    public void test08_restoreCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = post("/collaboration-groups/"+ CollaborationGroupTest.collaborationGroupID+"/restore")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test09_deleteCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = delete("/collaboration-groups/"+ CollaborationGroupTest.collaborationGroupToBeDeletedId)
                .header("Authorization", TestConfig.initiatorPersonId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // check whether the deletion is successful or not
        request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("collaborationRole", collaborationRoleSeller)
                .param("relatedProducts",productName)
                .param("partyID", partyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        Assert.assertSame(0, collaborationGroupResponse.getSize());
    }


    @Test
    public void test10_getOrderProcess() throws Exception{
        MockHttpServletRequestBuilder request = get("/process-instance-groups/order-document")
                .header("Authorization", TestConfig.responderPersonId)
                .param("processInstanceId", CollaborationGroupTest.idOfTheLastProcessInstance);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // OrderType order = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), OrderType.class);
        OrderType order = JsonSerializationUtility.getObjectMapper().readValue(mvcResult.getResponse().getContentAsString(),OrderType.class);
        Assert.assertEquals(test5_productName,order.getOrderLine().get(0).getLineItem().getItem().getName().get(0).getValue());
    }

    @Test
    public void test11_deleteProcessInstanceGroupAlongWithTheAssociatedGroup() throws Exception {
        // get the process instance group
        MockHttpServletRequestBuilder request = get("/process-instance-groups/" + BusinessProcessExecutionTest.transportProviderProcessInstanceGroupID)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
        ProcessInstanceGroup processInstanceGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroup.class);

        // get process instance id
        processInstanceId = processInstanceGroup.getProcessInstanceIDs().get(0);
        // get id of associated process instance group
        String associatedProcessInstanceGroupId = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(TestConfig.sellerPartyID,TestConfig.federationId,processInstanceGroup.getProcessInstanceIDs()).getID();
        // delete the group
        request = delete("/process-instance-groups/" + BusinessProcessExecutionTest.transportProviderProcessInstanceGroupID)
                .header("Authorization", TestConfig.initiatorPersonId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        // delete the associated group
        request = delete("/process-instance-groups/" + associatedProcessInstanceGroupId)
                .header("Authorization", TestConfig.initiatorPersonId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    /*
        Try to cancel a completed process instance
        */
    @Test
    public void test12_cancelProcessInstance() throws Exception {
        // cancel the process instance
        MockHttpServletRequestBuilder request = post("/processInstance/"+processInstanceId+"/cancel")
                .header("Authorization", TestConfig.initiatorPersonId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    /*
        Try to cancel a process instance which is already cancelled
     */
    @Test
    public void test13_cancelProcessInstance() throws Exception {
        // cancel the process instance
        MockHttpServletRequestBuilder request = post("/processInstance/"+ CollaborationGroupTest.cancelledProcessInstanceId+"/cancel")
                .header("Authorization", TestConfig.initiatorPersonId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    }

    /**
     * Retrieve the process instance id for the document used in {{@link BusinessProcessWorkflowTests#test1_startProcessInstance()}}
     * */
    @Test
    public void test13_getProcessInstanceIdForDocument() throws Exception {
        MockHttpServletRequestBuilder request = get("/processInstance/document/2892f360-763f-4e26-843d-c6347d9114ff")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        String processInstanceId = mvcResult.getResponse().getContentAsString();
        Assert.assertEquals(processInstanceId,BusinessProcessWorkflowTests.processInstanceID);
    }
}
