package eu.nimble.service.bp.impl.controller;

import com.google.gson.Gson;
import eu.nimble.service.bp.model.statistics.BusinessProcessCount;
import eu.nimble.service.bp.model.statistics.NonOrderedProducts;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private Gson gson = new Gson();

    private final String partyId = "706";
    private final String role = "SELLER";
    private final String statusTradingVolume = "WaitingResponse";
    private final String statusProcessCount = "Denied";
    private final double delta = 0.1;
    private final double tradingVolumeExpected = 5590;
    private final int expectedProcessCount = 1;
    private final int expectedCompanyCount = 1;
    private final String businessProcessType = "ORDER";
    private final int expectedProcessCountDown = 2;

    /**
     * Test scenario:
     * - Trading volume of processes in waiting response status
     * - Number of rejected processes
     * - Number of non-ordered products
     * - Test number of order processes via complete process break-down
     */

    @Test
    public void getTradingVolume() throws Exception {
        MockHttpServletRequestBuilder request = get("/statistics/trading-volume")
                .header("Authorization", TestConfig.responderPersonId)
                .param("partyId", partyId)
                .param("role", role)
                .param("status", statusTradingVolume);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        double tradingVolume = gson.fromJson(mvcResult.getResponse().getContentAsString(), double.class);
        Assert.assertEquals(tradingVolumeExpected, tradingVolume, delta);
    }

    @Test
    public void getProcessCount() throws Exception {
        MockHttpServletRequestBuilder request = get("/statistics/total-number/business-process")
                .header("Authorization", TestConfig.responderPersonId)
                .param("status", statusProcessCount);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        int processCount = gson.fromJson(mvcResult.getResponse().getContentAsString(), int.class);

        Assert.assertEquals(expectedProcessCount, processCount);
    }

    @Test
    public void getNonOrderedProducts() throws Exception {
        MockHttpServletRequestBuilder request = get("/statistics/non-ordered")
                .header("Authorization", TestConfig.responderPersonId)
                .param("partyId", partyId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        NonOrderedProducts nonOrderedProducts = gson.fromJson(mvcResult.getResponse().getContentAsString(), NonOrderedProducts.class);

        Assert.assertEquals(expectedCompanyCount, nonOrderedProducts.getCompanies().size());
    }

    @Test
    public void getProcessCountBreakDown() throws Exception {
        MockHttpServletRequestBuilder request = get("/statistics/total-number/business-process/break-down")
                .param("partyId", partyId)
                .param("role", role)
                .param("businessProcessType", businessProcessType)
                .header("Authorization", TestConfig.responderPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        BusinessProcessCount count = gson.fromJson(mvcResult.getResponse().getContentAsString(), BusinessProcessCount.class);

        Assert.assertSame(expectedProcessCountDown, count.getCounts().size());
    }
}
