package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.CollaborationGroupResponse;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test15_ProcessInstanceGroupControllerTest5 {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;

    private final String partyId = "706";
    private ObjectMapper objectMapper = new ObjectMapper();

    private final int test1_expectedSize = 2;
    private final String test2_expectedValue = "true";
    private final int test2_expectedSize = 0;

    @Test
    public void test1_getProcessInstanceGroupFilters() throws Exception {
        MockHttpServletRequestBuilder request = get("/group/filters")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
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
