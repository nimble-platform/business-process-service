package eu.nimble.service.bp.impl.controller;

import com.google.gson.Gson;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test01_StartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final String orderJSON1 = "/controller/orderJSON1.txt";
    private final String orderJSON2 = "/controller/orderJSON2.txt";
    private final String orderJSON3 = "/controller/orderJSON3.txt";
    private final String iirJSON1 = "/controller/itemInformationRequestJSON1.txt";

    public final static String orderId1 = "5b15c501-b90a-4f9c-ab0c-ca695e255237";
    public final static String iirId1 = "07ed85d2-3319-4dec-87f0-792d46a7c9a5";

    public static String processInstanceIdOrder1;
    public static String processInstanceIdOrder2;
    public static String processInstanceIdOrder3;
    public static String processInstanceIdIIR1;

    @Test
    public void test1_startProcessInstance() throws Exception {
        Gson gson = new Gson();
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderJSON1));

        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = gson.fromJson(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);

        processInstanceIdOrder1 = processInstance.getProcessInstanceID();
    }

    @Test
    public void test2_startProcessInstance() throws Exception {
        Gson gson = new Gson();
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderJSON2));

        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = gson.fromJson(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);

        processInstanceIdOrder2 = processInstance.getProcessInstanceID();
    }

    @Test
    public void test3_startProcessInstance() throws Exception {
        Gson gson = new Gson();
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderJSON3));

        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = gson.fromJson(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);

        processInstanceIdOrder3 = processInstance.getProcessInstanceID();
    }

    @Test
    public void test4_startProcessInstance() throws Exception {
        Gson gson = new Gson();
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(iirJSON1));

        MockHttpServletRequestBuilder request = post("/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = gson.fromJson(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);

        processInstanceIdIIR1 = processInstance.getProcessInstanceID();
    }
}
