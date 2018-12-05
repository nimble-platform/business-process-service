package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.CollaborationGroupResponse;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;
import org.junit.Assert;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder
@RunWith(SpringJUnit4ClassRunner.class)
public class Test15_ProcessInstanceGroupControllerTest5 {

    @Autowired
    private MockMvc mockMvc;

    private final String partyId = "706";
    private final String token = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxYnNrM09PZkNzdWF0LXV1X0lqU2JxX2QwMmtZM2NteXJheUpXeE93MmlZIn0.eyJqdGkiOiJmNDc0NjIwNy0xNmQ2LTRiNGEtYTBlNi00ZGNiNmE3NzNlOTMiLCJleHAiOjE1MzA4NjY0OTYsIm5iZiI6MCwiaWF0IjoxNTMwNzgwMDk2LCJpc3MiOiJodHRwOi8va2V5Y2xvYWs6ODA4MC9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJuaW1ibGVfY2xpZW50Iiwic3ViIjoiMWVlNmIyNzEtM2MyMy00YTZiLWJlMTktYmI3ZWJmNjVlYTVjIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibmltYmxlX2NsaWVudCIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6IjQyMmNmY2QzLTE3YzQtNGNlMy05MGQyLTc5NjRkNzRiZGRiNiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsibGVnYWxfcmVwcmVzZW50YXRpdmUiLCJuaW1ibGVfdXNlciIsImluaXRpYWxfcmVwcmVzZW50YXRpdmUiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sIm5hbWUiOiJhbGkgY2FuIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiY2FuQGdtYWlsLmNvbSIsImdpdmVuX25hbWUiOiJhbGkiLCJmYW1pbHlfbmFtZSI6ImNhbiIsImVtYWlsIjoiY2FuQGdtYWlsLmNvbSJ9.benTi_ujLLWFlKWXgawTOqT3K_p2jLIANNnyqVITRq8LS9iBcPH53itcSI8MoTu1vhNLq7EzlSjRyTaZeIJGWgzFaAZ5gR3k2rQXuw_dTHsX3jPQalHTkWNeljpxhF4qBYwF4DHq8zvpTjVWApB54GEsLpkJ3rW75cffBWMXxEKPdckovtSk5KYcW77327WjsXe8eOG7u4JtFwQfTH40XGxiIr0gMcv7ZIPdj0iJwXAhx4sE38pFik8VcvcT3EqooCmUxuVSdKpusOeUDrJxOCTKKAV7SYUgOiw_3OBZfIojYpdgZDABCuqR9l6Abj1U9rljSpcy9jfptdluasAAlA";
    private ObjectMapper objectMapper = new ObjectMapper();

    private final int test1_expectedSize = 2;
    private final String test2_expectedValue = "true";
    private final int test2_expectedSize = 0;

    @Test
    public void test1_getProcessInstanceGroupFilters() throws Exception {
        MockHttpServletRequestBuilder request = get("/group/filters")
                .header("Authorization", token)
                .param("collaborationRole", "SELLER")
                .param("partyID", "706");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstanceGroupFilter processInstanceGroupFilter = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroupFilter.class);

        Assert.assertSame(test1_expectedSize, processInstanceGroupFilter.getTradingPartnerIDs().size());

    }

    @Test
    public void test2_finalProcessInstanceGroupControllerTest() throws Exception {
        // test archiveAllGroups
        MockHttpServletRequestBuilder request = post("/group/archive-all")
                .param("partyID", partyId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        String body = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), String.class);

        Assert.assertEquals(test2_expectedValue, body);

        // test deleteAllArchivedGroups
        request = post("/group/delete-all")
                .param("partyID", partyId);

        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        body = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), String.class);

        Assert.assertEquals("true", body);
        // check whether all groups are deleted or not
        request = get("/group").param("partyID", partyId);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        CollaborationGroupResponse processInstanceGroupResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CollaborationGroupResponse.class);
        Assert.assertSame(test2_expectedSize, processInstanceGroupResponse.getSize());
    }
}
