package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.CollaborationGroupResponse;
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
import org.springframework.core.env.Environment;
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
@ActiveProfiles("local_dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
/*
    Here is the scenario:
        * Buyer initiates an item information request
        * Seller accepts the request
        * Then, buyer initiates a PPAP request and deletes his collaboration group
        * Seller accepts the PPAP request
        * We expect that a new collaboration group will be created for the buyer
 */
public class Test30_CollaborationGroupTest4 {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;
    @Autowired
    private ObjectMapper mapper;

    private final String itemInformationRequestJSON = "/controller/itemInformationRequestJSON4.txt";
    private final String itemInformationResponseJSON = "/controller/itemInformationResponseJSON4.txt";
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = mapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);
        processInstanceIdIIR = processInstance.getProcessInstanceID();

        CollaborationGroupResponse collaborationGroupResponse = getCollaborationGroupResponse();
        // set collaboration group and process instance groups ids
        buyerCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getID();
        sellerCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedCollaborationGroups().get(0).toString();
        buyerProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getID();
        sellerProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getAssociatedGroups().get(0);

    }

    @Test
    public void test2_continueProcess() throws Exception{
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid",processInstanceIdIIR);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
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
        MockHttpServletRequestBuilder request = delete("/collaboration-groups/"+collaborationGroupId);
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
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
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
    }

    private CollaborationGroupResponse getCollaborationGroupResponse() throws Exception{
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .param("collaborationRole", "BUYER")
                .param("relatedProducts",relatedProduct)
                .param("partyId", buyerPartyId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = mapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);
        return collaborationGroupResponse;
    }
}
