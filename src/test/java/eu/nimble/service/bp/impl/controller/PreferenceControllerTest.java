package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessPreferences;
import eu.nimble.utility.JsonSerializationUtility;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class PreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final String preferenceJSON = "/controller/processPreferenceJSON.txt";
    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
    private final String test1_expectedType = "SUCCESS";
    private final String preferenceJSON2 = "/controller/processPreferenceJSON2.txt";
    private final String test2_partnerId = "706";
    private final String test2_targetPartnerID = "1024";
    private final String test3_expectedType = "SUCCESS";
    private final String test4_partnerId = "706";
    private final String test4_expectedType = "SUCCESS";
    private final int test4_expectedSize = 3;

    @Test
    public void test1_addProcessPartnerPreference() throws Exception {
        String preference = IOUtils.toString(ProcessPreferences.class.getResourceAsStream(preferenceJSON));
        MockHttpServletRequestBuilder request = post("/preference")
                .header("Authorization", TestConfig.initiatorPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(preference);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(test1_expectedType, response.getType());
    }

    @Test
    public void test2_getProcessPartnerPreference() throws Exception {
        MockHttpServletRequestBuilder request = get("/preference/" + test2_partnerId)
                .header("Authorization", TestConfig.initiatorPersonId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessPreferences response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessPreferences.class);

        Assert.assertEquals(test2_targetPartnerID, response.getPreferences().get(0).getTargetPartnerID());
    }

    @Test
    public void test3_updateProcessPartnerPreference() throws Exception {
        String preference = IOUtils.toString(ProcessPreferences.class.getResourceAsStream(preferenceJSON2));
        MockHttpServletRequestBuilder request = put("/preference")
                .header("Authorization", TestConfig.initiatorPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(preference);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(test3_expectedType, response.getType());
    }

    @Test
    public void test4_getAndDeleteProcessPartnerPreference() throws Exception {
        MockHttpServletRequestBuilder request = get("/preference/" + test4_partnerId)
                .header("Authorization", TestConfig.initiatorPersonId);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessPreferences response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessPreferences.class);

        Assert.assertSame(test4_expectedSize, response.getPreferences().get(0).getProcessOrder().size());


        request = delete("/preference/" + test4_partnerId)
                .header("Authorization", TestConfig.initiatorPersonId);
        mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse apiResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(test4_expectedType, apiResponse.getType());
    }
}
