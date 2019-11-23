package eu.nimble.service.bp.impl.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class PaymentControllerTest {

    private String orderId = "5b15c501-b90a-4f9c-ab0c-ca695e255237"; // orderJSON1.txt

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test scenario:
     * - Create an invoice (payment is done) for the order {{@link StartControllerTest#test1_startProcessInstance()}}
     * - Check whether the payment is done for the order {{@link StartControllerTest#test1_startProcessInstance()}}
     */

    @Test
    public void test01_paymentDone() throws Exception {

        MockHttpServletRequestBuilder request = post("/paymentDone/"+orderId)
                .header("Authorization", TestConfig.responderPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }

    @Test
    public void test02_isPaymentDone() throws Exception {

        MockHttpServletRequestBuilder request = get("/paymentDone/"+orderId)
                .header("Authorization", TestConfig.responderPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        Assert.assertEquals("true", mvcResult.getResponse().getContentAsString());
    }

}
