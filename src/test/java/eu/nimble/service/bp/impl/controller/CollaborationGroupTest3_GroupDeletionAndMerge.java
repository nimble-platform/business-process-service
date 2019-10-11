package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.model.hyperjaxb.FederatedCollaborationGroupMetadataDAO;
import eu.nimble.service.bp.swagger.model.CollaborationGroup;
import eu.nimble.service.bp.swagger.model.FederatedCollaborationGroupMetadata;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceGroupDAOUtility;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
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

import java.util.Arrays;

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
/*
    Here is the scenario:
        * Buyer initiates an item information request
        * Seller accepts the request
        * Then, buyer initiates a PPAP request and deletes his collaboration group
        * Seller accepts the PPAP request
        * We expect that a new collaboration group will be created for the buyer
        * Then, the buyer merges two collaboration groups: the group created in CollaborationGroupTest2_GroupDeletion and the group created in CollaborationGroupTest3_GroupDeletionAndMerge
        * We retrieve collaboration groups which are projects for the buyer.
        * Finally, the buyer initiates another item information request in the project.
 */
public class CollaborationGroupTest3_GroupDeletionAndMerge {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    private final String itemInformationRequestJSON = "/controller/itemInformationRequestJSON4.txt";
    private final String itemInformationResponseJSON = "/controller/itemInformationResponseJSON4.txt";
    private final String itemInformationRequestJSON2 = "/controller/itemInformationRequestJSON6.txt";
    private final String PPAPRequestJSON = "/controller/PPAPRequestJSON2.txt";
    private final String PPAPResponseJSON = "/controller/PPAPResponseJSON2.txt";
    private final String relatedProduct = "Quantum Product example";
    private final String buyerPartyId = "1339";

    public static String processInstanceIdIIR;
    public static String processInstanceIdPPAP;
    private static String sellerCollaborationGroupID;
    private static String sellerProcessInstanceGroupID;
    private static String buyerCollaborationGroupID;
    private static String buyerProcessInstanceGroupID;

