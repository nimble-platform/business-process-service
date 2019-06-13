package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.impl.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
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
        * Seller accepts the request and deletes his collaboration group
        * Then, buyer initiates a PPAP request
        * We expect that a new collaboration group will be created for the seller
 */
public class Test29_CollaborationGroupTest3 {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    private final String itemInformationRequestJSON = "/controller/itemInformationRequestJSON3.txt";
    private final String itemInformationResponseJSON = "/controller/itemInformationResponseJSON3.txt";
    private final String PPAPRequestJSON = "/controller/PPAPRequestJSON1.txt";
    private final String relatedProduct = "Quantum Example product";
    private final String sellerPartyId = "706";

    public static String processInstanceIdIIR;
    private static String sellerCollaborationGroupID;
    private static String sellerProcessInstanceGroupID;
    private static String buyerCollaborationGroupID;
    private static String buyerProcessInstanceGroupID;

    @Test
    public void test1_startItemInformationRequest() throws Exception{
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationRequestJSON));

        MockHttpServletRequestBuilder request = post("/start")
                .header("Authorization",TestConfig.initiatorPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);
        processInstanceIdIIR = processInstance.getProcessInstanceID();

        // get collaboration group information for seller
        CollaborationGroupResponse collaborationGroupResponse = getCollaborationGroupResponse();

        Assert.assertSame(1, collaborationGroupResponse.getSize());


        // set collaboration group and process instance groups ids
        sellerCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getID();
        buyerCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedCollaborationGroups().get(0).toString();
        sellerProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getID();
        buyerProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getAssociatedGroups().get(0);
    }

    @Test
    public void test2_continueProcess() throws Exception{
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid",processInstanceIdIIR);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", TestConfig.responderPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.COMPLETED);
        Assert.assertEquals(processInstance.getProcessInstanceID(), processInstanceIdIIR);
    }

    /*
        After accepting item information request, seller deletes his collaboration group
     */
    @Test
    public void test3_deleteCollaborationGroup() throws Exception{
        // delete the collaboration group
        MockHttpServletRequestBuilder request = delete("/collaboration-groups/"+sellerCollaborationGroupID)
                .header("Authorization",TestConfig.responderPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = getCollaborationGroupResponse();
        Assert.assertSame(0,collaborationGroupResponse.getSize());
    }

    @Test
    public void test4_startPPAP() throws Exception{
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(PPAPRequestJSON));

        MockHttpServletRequestBuilder request = post("/start")
                .header("Authorization",TestConfig.initiatorPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", buyerProcessInstanceGroupID)
                .param("collaborationGID", buyerCollaborationGroupID)
                .param("precedingPid", processInstanceIdIIR);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);
    }

    /*
        After buyer starts the PPAP request, check whether the new collaboration group is created for the seller or not
     */
    @Test
    public void test5_checkCollaborationGroup() throws Exception{
        CollaborationGroupResponse collaborationGroupResponse = getCollaborationGroupResponse();
        Assert.assertSame(1,collaborationGroupResponse.getSize());
    }

    private CollaborationGroupResponse getCollaborationGroupResponse() throws Exception{
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("partyId", sellerPartyId)
                .param("relatedProducts", relatedProduct)
                .param("collaborationRole", "SELLER")
                .param("offset", "0")
                .param("limit", "5");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CollaborationGroupResponse collaborationGroupResponse = mapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);
        return collaborationGroupResponse;
    }
}
