package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.CollaborationGroupResponse;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@ActiveProfiles("local_dev")
@FixMethodOrder
@RunWith(SpringJUnit4ClassRunner.class)
public class Test25_TrustControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private final String token = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxYnNrM09PZkNzdWF0LXV1X0lqU2JxX2QwMmtZM2NteXJheUpXeE93MmlZIn0.eyJqdGkiOiJmNDc0NjIwNy0xNmQ2LTRiNGEtYTBlNi00ZGNiNmE3NzNlOTMiLCJleHAiOjE1MzA4NjY0OTYsIm5iZiI6MCwiaWF0IjoxNTMwNzgwMDk2LCJpc3MiOiJodHRwOi8va2V5Y2xvYWs6ODA4MC9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJuaW1ibGVfY2xpZW50Iiwic3ViIjoiMWVlNmIyNzEtM2MyMy00YTZiLWJlMTktYmI3ZWJmNjVlYTVjIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibmltYmxlX2NsaWVudCIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6IjQyMmNmY2QzLTE3YzQtNGNlMy05MGQyLTc5NjRkNzRiZGRiNiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsibGVnYWxfcmVwcmVzZW50YXRpdmUiLCJuaW1ibGVfdXNlciIsImluaXRpYWxfcmVwcmVzZW50YXRpdmUiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sIm5hbWUiOiJhbGkgY2FuIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiY2FuQGdtYWlsLmNvbSIsImdpdmVuX25hbWUiOiJhbGkiLCJmYW1pbHlfbmFtZSI6ImNhbiIsImVtYWlsIjoiY2FuQGdtYWlsLmNvbSJ9.benTi_ujLLWFlKWXgawTOqT3K_p2jLIANNnyqVITRq8LS9iBcPH53itcSI8MoTu1vhNLq7EzlSjRyTaZeIJGWgzFaAZ5gR3k2rQXuw_dTHsX3jPQalHTkWNeljpxhF4qBYwF4DHq8zvpTjVWApB54GEsLpkJ3rW75cffBWMXxEKPdckovtSk5KYcW77327WjsXe8eOG7u4JtFwQfTH40XGxiIr0gMcv7ZIPdj0iJwXAhx4sE38pFik8VcvcT3EqooCmUxuVSdKpusOeUDrJxOCTKKAV7SYUgOiw_3OBZfIojYpdgZDABCuqR9l6Abj1U9rljSpcy9jfptdluasAAlA";
    private final String partyID = "706";
    private final String buyerPartyID = "1339";
    private final String collaborationRole = "BUYER";
    private final String relatedProduct = "QDeneme";
    @Test
    public void test1_createRatingAndReview() throws Exception {
        // get Receipt advice process instance id
        MockHttpServletRequestBuilder request = get("/collaboration-groups")
                .param("collaborationRole", collaborationRole)
                .param("relatedProducts",relatedProduct)
                .param("partyID", buyerPartyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse collaborationGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);

        int size = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().size();
        String processInstanceID = collaborationGroupResponse.getCollaborationGroups().get(0).getAssociatedProcessInstanceGroups().get(0).getProcessInstanceIDs().get(size - 1);

        String ratings = "[{\"id\":\"QualityOfTheNegotiationProcess\",\"valueDecimal\":5},{\"id\":\"QualityOfTheOrderingProcess\",\"valueDecimal\":3},{\"id\":\"ResponseTime\",\"valueDecimal\":4},{\"id\":\"ProductListingAccuracy\",\"valueDecimal\":2},{\"id\":\"ConformanceToOtherAgreedTerms\",\"valueDecimal\":5},{\"id\":\"DeliveryAndPackaging\",\"valueDecimal\":3}]";
        String reviews = "[{\"comment\":\"It's working\",\"typeCode\":{\"value\":\"that's ok\",\"name\":\"\",\"uri\":\"\",\"listID\":\"\",\"listURI\":\"\"}},{\"comment\":\"not bad\",\"typeCode\":{\"value\":\"cool\",\"name\":\"\",\"uri\":\"\",\"listID\":\"\",\"listURI\":\"\"}}]";
        // create ratings and reviews
        request = post("/ratingsAndReviews")
                .header("Authorization",token)
                .param("processInstanceID",processInstanceID)
                .param("reviews", reviews)
                .param("ratings",ratings)
                .param("partyID", partyID);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

    }
}
