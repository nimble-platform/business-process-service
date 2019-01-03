package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.impl.util.bp.DocumentEnumClassMapper;
import eu.nimble.service.bp.impl.contract.ContractGenerator;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.processor.orderresponse.DefaultOrderResponseSender;
import eu.nimble.service.bp.swagger.model.*;
import eu.nimble.service.bp.swagger.model.Process;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
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
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test04_BusinessProcessesTest {

    @Autowired
    private Environment environment;

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

    private String itemInformationProcessInstanceID;
    private String PPAPProcessInstanceID;
    private String negotiationProcessInstanceID;
    private String orderProcessInstanceID;
    private String tepItemInformationProcessInstanceID;
    private String tepNegotiationProcessInstanceID;
    private String tepProcessInstanceID;
    private String dispatchProcessInstanceID;

    private String sellerCollaborationGroupID;
    private String sellerProcessInstanceGroupID;
    private String sellerTransportProcessInstanceGroupID;
    private String buyerCollaborationGroupID;
    private String buyerProcessInstanceGroupID;
    private String transportProviderCollaborationGroupID;
    private String transportProviderProcessInstanceGroupID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        itemInformationProcessInstanceID = processInstance.getProcessInstanceID();

        // get collaboration group information for seller
        request = get("/group")
                .param("partyID", partyID)
                .param("relatedProducts", productName)
                .param("collaborationRole", "SELLER")
                .param("offset", offset)
                .param("limit", limit);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        Assert.assertSame(1, collaborationGroupResponse.getSize());


        // set collaboration group and process instance groups ids
        sellerCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getID();
        buyerCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedCollaborationGroups().get(0).toString();
        sellerProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getID();
        buyerProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getAssociatedGroups().get(0);
    }

    public void test02_ItemInformationResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid", itemInformationProcessInstanceID);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test03_PPAPRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(PPAPRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", buyerProcessInstanceGroupID)
                .param("collaborationGID", buyerCollaborationGroupID)
                .param("precedingPid", itemInformationProcessInstanceID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        PPAPProcessInstanceID = processInstance.getProcessInstanceID();
    }


    public void test04_PPAPResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(PPAPResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid", PPAPProcessInstanceID);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test05_NegotiationRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(negotiationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", buyerProcessInstanceGroupID)
                .param("collaborationGID", buyerCollaborationGroupID)
                .param("precedingPid", PPAPProcessInstanceID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        negotiationProcessInstanceID = processInstance.getProcessInstanceID();
    }

    public void test06_NegotiationResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(negotiationResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid", negotiationProcessInstanceID);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }


    public void test07_OrderRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", buyerProcessInstanceGroupID)
                .param("collaborationGID", buyerCollaborationGroupID)
                .param("precedingPid", PPAPProcessInstanceID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        orderProcessInstanceID = processInstance.getProcessInstanceID();
    }

    public void test08_OrderResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid", orderProcessInstanceID);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test09_TEPItemInformationRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepItemInformationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("collaborationGID", sellerCollaborationGroupID)
                .param("precedingGid", sellerProcessInstanceGroupID)
                .param("precedingPid", orderProcessInstanceID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        tepItemInformationProcessInstanceID = processInstance.getProcessInstanceID();

        // get process instance group info
        request = get("/group")
                .param("partyID", partyID)
                .param("relatedProducts", serviceName)
                .param("collaborationRole", "BUYER")
                .param("offset", offset)
                .param("limit", limit);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        Assert.assertSame(2, collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().size());

        // set collaboration group and process instance groups ids
        sellerTransportProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(1).getID();
        transportProviderCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedCollaborationGroups().get(1).toString();
        transportProviderProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(1).getAssociatedGroups().get(0);
    }

    public void test10_TEPItemInformationResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepItemInformationResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid", tepItemInformationProcessInstanceID);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", transportProviderProcessInstanceGroupID)
                .param("collaborationGID", transportProviderCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test11_TEPNegotiationRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepNegotiationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerTransportProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID)
                .param("precedingPid", tepItemInformationProcessInstanceID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        tepNegotiationProcessInstanceID = processInstance.getProcessInstanceID();
    }

    public void test12_TEPNegotiationResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepNegotiationResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid", tepNegotiationProcessInstanceID);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", transportProviderProcessInstanceGroupID)
                .param("collaborationGID", transportProviderCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test13_TEPRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerTransportProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID)
                .param("precedingPid", tepNegotiationProcessInstanceID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        tepProcessInstanceID = processInstance.getProcessInstanceID();
    }

    public void test14_TEPResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid", tepProcessInstanceID);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", transportProviderProcessInstanceGroupID)
                .param("collaborationGID", transportProviderCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test15_DispatchRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(dispatchRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", sellerTransportProcessInstanceGroupID)
                .param("collaborationGID", sellerCollaborationGroupID)
                .param("precedingPid", tepProcessInstanceID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        dispatchProcessInstanceID = processInstance.getProcessInstanceID();
    }

    public void test16_ReceiptAdviceResponse() throws Exception {
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(receiptAdviceResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid", dispatchProcessInstanceID);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid", buyerProcessInstanceGroupID)
                .param("collaborationGID", buyerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test17_dataChannelCreationConditions() throws Exception {
        DefaultOrderResponseSender defaultOrderResponseSender = new DefaultOrderResponseSender();
        String orderProcessInstanceInputMessageString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderRequestJSON));
        String orderResponseProcessInstanceInputMessageString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderResponseJSON));
        ProcessInstanceInputMessage orderProcessInstanceInputMessage = JsonSerializationUtility.getObjectMapper().readValue(orderProcessInstanceInputMessageString, ProcessInstanceInputMessage.class);
        ProcessInstanceInputMessage orderResponseProcessInstanceInputMessage = JsonSerializationUtility.getObjectMapper().readValue(orderResponseProcessInstanceInputMessageString, ProcessInstanceInputMessage.class);

        OrderType order = JsonSerializationUtility.getObjectMapper().readValue(orderProcessInstanceInputMessage.getVariables().getContent(), OrderType.class);
        OrderResponseSimpleType orderResponse = JsonSerializationUtility.getObjectMapper().readValue(orderResponseProcessInstanceInputMessage.getVariables().getContent(), OrderResponseSimpleType.class);

        Method method = DefaultOrderResponseSender.class.getDeclaredMethod("needToCreateDataChannel", OrderType.class, OrderResponseSimpleType.class);
        method.setAccessible(true);
        Boolean needToCreateDataChannel = (Boolean) method.invoke(defaultOrderResponseSender, order, orderResponse);
        method.setAccessible(false);
        Assert.assertTrue(needToCreateDataChannel);
    }

    @Test
    public void test18_getClauses() throws Exception{
        // get the order
        String orderProcessInstanceInputMessageString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderRequestJSON));
        ProcessInstanceInputMessage orderProcessInstanceInputMessage = JsonSerializationUtility.getObjectMapper().readValue(orderProcessInstanceInputMessageString, ProcessInstanceInputMessage.class);
        OrderType order = JsonSerializationUtility.getObjectMapper().readValue(orderProcessInstanceInputMessage.getVariables().getContent(), OrderType.class);

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
