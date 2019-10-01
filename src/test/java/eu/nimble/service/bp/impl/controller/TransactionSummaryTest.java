package eu.nimble.service.bp.impl.controller;

import eu.nimble.service.bp.model.export.TransactionSummary;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
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

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class TransactionSummaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getTransactionSummaries() throws Exception {
        List<TransactionSummary> transactionSummaries = ProcessDocumentMetadataDAOUtility.getTransactionSummaries("747","745","incoming",false,TestConfig.buyerPartyID);
        Assert.assertEquals(3,transactionSummaries.size());
        transactionSummaries = ProcessDocumentMetadataDAOUtility.getTransactionSummaries("1339",null,"outgoing",null,TestConfig.buyerPartyID);
        Assert.assertEquals(16,transactionSummaries.size());
    }

}
