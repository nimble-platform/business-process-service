package eu.nimble.service.bp.impl.controller;

import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.utility.JsonSerializationUtility;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("local_dev")
@RunWith(SpringJUnit4ClassRunner.class)
public class Test28_HjidCheckTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;

    private final String itemInformationRequestJSON ="/controller/itemInformationRequestHjidCheck.txt";

    @Test
    public void test1_updateBusinessProcess() throws Exception{
        boolean checkEntityIds = Boolean.valueOf(environment.getProperty("nimble.check-entity-ids"));
        if(checkEntityIds == false) {
            return;
        }
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationRequestJSON));
        // start the business process
        MockHttpServletRequestBuilder request = post("/start")
                .header("Authorization",environment.getProperty("nimble.test-initiator-person-id"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessInstance processInstance = JsonSerializationUtility.getObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ProcessInstance.class);
        Assert.assertEquals(processInstance.getStatus(), ProcessInstance.StatusEnum.STARTED);

        // get document content
        request = get("/document/json/2890ce89-b695-4c51-bae5-c8acd2a48cc6")
                .header("Authorization",environment.getProperty("nimble.test-initiator-person-id"));
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ItemInformationRequestType iir = JsonSerializationUtility.getObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ItemInformationRequestType.class);


        // get document content
        request = get("/document/json/154d8ee1-f6f5-4bh5-9957-58068565eb41")
                .header("Authorization",environment.getProperty("nimble.test-initiator-person-id"));
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ItemInformationRequestType itemInformationRequest = JsonSerializationUtility.getObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), ItemInformationRequestType.class);
        // update hjid field
        itemInformationRequest.getItemInformationRequestLine().get(0).getSalesItem().get(0).getItem().getCommodityClassification().get(0).getItemClassificationCode().setHjid(iir.getHjid());

        request = put("/processInstance")
                .header("Authorization",environment.getProperty("nimble.test-initiator-person-id"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSerializationUtility.getObjectMapper().writeValueAsString(itemInformationRequest))
                .param("processID", "ITEMINFORMATIONREQUEST")
                .param("processInstanceID", processInstance.getProcessInstanceID())
                .param("creatorUserID", "1337");
        mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    }
}
