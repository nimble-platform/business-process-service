package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.CollaborationGroupResponse;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;
import eu.nimble.utility.JsonSerializationUtility;
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
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test15_ProcessInstanceGroupControllerTest3 {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;

    private final String partyId = "706";
    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

    private final int test1_expectedSize = 2;
    private final String test2_expectedValue = "true";
    private final int test2_expectedSize = 0;

    @Test
    public void test1_getProcessInstanceGroupFilters() throws Exception {
        MockHttpServletRequestBuilder request = get("/process-instance-groups/filters")
                .header("Authorization", environment.getProperty("nimble.test-responder-person-id"))
                .param("collaborationRole", "SELLER")
                .param("partyId", "706");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstanceGroupFilter processInstanceGroupFilter = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessInstanceGroupFilter.class);

        Assert.assertSame(test1_expectedSize, processInstanceGroupFilter.getTradingPartnerIDs().size());

    }
}
