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
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class Test09_PreferenceControllerTest2 {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Environment environment;

    private final String preferenceJSON = "/controller/processPreferenceJSON2.txt";
    private final String partnerId = "706";
    private final String targetPartnerID = "1024";
    private final String expectedType = "SUCCESS";
    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();

    @Test
    public void getProcessPartnerPreference() throws Exception {
        MockHttpServletRequestBuilder request = get("/preference/" + partnerId)
                .header("Authorization", environment.getProperty("nimble.test-initiator-person-id"));

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ProcessPreferences response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ProcessPreferences.class);

        Assert.assertEquals(targetPartnerID, response.getPreferences().get(0).getTargetPartnerID());
    }

    @Test
    public void updateProcessPartnerPreference() throws Exception {
        String preference = IOUtils.toString(ProcessPreferences.class.getResourceAsStream(preferenceJSON));
        MockHttpServletRequestBuilder request = put("/preference")
                .header("Authorization", environment.getProperty("nimble.test-initiator-person-id"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(preference);

        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response.getType());
    }
}
