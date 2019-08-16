package eu.nimble.service.bp.impl.controller;

/**
 * Created by dogukan on 16.07.2018.
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.contract.FrameContractService;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemIdentificationType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.digitalagreement.DigitalAgreementType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.joda.time.DateTime;
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

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class FrameContractControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FrameContractService frameContractService;

    private static String itemId = "itemId";

    private ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
    private static DigitalAgreementType frameContract;

    /**
     * Test scenario:
     * 1) Create and retrieve frame contract via its identifier
     * 2) Retrieve frame contract via participant companies and participant id
     * 3) Retrieve all frame contracts of a party
     * 4) Retrieve an expired frame contract (response is supposed to be 404)
     * 5) Update and existing expired frame contract and retrieve it (response is supposed to be not-null)
     */

    @Test
    public void test1_createAndGetFrameContract() throws Exception {
        // create frame contract
        ItemType item = new ItemType();
        ItemIdentificationType id = new ItemIdentificationType();
        id.setID(itemId);
        item.setManufacturersItemIdentification(id);

        QuantityType duration = new QuantityType();
        duration.setValue(new BigDecimal(3));
        duration.setUnitCode("month(s)");

        DigitalAgreementType frameContract = frameContractService.createDigitalAgreement(TestConfig.sellerPartyID, TestConfig.buyerPartyID, item, duration, "quotationId");

        // retrieve contract
        MockHttpServletRequestBuilder request = get("/contract/digital-agreement/" + frameContract.getHjid())
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        this.frameContract = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DigitalAgreementType.class);

        Assert.assertEquals(frameContract.getHjid(), this.frameContract.getHjid());
    }

    @Test
    public void test2_getDigitalAgreementForPartiesAndProduct() throws Exception {
        MockHttpServletRequestBuilder request = get("/contract/digital-agreement")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("buyerId",TestConfig.buyerPartyID)
                .param("sellerId",TestConfig.sellerPartyID)
                .param("productId",itemId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        DigitalAgreementType fc =  objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DigitalAgreementType.class);
        Assert.assertEquals(frameContract.getHjid(),fc.getHjid());
    }

    @Test
    public void test3_getDigitalAgreementsForParty() throws Exception {
        MockHttpServletRequestBuilder request = get("/contract/digital-agreement/all")
                .header("Authorization", TestConfig.initiatorPersonId)
                .param("partyId",TestConfig.sellerPartyID);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        List<DigitalAgreementType> frameContracts = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),new TypeReference<List<DigitalAgreementType>>(){});
        Assert.assertEquals(1,frameContracts.size());
        Assert.assertEquals(frameContract.getHjid(),frameContracts.get(0).getHjid());
    }

    @Test
    public void test4_expiredFrameContractTest() throws Exception {
        // pull the start and end dates of the contract earlier than the current date
        DateTime start = DateTime.now().minusMonths(5);
        DateTime end = start.minusMonths(2);
        XMLGregorianCalendar[] updatedDates = FrameContractService.transformJodaDatesToXMLGregorian(start, end);

        frameContract.getDigitalAgreementTerms().getValidityPeriod().setStartDate(updatedDates[0]);
        frameContract.getDigitalAgreementTerms().getValidityPeriod().setEndDate(updatedDates[1]);
        frameContract = new JPARepositoryFactory().forCatalogueRepository().updateEntity(frameContract);

        MockHttpServletRequestBuilder request = get("/contract/digital-agreement?buyerId=" + TestConfig.buyerPartyID + "&sellerId=" + TestConfig.sellerPartyID + "&productId=" + itemId)
                .header("Authorization", TestConfig.initiatorPersonId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    @Test
    public void test5_updateExpiredContractNotFoundTest() throws Exception {
        // retrieve contract
        MockHttpServletRequestBuilder request = get("/contract/digital-agreement/" + frameContract.getHjid())
                .header("Authorization", TestConfig.initiatorPersonId);
        MvcResult mvcResult = this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
        frameContract = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DigitalAgreementType.class);

        QuantityType duration = new QuantityType();
        duration.setValue(new BigDecimal(3));
        duration.setUnitCode("month(s)");

        frameContractService.createOrUpdateFrameContract(TestConfig.sellerPartyID, TestConfig.buyerPartyID, frameContract.getItem(), duration, "quotationId2");

        request = get("/contract/digital-agreement?buyerId=" + TestConfig.buyerPartyID + "&sellerId=" + TestConfig.sellerPartyID + "&productId=" + itemId)
                .header("Authorization", TestConfig.initiatorPersonId);
        this.mockMvc.perform(request).andDo(print()).andExpect(status().isOk()).andReturn();
    }
}
