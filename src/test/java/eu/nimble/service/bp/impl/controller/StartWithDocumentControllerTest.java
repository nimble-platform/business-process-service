package eu.nimble.service.bp.impl.controller;

import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import org.apache.commons.io.IOUtils;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class StartWithDocumentControllerTest {

    /**
     * Test scenario:
     * - Check whether a process is completed using a response document which does not have a valid reference to a request document
     */

    @Autowired
    private MockMvc mockMvc;

    private final String itemInformationResponseJSON = "/controller/itemInformationResponseJSON6.txt";

    @Test
    public void test01_responseDocumentWithNoReferenceToRequestDocument() throws Exception {
        String inputMessageAsString = IOUtils.toString(ProcessInstanceInputMessage.class.getResourceAsStream(itemInformationResponseJSON));

        MockHttpServletRequestBuilder request = post("/process-document")
                .header("Authorization", TestConfig.responderPersonId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputMessageAsString);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isBadRequest()).andReturn();
    }

}
