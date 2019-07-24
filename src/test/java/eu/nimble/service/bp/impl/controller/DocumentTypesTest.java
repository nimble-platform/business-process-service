package eu.nimble.service.bp.impl.controller;

import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.util.bp.DocumentEnumClassMapper;
import eu.nimble.service.bp.util.camunda.CamundaEngine;
import eu.nimble.service.bp.swagger.model.Process;
import eu.nimble.service.bp.swagger.model.Transaction;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by suat on 03-Jan-19.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringJUnit4ClassRunner.class)
public class DocumentTypesTest {

    /**
     * Checks whether all the documents types included in the business processes are resolvable i.e. a Class can be inferred
     * given a {@link eu.nimble.service.bp.swagger.model.Transaction.DocumentTypeEnum}
     *
     * @throws Exception
     */
    @Test
    public void test01_resolveDocumentTypes() throws Exception {
        List<Process> processDefinitions = CamundaEngine.getProcessDefinitions();
        for (Process process : processDefinitions) {
            List<Transaction> transactions = process.getTransactions();
            for (Transaction transaction : transactions) {
                Class messageClass = DocumentEnumClassMapper.getDocumentClass(DocumentType.valueOf(transaction.getDocumentType().toString()));
                Assert.assertNotNull(messageClass);
            }
        }
    }

    /**
     * This test ensures that all the messages exchanged in the business processes has an {@code id} field of
     * {@link String} type.
     *
     * @throws Exception
     */
    @Test
    public void test02_idFieldsInDocumentMessages() throws Exception {
        List<Process> processDefinitions = CamundaEngine.getProcessDefinitions();
        for (Process process : processDefinitions) {
            List<Transaction> transactions = process.getTransactions();
            for (Transaction transaction : transactions) {
                Class messageClass = DocumentEnumClassMapper.getDocumentClass(DocumentType.valueOf(transaction.getDocumentType().toString()));
                try {
                    Field f = messageClass.getDeclaredField("id");
                    Assert.assertEquals("id field is not type of String", String.class, f.getType());
                } catch (Exception e) {
                    Assert.assertTrue("Non-existence if field for " + messageClass, false);
                }
            }
        }
    }
}
