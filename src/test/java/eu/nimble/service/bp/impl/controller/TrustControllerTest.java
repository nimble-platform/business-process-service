package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.model.trust.NegotiationRatings;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class TrustControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private final String partyID = "706";
    private final String buyerPartyID = "1339";
    private final String collaborationRole = "BUYER";
    private final String relatedProduct = "QDeneme";

    public static String processInstanceId;

    /**
     * Test scenario:
     * - Retrieve a collaboration group for a buyer user
     * - Create ratings and reviews for the last process instance of the collaboration group
     * - Check whether the collaboration is rated
     * - Retrieve rating summary
     * - Retrieve all individual ratings
     * - Retrieve rating summary non-existing company (bad request expected)
     * - Retrieve individual ratings for non-existing company (bad request expected)
     */

    @Test
    public void test1_createRatingAndReview() throws Exception {
        // get Receipt advice process instance id
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("collaborationRole", collaborationRole)
                .param("relatedProducts",relatedProduct)
                .param("partyID", buyerPartyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        int size = collaborationGroupResponse.getCollaborationGroups().get(1).getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().size();
        processInstanceId = collaborationGroupResponse.getCollaborationGroups().get(1).getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(size - 1);

        String ratings = "[{\"id\":\"QualityOfTheNegotiationProcess\",\"valueDecimal\":5},{\"id\":\"QualityOfTheOrderingProcess\",\"valueDecimal\":3},{\"id\":\"ResponseTime\",\"valueDecimal\":4},{\"id\":\"ProductListingAccuracy\",\"valueDecimal\":2},{\"id\":\"ConformanceToOtherAgreedTerms\",\"valueDecimal\":5},{\"id\":\"DeliveryAndPackaging\",\"valueDecimal\":3}]";
        String reviews = "[{\"comment\":\"It's working\",\"typeCode\":{\"value\":\"that's ok\",\"name\":\"\",\"uri\":\"\",\"listID\":\"\",\"listURI\":\"\"}},{\"comment\":\"not bad\",\"typeCode\":{\"value\":\"cool\",\"name\":\"\",\"uri\":\"\",\"listID\":\"\",\"listURI\":\"\"}}]";
        // create ratings and reviews
        request = post("/ratingsAndReviews")
                .header("Authorization",TestConfig.responderPersonId)
                .header("federationId",TestConfig.federationId)
                .param("processInstanceID",processInstanceId)
                .param("reviews", reviews)
                .param("ratings",ratings)
                .param("partyId", partyID);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

    }

    @Test
    public void test2_isRated() throws Exception{
        MockHttpServletRequestBuilder request = get("/processInstance/"+processInstanceId+"/isRated")
                .header("Authorization", TestConfig.responderPersonId)
                .header("federationId",TestConfig.federationId)
                .param("partyId","706");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        Assert.assertEquals("true",mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void test3_getRatingsSummary() throws Exception {
        MockHttpServletRequestBuilder request = get("/ratingsSummary")
                .header("Authorization", TestConfig.responderPersonId)
                .header("federationId",TestConfig.federationId)
                .param("partyId",partyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test4_listAllIndividualRatingsAndReviews() throws Exception {
        MockHttpServletRequestBuilder request = get("/ratingsAndReviews")
                .header("Authorization", TestConfig.responderPersonId)
                .header("federationId",TestConfig.federationId)
                .param("partyId",partyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        List<NegotiationRatings> negotiationRatings = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),new TypeReference<List<NegotiationRatings>>(){});
        // get ratings for the process instance id
        for(NegotiationRatings ratings:negotiationRatings){
            if(ratings.getProcessInstanceID().equals(TrustControllerTest.processInstanceId)){
                Assert.assertEquals(6,ratings.getRatings().size());
            }
        }
    }

    // try to get Ratings Summary of a company which does not have a QualifyingParty
    @Test
    public void test5_getRatingsSummaryForNonExistingCompany() throws Exception {
        MockHttpServletRequestBuilder request = get("/ratingsSummary")
                .header("Authorization", TestConfig.responderPersonId)
                .param("partyId","9999");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    }

    // try to get Individual Ratings and Reviews of a company which does not have a QualifyingParty
    @Test
    public void test6_listAllIndividualRatingsAndReviewsForNonExistingParty() throws Exception {
        MockHttpServletRequestBuilder request = get("/ratingsAndReviews")
                .header("Authorization", TestConfig.responderPersonId)
                .param("partyId","9999");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    }
}
