package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.swagger.model.CollaborationGroup;
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
public class Test24_CollaborationGroupTest2 {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private final String productName = "QDeneme";
    private final String collaborationRole = "SELLER";
    private final String relatedProduct = "QProduct";
    private final String partyID = "706";

    @Test
    public void test1_getCollaborationGroup() throws Exception {
        MockHttpServletRequestBuilder request = get("/collaboration-groups/"+Test23_CollaborationGroupTest.collaborationGroupID)
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroup collaborationGroup = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroup.class);
        Assert.assertEquals(Test23_CollaborationGroupTest.groupName,collaborationGroup.getName());
    }

    @Test
    public void test2_restoreCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = post("/collaboration-groups/"+Test23_CollaborationGroupTest.collaborationGroupID+"/restore")
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test3_deleteCollaborationGroup() throws Exception{
        MockHttpServletRequestBuilder request = delete("/collaboration-groups/"+Test23_CollaborationGroupTest.collaborationGroupToBeDeletedId)
                .header("Authorization", TestConfig.initiatorPersonId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        // check whether the deletion is successful or not
        request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("collaborationRole", collaborationRole)
                .param("relatedProducts",relatedProduct)
                .param("partyID", partyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        Assert.assertSame(0, collaborationGroupResponse.getSize());
    }

    @Test
    public void test4_getOrderProcess() throws Exception{
        MockHttpServletRequestBuilder request = get("/process-instance-groups/order-document")
                .header("Authorization", TestConfig.responderPersonId)
                .param("processInstanceId",Test23_CollaborationGroupTest.idOfTheLastProcessInstance);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

       // OrderType order = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), OrderType.class);
        OrderType order = JsonSerializationUtility.getObjectMapper().readValue(mvcResult.getResponse().getContentAsString(),OrderType.class);
        Assert.assertEquals(productName,order.getOrderLine().get(0).getLineItem().getItem().getName().get(0).getValue());
    }

}
