package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.swagger.model.ModelApiResponse;
import eu.nimble.service.bp.swagger.model.Process;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local_dev")
@FixMethodOrder
@RunWith(SpringJUnit4ClassRunner.class)
public class Test07_ContentControllerTest2 {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();
    private final String expectedResult = "SUCCESS";
    private final String processDefinitionJSON = "/controller/contentJSON.txt";

    @Test
    public void deleteProcessDefinition() throws Exception {
        MockHttpServletRequestBuilder request = delete("/content/" + Test06_ContentControllerTest.process.getProcessID());
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse apiResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedResult, apiResponse.getType());
    }

    @Test
    public void addProcessDefinition() throws Exception {
        String processDefJSON = IOUtils.toString(Process.class.getResourceAsStream(processDefinitionJSON));

        MockHttpServletRequestBuilder request = post("/content")
                .contentType(MediaType.APPLICATION_JSON)
                .content(processDefJSON);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andReturn();

        ModelApiResponse apiResponse = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ModelApiResponse.class);

        Assert.assertEquals(expectedResult, apiResponse.getType());
    }

}
