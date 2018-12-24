package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by dogukan on 16.07.2018.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class Test19_ApplicationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper().
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
            configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

    private final String processConfigJSON = "/controller/processConfigurationJSON1.txt";
    private final String processConfigJSON2 = "/controller/processConfigurationJSON2.txt";

    private final String expectedType = "SUCCESS";

    @Test
    public void test1_addProcessConfiguration() throws Exception {
        String processConfig = IOUtils.toString(ProcessConfiguration.class.getResourceAsStream(processConfigJSON));
        MockHttpServletRequestBuilder request = post("/application")
                .contentType(MediaType.APPLICATION_JSON)
                .content(processConfig);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response.getType());
    }

    @Test
    public void test2_addProcessConfiguration() throws Exception {
        String processConfig = IOUtils.toString(ProcessConfiguration.class.getResourceAsStream(processConfigJSON2));
        MockHttpServletRequestBuilder request = post("/application")
                .contentType(MediaType.APPLICATION_JSON)
                .content(processConfig);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        ModelApiResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedType, response.getType());
    }
}
