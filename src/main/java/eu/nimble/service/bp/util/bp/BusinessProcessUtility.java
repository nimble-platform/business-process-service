package eu.nimble.service.bp.util.bp;

import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.util.camunda.CamundaEngine;
import eu.nimble.service.bp.swagger.model.Transaction;
import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 03-Jan-19.
 */
public class BusinessProcessUtility {

    public static Transaction.DocumentTypeEnum getInitialDocumentForProcess(String businessProcessType) {
        return CamundaEngine.getTransactions(businessProcessType).get(0).getDocumentType();
    }

    public static Transaction.DocumentTypeEnum getResponseDocumentForProcess(String businessProcessType) {
        return CamundaEngine.getTransactions(businessProcessType).get(1).getDocumentType();
    }

    public static List<Transaction.DocumentTypeEnum> getInitialDocumentsForAllProcesses() {
        return getSpecifiedDocumentsForAllProcesses(0);
    }

    public static List<Transaction.DocumentTypeEnum> getResponseDocumentsForAllProcesses() {
        return getSpecifiedDocumentsForAllProcesses(1);
    }

    public static boolean isInitialDocument(Class klass) {
        List<Transaction.DocumentTypeEnum> initialDocuments = getInitialDocumentsForAllProcesses();
        for(Transaction.DocumentTypeEnum documentType : initialDocuments) {
            if(klass.equals(DocumentEnumClassMapper.getDocumentClass(DocumentType.valueOf(documentType.toString())))) {
               return true;
            }
        }
        return false;
    }

    private static List<Transaction.DocumentTypeEnum> getSpecifiedDocumentsForAllProcesses(int index) {
        List<Transaction.DocumentTypeEnum> initialDocuments = new ArrayList<>();
        List<ProcessDefinition> processDefinitions = CamundaEngine.getCamundaProcessDefinitions();
        for (ProcessDefinition processDefinition : processDefinitions) {
            initialDocuments.add(CamundaEngine.getTransactions(processDefinition.getKey()).get(index).getDocumentType());
        }
        return initialDocuments;
    }
}
