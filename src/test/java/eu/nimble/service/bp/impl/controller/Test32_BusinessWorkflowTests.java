package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.util.persistence.catalogue.PartyPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.TrustPersistenceUtility;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test32_BusinessWorkflowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String itemInformationRequestJSON = "/controller/itemInformationRequestJSON5.txt";
    private final String itemInformationResponseJSON = "/controller/itemInformationResponseJSON5.txt";
    private final String ppapRequestJSON = "/controller/PPAPRequestJSON3.txt";

    private static String processInstanceID;
    public static String sellerCollaborationGroupID;
    private static String sellerProcessInstanceGroupID;

    @Test
    public void test1_startProcessInstance() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .header("Authorization", "745")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        processInstanceID = processInstance.getProcessInstanceID();

        // get collaboration group information for seller
        request = get("/collaboration-groups")
                .header("Authorization", "1337")
                .param("partyID","1339")
                .param("relatedProducts","QExample local")
                .param("collaborationRole","SELLER")
                .param("offset","0")
                .param("limit","5");
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        // set collaboration group and process instance groups ids
        sellerCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getID();
        sellerProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getID();
    }

    /*
        Check whether the Completed Task is created for the process instance or not
     */
    @Test
    public void test2_startProcessInstance() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid", processInstanceID);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", "1337")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get QualifyingParty of seller
        QualifyingPartyType qualifyingParty = PartyPersistenceUtility.getQualifyingPartyType("747", "745");
        // since the seller's workflow consists of only Item_Information_Request, we expect to have a CompletedTask for this process instance
        Assert.assertEquals(true, TrustPersistenceUtility.completedTaskExist(qualifyingParty,processInstanceID));
    }

    /* In this test case, we try to start a PPAP process.
       However, since PPAP is not included in the workflow of seller company, we expect to get a BadRequestException.*/
    @Test
    public void test3_startProcessInstance() throws Exception {
        Gson gson = new Gson();
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(ppapRequestJSON));

        MockHttpServletRequestBuilder request = post("/start")
                .header("Authorization", "745")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    }
}
