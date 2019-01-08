package eu.nimble.service.bp.impl.controller;

import com.google.gson.Gson;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
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
@ActiveProfiles("local_dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test02_ContinueControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;

    private final String orderResponseJSON1 = "/controller/orderResponseJSON1.txt";

    @Test
    public void continueProcessInstance() throws Exception {
        Gson gson = new Gson();
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(orderResponseJSON1));
        inputMessageAsString = inputMessageAsString.replace("pid", Test01_StartControllerTest.processInstanceIdOrder1);

        MockHttpServletRequestBuilder request = post("/continue")
                .header("Authorization", environment.getProperty("nimble.test-responder-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = gson.fromJson(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.COMPLETED);
        Assert.assertEquals(processInstance.getProcessInstanceID(), Test01_StartControllerTest.processInstanceIdOrder1);
    }

}
