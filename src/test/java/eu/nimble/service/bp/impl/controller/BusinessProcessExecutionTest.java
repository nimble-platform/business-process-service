package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.contract.ContractGenerator;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.swagger.model.*;
import eu.nimble.service.bp.util.persistence.bp.CollaborationGroupDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.JsonSerializationUtility;
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class BusinessProcessExecutionTest {


    private final String itemInformationRequestJSON = "/controller/itemInformationRequestJSON2.txt";
    private final String itemInformationResponseJSON = "/controller/itemInformationResponseJSON2.txt";
    private final String PPAPRequestJSON = "/controller/PPAPRequestJSON.txt";
    private final String PPAPResponseJSON = "/controller/PPAPResponseJSON.txt";
    private final String negotiationRequestJSON = "/controller/negotiationRequestJSON.txt";
    private final String negotiationResponseJSON = "/controller/negotiationResponseJSON.txt";
    private final String orderRequestJSON = "/controller/orderRequestJSON.txt";
    private final String orderResponseJSON = "/controller/orderResponseJSON.txt";
    private final String tepItemInformationRequestJSON = "/controller/tepItemInformationRequestJSON.txt";
    private final String tepItemInformationResponseJSON = "/controller/tepItemInformationResponseJSON.txt";
    private final String tepNegotiationRequestJSON = "/controller/tepNegotiationRequestJSON.txt";
    private final String tepNegotiationResponseJSON = "/controller/tepNegotiationResponseJSON.txt";
    private final String tepRequestJSON = "/controller/tepRequestJSON.txt";
    private final String tepResponseJSON = "/controller/tepResponseJSON.txt";
    private final String dispatchRequestJSON = "/controller/dispatchRequestJSON.txt";
    private final String receiptAdviceResponseJSON = "/controller/receiptAdviceResponseJSON.txt";

    private final String offset = "0";
    private final String limit = "5";
    private final String partyID = "706";
    private final String productName = "QDeneme";
    private final String serviceName = "QService";

    public static String buyerProcessInstanceGroupID;
    public static String transportProviderProcessInstanceGroupID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test scenario:
     * - Test complete cycle (request and response) of all the available business processes
     */

    @Test
    public void test00_businessProcesses() throws Exception {
        test01_ItemInformationRequest();
        test02_ItemInformationResponse();
        test03_PPAPRequest();
        test04_PPAPResponse();
        test05_NegotiationRequest();
        test06_NegotiationResponse();
        test07_OrderRequest();
        test08_OrderResponse();
        test09_TEPItemInformationRequest();
        test10_TEPItemInformationResponse();
        test11_TEPNegotiationRequest();
        test12_TEPNegotiationResponse();
        test13_TEPRequest();
        test14_TEPResponse();
        test15_DispatchRequest();
        test16_ReceiptAdviceResponse();
    }

    public void test01_ItemInformationRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);

        // get collaboration group information for seller
        request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("federationId",TestConfig.federationId)
                .param("partyId",partyID)
                .param("relatedProducts",productName)
                .param("collaborationRole","SELLER")
                .param("offset",offset)
                .param("limit",limit);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        Assert.assertSame(1, collaborationGroupResponse.getSize());
        // set collaboration group and process instance groups ids
        buyerProcessInstanceGroupID = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(TestConfig.buyerPartyID,TestConfig.federationId,Arrays.asList(processInstance.getProcessInstanceID())).getID();
    }

    public void test02_ItemInformationResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationResponseJSON));

        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.responderPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test03_PPAPRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(PPAPRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }


    public void test04_PPAPResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(PPAPResponseJSON));

        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.responderPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test05_NegotiationRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(negotiationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test06_NegotiationResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(negotiationResponseJSON));

        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.responderPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }


    public void test07_OrderRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test08_OrderResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderResponseJSON));

        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.responderPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test09_TEPItemInformationRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepItemInformationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.responderPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);

        // get process instance group info
        request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("federationId",TestConfig.federationId)
                .param("partyId",partyID)
                .param("relatedProducts",serviceName)
                .param("collaborationRole","BUYER")
                .param("offset",offset)
                .param("limit",limit);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        Assert.assertSame(1, collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().size());
        Assert.assertSame(1, collaborationGroupResponse.getCollaborationGroups().get(0).getFederatedCollaborationGroupMetadatas().size());

        // set collaboration group and process instance groups ids
        transportProviderProcessInstanceGroupID = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(TestConfig.transportProviderPartyId,TestConfig.federationId,Arrays.asList(processInstance.getProcessInstanceID())).getID();
    }

    public void test10_TEPItemInformationResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepItemInformationResponseJSON));

        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.tepPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test11_TEPNegotiationRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepNegotiationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.responderPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test12_TEPNegotiationResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepNegotiationResponseJSON));

        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.tepPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test13_TEPRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.responderPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test14_TEPResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepResponseJSON));

        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.tepPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test15_DispatchRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(dispatchRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.responderPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test16_ReceiptAdviceResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(receiptAdviceResponseJSON));

        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.initiatorPersonId)
                .header("initiatorFederationId",TestConfig.federationId)
                .header("responderFederationId",TestConfig.federationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    /**
     * After {@link #test06_NegotiationResponse} step, a data channel should be created since data monitoring is requested.
     */
    @Test
    public void test17_dataChannelCreation() throws Exception {
        MockHttpServletRequestBuilder request = get("/process-instance-groups/" + buyerProcessInstanceGroupID)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstanceGroup processInstanceGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroup.class);

        Assert.assertNotNull(processInstanceGroup.getDataChannelId());
    }

    @Test
    public void test18_getClauses() throws Exception{
        // get the order
        String orderString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderRequestJSON));
        OrderType order = JsonSerializationUtility.getObjectMapper().readValue(orderString, OrderType.class);

        // getClauses method will be tested
        Method method = ContractGenerator.class.getDeclaredMethod("getClauses", OrderType.class);
        method.setAccessible(true);

        Map<DocumentType,List<ClauseType>> clauses = (Map<DocumentType, List<ClauseType>>) method.invoke(new ContractGenerator(),order);

        // we expect to have one clause for each possible document type (Quotation,PPAPResponse and ItemInformationResponse)
        Assert.assertSame(1,clauses.get(DocumentType.QUOTATION).size());
        Assert.assertSame(1,clauses.get(DocumentType.PPAPRESPONSE).size());
        Assert.assertSame(1,clauses.get(DocumentType.ITEMINFORMATIONRESPONSE).size());
    }
}
