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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test25_TrustControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;
    @Autowired
    private ObjectMapper objectMapper;

    private final String partyID = "706";
    private final String buyerPartyID = "1339";
    private final String collaborationRole = "BUYER";
    private final String relatedProduct = "QDeneme";

    public static String processInstanceId;
    @Test
    public void test1_createRatingAndReview() throws Exception {
        // get Receipt advice process instance id
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", environment.getProperty("nimble.test-initiator-person-id"))
                .param("collaborationRole", collaborationRole)
                .param("relatedProducts",relatedProduct)
                .param("partyID", buyerPartyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        int size = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().size();
        processInstanceId = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(size - 1);

        String ratings = "[{\"id\":\"QualityOfTheNegotiationProcess\",\"valueDecimal\":5},{\"id\":\"QualityOfTheOrderingProcess\",\"valueDecimal\":3},{\"id\":\"ResponseTime\",\"valueDecimal\":4},{\"id\":\"ProductListingAccuracy\",\"valueDecimal\":2},{\"id\":\"ConformanceToOtherAgreedTerms\",\"valueDecimal\":5},{\"id\":\"DeliveryAndPackaging\",\"valueDecimal\":3}]";
        String reviews = "[{\"comment\":\"It's working\",\"typeCode\":{\"value\":\"that's ok\",\"name\":\"\",\"uri\":\"\",\"listID\":\"\",\"listURI\":\"\"}},{\"comment\":\"not bad\",\"typeCode\":{\"value\":\"cool\",\"name\":\"\",\"uri\":\"\",\"listID\":\"\",\"listURI\":\"\"}}]";
        // create ratings and reviews
        request = post("/ratingsAndReviews")
                .header("Authorization",environment.getProperty("nimble.test-responder-person-id"))
                .param("processInstanceID",processInstanceId)
                .param("reviews", reviews)
                .param("ratings",ratings)
                .param("partyId", partyID);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

    }

    @Test
    public void test2_isRated() throws Exception{
        MockHttpServletRequestBuilder request = get("/processInstance/"+processInstanceId+"/isRated")
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"))
                .param("partyId","706");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        Assert.assertEquals("true",mvcResult.getResponse().getContentAsString());
    }
}
