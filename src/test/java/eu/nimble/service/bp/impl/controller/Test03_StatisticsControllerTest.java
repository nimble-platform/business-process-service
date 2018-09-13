package eu.nimble.service.bp.impl.controller;

import com.google.gson.Gson;
import eu.nimble.service.bp.impl.model.statistics.BusinessProcessCount;
import eu.nimble.service.bp.impl.model.statistics.NonOrderedProducts;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder
@RunWith(SpringJUnit4ClassRunner.class)
public class Test03_StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private Gson gson = new Gson();

    private final String token = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxYnNrM09PZkNzdWF0LXV1X0lqU2JxX2QwMmtZM2NteXJheUpXeE93MmlZIn0.eyJqdGkiOiJmNDc0NjIwNy0xNmQ2LTRiNGEtYTBlNi00ZGNiNmE3NzNlOTMiLCJleHAiOjE1MzA4NjY0OTYsIm5iZiI6MCwiaWF0IjoxNTMwNzgwMDk2LCJpc3MiOiJodHRwOi8va2V5Y2xvYWs6ODA4MC9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJuaW1ibGVfY2xpZW50Iiwic3ViIjoiMWVlNmIyNzEtM2MyMy00YTZiLWJlMTktYmI3ZWJmNjVlYTVjIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibmltYmxlX2NsaWVudCIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6IjQyMmNmY2QzLTE3YzQtNGNlMy05MGQyLTc5NjRkNzRiZGRiNiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsibGVnYWxfcmVwcmVzZW50YXRpdmUiLCJuaW1ibGVfdXNlciIsImluaXRpYWxfcmVwcmVzZW50YXRpdmUiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sIm5hbWUiOiJhbGkgY2FuIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiY2FuQGdtYWlsLmNvbSIsImdpdmVuX25hbWUiOiJhbGkiLCJmYW1pbHlfbmFtZSI6ImNhbiIsImVtYWlsIjoiY2FuQGdtYWlsLmNvbSJ9.benTi_ujLLWFlKWXgawTOqT3K_p2jLIANNnyqVITRq8LS9iBcPH53itcSI8MoTu1vhNLq7EzlSjRyTaZeIJGWgzFaAZ5gR3k2rQXuw_dTHsX3jPQalHTkWNeljpxhF4qBYwF4DHq8zvpTjVWApB54GEsLpkJ3rW75cffBWMXxEKPdckovtSk5KYcW77327WjsXe8eOG7u4JtFwQfTH40XGxiIr0gMcv7ZIPdj0iJwXAhx4sE38pFik8VcvcT3EqooCmUxuVSdKpusOeUDrJxOCTKKAV7SYUgOiw_3OBZfIojYpdgZDABCuqR9l6Abj1U9rljSpcy9jfptdluasAAlA";

    private final String companyId = "706";
    private final String role = "SELLER";
    private final String statusTradingVolume = "WaitingResponse";
    private final String statusProcessCount = "Denied";
    private final double delta = 0.1;
    private final double tradingVolumeExpected = 5590;
    private final int expectedProcessCount = 1;
    private final int expectedCompanyCount = 1;
    private final String businessProcessType = "ORDER";
    private final int expectedProcessCountDown = 2;

    @Test
    public void getTradingVolume() throws Exception {
        MockHttpServletRequestBuilder request = get("/statistics/trading-volume")
                .param("companyId", companyId)
                .param("role", role)
                .param("status", statusTradingVolume);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        double tradingVolume = gson.fromJson(mvcResult.getResponse().getContentAsString(), double.class);
        Assert.assertEquals(tradingVolumeExpected, tradingVolume, delta);
    }

    @Test
    public void getProcessCount() throws Exception {
        MockHttpServletRequestBuilder request = get("/statistics/total-number/business-process")
                .param("status", statusProcessCount);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        int processCount = gson.fromJson(mvcResult.getResponse().getContentAsString(), int.class);

        Assert.assertEquals(expectedProcessCount, processCount);
    }

    @Test
    public void getNonOrderedProducts() throws Exception {
        MockHttpServletRequestBuilder request = get("/statistics/non-ordered")
                .param("companyId", companyId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        NonOrderedProducts nonOrderedProducts = gson.fromJson(mvcResult.getResponse().getContentAsString(), NonOrderedProducts.class);

        Assert.assertEquals(expectedCompanyCount, nonOrderedProducts.getCompanies().size());
    }

    @Test
    public void getProcessCountBreakDown() throws Exception {
        MockHttpServletRequestBuilder request = get("/statistics/total-number/business-process/break-down")
                .param("companyId", companyId)
                .param("role", role)
                .param("businessProcessType", businessProcessType)
                .header("Authorization", token);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        BusinessProcessCount count = gson.fromJson(mvcResult.getResponse().getContentAsString(), BusinessProcessCount.class);

        Assert.assertSame(expectedProcessCountDown, count.getCounts().size());
    }
}
