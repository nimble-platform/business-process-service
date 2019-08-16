package eu.nimble.service.bp.impl.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.dashboard.CollaborationGroupResponse;
import eu.nimble.service.bp.model.tt.OrderEPC;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.util.persistence.DataIntegratorUtil;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.formula.functions.T;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.portlet.MockClientDataRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import springfox.documentation.spring.web.json.Json;

import java.io.IOException;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by suat on 26-Jul-19.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class EPCControllerTest {
    private static String exampleCataloguePath = "/controller/example_catalogue.json";
    private static String catalogueLineId = "7a56d940-64e0-4619-a7e2-4d39b6682dd4";

    @Autowired
    private MockMvc mockMvc;

    @BeforeClass
    public static void addCatalogue() throws IOException {
        String inputMessageAsString = IOUtils.toString(EPCControllerTest.class.getResourceAsStream(exampleCataloguePath));
        CatalogueType catalogue = JsonSerializationUtility.getObjectMapper().readValue(inputMessageAsString, CatalogueType.class);
        DataIntegratorUtil.checkExistingParties(catalogue);
        EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(catalogue.getProviderParty().getPartyIdentification().get(0).getID());
        repositoryWrapper.updateEntityForPersistCases(catalogue);
    }

    @Test
    public void test01_getCatalogueLineForCodeTest() throws Exception {

        MockHttpServletRequestBuilder request = get("/t-t/catalogueline")
                .header("Authorization", TestConfig.responderPersonId)
                .param("epc", "2607191506");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
        CatalogueLineType line = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CatalogueLineType.class);

        Assert.assertEquals(line.getID(), catalogueLineId);
    }

    @Test
    public void test02_getCatalogueLineForCodeTest() throws Exception {

        MockHttpServletRequestBuilder request = get("/t-t/epc-codes")
                .header("Authorization", TestConfig.responderPersonId)
                .param("productId", "2506");
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> epcCodes = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<List<String>>() {});

        Assert.assertEquals(epcCodes.size(), 2);
    }
}
