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
@FixMethodOrder
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test04_BusinessProcessesTest {

    private final String token = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxYnNrM09PZkNzdWF0LXV1X0lqU2JxX2QwMmtZM2NteXJheUpXeE93MmlZIn0.eyJqdGkiOiJmNDc0NjIwNy0xNmQ2LTRiNGEtYTBlNi00ZGNiNmE3NzNlOTMiLCJleHAiOjE1MzA4NjY0OTYsIm5iZiI6MCwiaWF0IjoxNTMwNzgwMDk2LCJpc3MiOiJodHRwOi8va2V5Y2xvYWs6ODA4MC9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJuaW1ibGVfY2xpZW50Iiwic3ViIjoiMWVlNmIyNzEtM2MyMy00YTZiLWJlMTktYmI3ZWJmNjVlYTVjIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibmltYmxlX2NsaWVudCIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6IjQyMmNmY2QzLTE3YzQtNGNlMy05MGQyLTc5NjRkNzRiZGRiNiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsibGVnYWxfcmVwcmVzZW50YXRpdmUiLCJuaW1ibGVfdXNlciIsImluaXRpYWxfcmVwcmVzZW50YXRpdmUiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sIm5hbWUiOiJhbGkgY2FuIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiY2FuQGdtYWlsLmNvbSIsImdpdmVuX25hbWUiOiJhbGkiLCJmYW1pbHlfbmFtZSI6ImNhbiIsImVtYWlsIjoiY2FuQGdtYWlsLmNvbSJ9.benTi_ujLLWFlKWXgawTOqT3K_p2jLIANNnyqVITRq8LS9iBcPH53itcSI8MoTu1vhNLq7EzlSjRyTaZeIJGWgzFaAZ5gR3k2rQXuw_dTHsX3jPQalHTkWNeljpxhF4qBYwF4DHq8zvpTjVWApB54GEsLpkJ3rW75cffBWMXxEKPdckovtSk5KYcW77327WjsXe8eOG7u4JtFwQfTH40XGxiIr0gMcv7ZIPdj0iJwXAhx4sE38pFik8VcvcT3EqooCmUxuVSdKpusOeUDrJxOCTKKAV7SYUgOiw_3OBZfIojYpdgZDABCuqR9l6Abj1U9rljSpcy9jfptdluasAAlA";
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
    public void test_businessProcesses() throws Exception{
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
        request = get("/collaboration-groups")
                .param("partyID",partyID)
                .param("relatedProducts",productName)
                .param("collaborationRole","SELLER")
                .param("offset",offset)
                .param("limit",limit);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),CollaborationGroupResponse.class);

        Assert.assertSame(1,collaborationGroupResponse.getSize());


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
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",sellerProcessInstanceGroupID)
                .param("collaborationGID",sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test03_PPAPRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(PPAPRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",buyerProcessInstanceGroupID)
                .param("collaborationGID",buyerCollaborationGroupID)
                .param("precedingPid",itemInformationProcessInstanceID);
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
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",sellerProcessInstanceGroupID)
                .param("collaborationGID",sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test05_NegotiationRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(negotiationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",buyerProcessInstanceGroupID)
                .param("collaborationGID",buyerCollaborationGroupID)
                .param("precedingPid",PPAPProcessInstanceID);
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
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",sellerProcessInstanceGroupID)
                .param("collaborationGID",sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }


    public void test07_OrderRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",buyerProcessInstanceGroupID)
                .param("collaborationGID",buyerCollaborationGroupID)
                .param("precedingPid",PPAPProcessInstanceID);
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
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",sellerProcessInstanceGroupID)
                .param("collaborationGID",sellerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test09_TEPItemInformationRequest() throws Exception{
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepItemInformationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("collaborationGID",sellerCollaborationGroupID)
                .param("precedingGid",sellerProcessInstanceGroupID)
                .param("precedingPid",orderProcessInstanceID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // get process instance id
        ProcessInstance processInstance = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        tepItemInformationProcessInstanceID = processInstance.getProcessInstanceID();

        // get process instance group info
        request = get("/collaboration-groups")
                .param("partyID",partyID)
                .param("relatedProducts",serviceName)
                .param("collaborationRole","BUYER")
                .param("offset",offset)
                .param("limit",limit);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),CollaborationGroupResponse.class);

        Assert.assertSame(2,collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().size());

        // set collaboration group and process instance groups ids
        sellerTransportProcessInstanceGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(1).getID();
        transportProviderCollaborationGroupID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedCollaborationGroups().get(1).toString();
        transportProviderProcessInstanceGroupID  = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(1).getAssociatedGroups().get(0);
    }

    public void test10_TEPItemInformationResponse() throws Exception{
        // replace the process instance id
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepItemInformationResponseJSON));
        inputMessageAsString = inputMessageAsString.replace("pid", tepItemInformationProcessInstanceID);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",transportProviderProcessInstanceGroupID)
                .param("collaborationGID",transportProviderCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test11_TEPNegotiationRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepNegotiationRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",sellerTransportProcessInstanceGroupID)
                .param("collaborationGID",sellerCollaborationGroupID)
                .param("precedingPid",tepItemInformationProcessInstanceID);
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
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",transportProviderProcessInstanceGroupID)
                .param("collaborationGID",transportProviderCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test13_TEPRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(tepRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",sellerTransportProcessInstanceGroupID)
                .param("collaborationGID",sellerCollaborationGroupID)
                .param("precedingPid",tepNegotiationProcessInstanceID);
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
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",transportProviderProcessInstanceGroupID)
                .param("collaborationGID",transportProviderCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    public void test15_DispatchRequest() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(dispatchRequestJSON));

        // start business process
        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",sellerTransportProcessInstanceGroupID)
                .param("collaborationGID",sellerCollaborationGroupID)
                .param("precedingPid",tepProcessInstanceID);
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
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString)
                .param("gid",buyerProcessInstanceGroupID)
                .param("collaborationGID",buyerCollaborationGroupID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }
}