    @Test
    public void test1_startItemInformationRequest() throws Exception{
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationRequestJSON));

        MockHttpServletRequestBuilder request = post("/start")
                .header("Authorization",TestConfig.initiatorPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);
        processInstanceIdIIR = processInstance.getProcessInstanceID();

        CollaborationGroupResponse collaborationGroupResponse = getCollaborationGroupResponse();
        // set collaboration group and process instance groups ids
        buyerCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getID();
        sellerCollaborationGroupID = CollaborationGroupDAOUtility.getCollaborationGroup(TestConfig.sellerPartyID, TestConfig.federationId,Arrays.asList(processInstanceIdIIR)).getHjid().toString();
        buyerProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getID();
        sellerProcessInstanceGroupID = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(TestConfig.sellerPartyID,TestConfig.federationId,Arrays.asList(processInstanceIdIIR)).getID();

    }

    @Test
    public void test2_continueProcess() throws Exception{
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid",processInstanceIdIIR);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", TestConfig.responderPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.COMPLETED);
        Assert.assertEquals(processInstance.getProcessInstanceID(), processInstanceIdIIR);

    }

    @Test
    public void test3_startPPAP() throws Exception{
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(PPAPRequestJSON));

        MockHttpServletRequestBuilder request = post("/start")
                .header("Authorization",TestConfig.initiatorPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", buyerProcessInstanceGroupID)
                .param("collaborationGID", buyerCollaborationGroupID)
                .param("precedingPid", processInstanceIdIIR);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);

        processInstanceIdPPAP = processInstance.getProcessInstanceID();

    }

    /*
        After sending the PPAP request, buyer deletes his collaboration group
     */
    @Test
    public void test4_deleteCollaborationGroup() throws Exception{
        // get the collaboration group id
        CollaborationGroupResponse collaborationGroupResponse = getCollaborationGroupResponse();

        Assert.assertSame(1, collaborationGroupResponse.getSize());
        String collaborationGroupId = collaborationGroupResponse.getCollaborationGroups().get(0).getID();

        // delete the collaboration group
        MockHttpServletRequestBuilder request = delete("/collaboration-groups/"+collaborationGroupId)
                .header("Authorization",TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // check whether the deletion is successful or not
        collaborationGroupResponse = getCollaborationGroupResponse();

        Assert.assertSame(0, collaborationGroupResponse.getSize());
    }

    @Test
    public void test5_continueProcess() throws Exception{
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(PPAPResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid",processInstanceIdPPAP);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", TestConfig.responderPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.COMPLETED);
        Assert.assertEquals(processInstance.getProcessInstanceID(), processInstanceIdPPAP);
    }

    /*
        After seller sends the PPAP response, check whether the new collaboration group is created for the buyer or not
     */
    @Test
    public void test6_checkCollaborationGroup() throws Exception{
        CollaborationGroupResponse collaborationGroupResponse = getCollaborationGroupResponse();
        Assert.assertSame(1,collaborationGroupResponse.getSize());
        // update buyer's collaboration group id
        buyerCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getID();
    }

    /*
        The buyer merges two collaboration groups: the group created in CollaborationGroupTest2_GroupDeletion and the group created in CollaborationGroupTest3_GroupDeletionAndMerge
     */
    @Test
    public void test7_mergeCollaborationGroups() throws Exception{
        FederatedCollaborationGroupMetadata federatedCollaborationGroupMetadata = new FederatedCollaborationGroupMetadata();
        federatedCollaborationGroupMetadata.setID(CollaborationGroupTest2_GroupDeletion.buyerCollaborationGroupID);
        federatedCollaborationGroupMetadata.setFederationID(TestConfig.federationId);
        String body = mapper.writeValueAsString(Arrays.asList(federatedCollaborationGroupMetadata));
        MockHttpServletRequestBuilder request = post("/collaboration-groups/merge")
                .header("Authorization", TestConfig.responderPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .param("bcid", buyerCollaborationGroupID)
                .param("currentInstanceFederationId",TestConfig.federationId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // try to get the old group which is merged to the new one
        request = get("/collaboration-groups/"+ CollaborationGroupTest2_GroupDeletion.buyerCollaborationGroupID)
                .header("Authorization", TestConfig.responderPersonId);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
        // get the new one
        request = get("/collaboration-groups/"+buyerCollaborationGroupID)
                .header("Authorization", TestConfig.responderPersonId);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CollaborationGroup collaborationGroup = mapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroup.class);
        Assert.assertEquals(true,collaborationGroup.getIsProject());
    }

    /*
        For the buyer, retrieve collaboration groups which are projects.
     */
    @Test
    public void test8_getCollaborationGroups() throws Exception{
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("federationId",TestConfig.federationId)
                .param("partyId", TestConfig.buyerPartyID)
                .param("isProject","true");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = mapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);
        Assert.assertSame(1,collaborationGroupResponse.getSize());
        Assert.assertEquals(buyerCollaborationGroupID,collaborationGroupResponse.getCollaborationGroups().get(0).getID());
        // update buyer process instance group id
        buyerProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getID();
    }

    /*
        After merging collaboration groups, check whether the buyer can start new processes in this project or not.
     */
    @Test
    public void test9_startItemInformationRequest() throws Exception{
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationRequestJSON2));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", buyerProcessInstanceGroupID)
                .param("collaborationGID", buyerCollaborationGroupID)
                .param("precedingPid", processInstanceIdPPAP);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    private CollaborationGroupResponse getCollaborationGroupResponse() throws Exception{
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("federationId",TestConfig.federationId)
                .param("collaborationRole", "BUYER")
                .param("relatedProducts",relatedProduct)
                .param("partyId", buyerPartyId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = mapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);
        return collaborationGroupResponse;
    }
}
