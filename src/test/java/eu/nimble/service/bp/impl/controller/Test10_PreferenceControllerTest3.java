package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessPreferences;
import eu.nimble.utility.JsonSerializationUtility;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test10_PreferenceControllerTest3 {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
    private final String partnerId = "706";
    private final String expectedType = "SUCCESS";
    private final int expectedSize = 3;

    @Test
    public void getAndDeleteProcessPartnerPreference() throws Exception {
        MockHttpServletRequestBuilder request = get("/preference/" + partnerId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessPreferences response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessPreferences.class);

        Assert.assertSame(expectedSize, response.getPreferences().get(0).getProcessOrder().size());


        request = delete("/preference/" + partnerId);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse apiResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, apiResponse.getType());
    }
}
