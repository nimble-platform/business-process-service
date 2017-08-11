/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.swagger.model.*;
import eu.nimble.service.bp.swagger.model.Process;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * @author yildiray
 */
public class HibernateSwaggerObjectMapper {

    public static ProcessApplicationConfigurationsDAO createProcessApplicationConfigurations_DAO(ProcessApplicationConfigurations body) {
        ProcessApplicationConfigurationsDAO processApplicationConfigurationsDAO = new ProcessApplicationConfigurationsDAO();

        processApplicationConfigurationsDAO.setProcessID(body.getProcessID());
        processApplicationConfigurationsDAO.setPartnerID(body.getPartnerID());

        List<ApplicationConfiguration> applicationConfigurations = body.getApplicationConfigurations();
        for (ApplicationConfiguration applicationConfiguration : applicationConfigurations) {
            ApplicationConfigurationDAO applicationConfigurationDAO = new ApplicationConfigurationDAO();
            applicationConfigurationDAO.setRoleType(RoleType.fromValue(applicationConfiguration.getRoleType().toString()));
            applicationConfigurationDAO.setApplicationType(ApplicationType.fromValue(applicationConfiguration.getApplicationType().toString()));
            applicationConfigurationDAO.setTransactionID(applicationConfiguration.getTransactionID());

            ExecutionConfigurationDAO executionConfigurationDAO = new ExecutionConfigurationDAO();
            executionConfigurationDAO.setExecutionType(ApplicationExecutionType.fromValue(applicationConfiguration.getExecution().getExecutionType().toString()));
            executionConfigurationDAO.setURI(applicationConfiguration.getExecution().getURI());
            applicationConfigurationDAO.setExecution(executionConfigurationDAO);

            processApplicationConfigurationsDAO.getApplicationConfigurations().add(applicationConfigurationDAO);
        }

        return processApplicationConfigurationsDAO;
    }

    public static ProcessPreferencesDAO createProcessPreferences_DAO(ProcessPreferences body) {
        ProcessPreferencesDAO processPreferencesDAO = new ProcessPreferencesDAO();

        processPreferencesDAO.setPartnerID(body.getPartnerID());
        List<ProcessPreference> preferences = body.getPreferences();
        for (ProcessPreference preference : preferences) {
            ProcessPreferenceDAO processPreferenceDAO = new ProcessPreferenceDAO();

            processPreferenceDAO.setTargetPartnerID(preference.getTargetPartnerID());

            List<ProcessPreference.ProcessOrderEnum> processOrder = preference.getProcessOrder();
            for (ProcessPreference.ProcessOrderEnum businessProcessType : processOrder) {
                ProcessType processType = ProcessType.fromValue(businessProcessType.toString());
                processPreferenceDAO.getProcessOrder().add(processType);
            }
            processPreferencesDAO.getPreferences().add(processPreferenceDAO);
        }

        return processPreferencesDAO;
    }

    public static ProcessInstanceInputMessageDAO createProcessInstanceInputMessage_DAO(ProcessInstanceInputMessage body) {
        ProcessInstanceInputMessageDAO processInstanceInputMessageDAO = new ProcessInstanceInputMessageDAO();

        processInstanceInputMessageDAO.setProcessInstanceID(body.getProcessInstanceID());

        ProcessVariablesDAO processVariablesDAO = new ProcessVariablesDAO();
        processVariablesDAO.setProcessID(body.getVariables().getProcessID());
        processVariablesDAO.setContentUUID(body.getVariables().getContentUUID());
        processVariablesDAO.setInitiatorID(body.getVariables().getInitiatorID());
        processVariablesDAO.setResponderID(body.getVariables().getResponderID());

        processInstanceInputMessageDAO.setVariables(processVariablesDAO);
        return processInstanceInputMessageDAO;
    }

    public static ProcessApplicationConfigurations createProcessApplicationConfigurations(ProcessApplicationConfigurationsDAO processApplicationConfigurationsDAO) {
        ProcessApplicationConfigurations processApplicationConfigurations = new ProcessApplicationConfigurations();

        processApplicationConfigurations.setPartnerID(processApplicationConfigurationsDAO.getPartnerID());
        processApplicationConfigurations.setProcessID(processApplicationConfigurationsDAO.getProcessID());
        List<ApplicationConfigurationDAO> applicationConfigurationDAOS = processApplicationConfigurationsDAO.getApplicationConfigurations();
        for (ApplicationConfigurationDAO applicationConfigurationDAO : applicationConfigurationDAOS) {
            ApplicationConfiguration applicationConfiguration = createApplicationConfiguration(applicationConfigurationDAO);
            processApplicationConfigurations.getApplicationConfigurations().add(applicationConfiguration);
        }

        return processApplicationConfigurations;
    }

    public static ApplicationConfiguration createApplicationConfiguration(ApplicationConfigurationDAO applicationConfigurationDAO) {
        ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
        applicationConfiguration.setRoleType(ApplicationConfiguration.RoleTypeEnum.valueOf(applicationConfigurationDAO.getRoleType().value()));
        applicationConfiguration.setTransactionID(applicationConfigurationDAO.getTransactionID());
        applicationConfiguration.setApplicationType(ApplicationConfiguration.ApplicationTypeEnum.valueOf(applicationConfigurationDAO.getApplicationType().value()));

        ExecutionConfiguration executionConfiguration = new ExecutionConfiguration();
        executionConfiguration.setURI(applicationConfigurationDAO.getExecution().getURI());
        executionConfiguration.setExecutionType(ExecutionConfiguration.ExecutionTypeEnum.valueOf(applicationConfigurationDAO.getExecution().getExecutionType().value()));

        applicationConfiguration.setExecution(executionConfiguration);

        return applicationConfiguration;
    }

    public static ProcessPreferences createProcessPreferences(ProcessPreferencesDAO processPreferencesDAO) {
        ProcessPreferences processPreferences = new ProcessPreferences();
        processPreferences.setPartnerID(processPreferencesDAO.getPartnerID());
        List<ProcessPreferenceDAO> preferences = processPreferencesDAO.getPreferences();
        for (ProcessPreferenceDAO preference : preferences) {
            ProcessPreference processPreference = new ProcessPreference();
            processPreference.setTargetPartnerID(preference.getTargetPartnerID());

            List<ProcessType> processOrder = preference.getProcessOrder();
            for (ProcessType process : processOrder) {
                processPreference.getProcessOrder().add(ProcessPreference.ProcessOrderEnum.valueOf(process.value()));
            }

            processPreferences.getPreferences().add(processPreference);
        }

        return processPreferences;
    }

    public static ProcessDocumentMetadata createProcessDocumentMetadata(ProcessDocumentMetadataDAO processDocumentDAO) {
        ProcessDocumentMetadata processDocument = new ProcessDocumentMetadata();
        processDocument.setProcessInstanceID(processDocumentDAO.getProcessInstanceID());
        processDocument.setDocumentID(processDocumentDAO.getDocumentID());
        processDocument.setInitiatorID(processDocumentDAO.getInitiatorID());
        processDocument.setResponderID(processDocumentDAO.getResponderID());
        processDocument.setSubmissionDate(processDocumentDAO.getSubmissionDate());
        processDocument.setStatus(ProcessDocumentMetadata.StatusEnum.valueOf(processDocumentDAO.getStatus().value()));
        processDocument.setType(ProcessDocumentMetadata.TypeEnum.valueOf(processDocumentDAO.getType().value()));
        return processDocument;
    }


    public static ProcessInstanceDAO createProcessInstance_DAO(ProcessInstance processInstance) {
        ProcessInstanceDAO processInstanceDAO = new ProcessInstanceDAO();
        processInstanceDAO.setProcessID(processInstance.getProcessID());
        processInstanceDAO.setProcessInstanceID(processInstance.getProcessInstanceID());
        processInstanceDAO.setStatus(ProcessInstanceStatus.fromValue(processInstance.getStatus().toString()));
        return processInstanceDAO;
    }

    public static ProcessDocumentMetadataDAO createProcessDocumentMetadata_DAO(ProcessDocumentMetadata body) {
        ProcessDocumentMetadataDAO processDocumentDAO = new ProcessDocumentMetadataDAO();
        processDocumentDAO.setProcessInstanceID(body.getProcessInstanceID());
        processDocumentDAO.setInitiatorID(body.getInitiatorID());
        processDocumentDAO.setResponderID(body.getResponderID());
        processDocumentDAO.setStatus(ProcessDocumentStatus.fromValue(body.getStatus().toString()));
        processDocumentDAO.setDocumentID(body.getDocumentID());
        processDocumentDAO.setType(DocumentType.fromValue(body.getType().toString()));
        processDocumentDAO.setSubmissionDate(body.getSubmissionDate());
        return processDocumentDAO;
    }

    public static ResponseEntity<ModelApiResponse> getApiResponse() {
        ModelApiResponse apiResponse = new ModelApiResponse();
        apiResponse.setType("SUCCESS");
        apiResponse.setMessage("Successful operation");
        apiResponse.setCode(200);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    public static ProcessPreferences createDefaultProcessPreferences() {
        ProcessPreferences preferences = new ProcessPreferences();

        ProcessPreference preference = new ProcessPreference();
        preference.setTargetPartnerID("DEFAULT");
        preference.addProcessOrderItem(ProcessPreference.ProcessOrderEnum.PRODUCTCONFIGURATION);
        preference.addProcessOrderItem(ProcessPreference.ProcessOrderEnum.CATALOGUE);
        preference.addProcessOrderItem(ProcessPreference.ProcessOrderEnum.NEGOTIATION);
        preference.addProcessOrderItem(ProcessPreference.ProcessOrderEnum.ORDER);
        preference.addProcessOrderItem(ProcessPreference.ProcessOrderEnum.INVOICE);
        preference.addProcessOrderItem(ProcessPreference.ProcessOrderEnum.REMITTANCEADVICE);
        preference.addProcessOrderItem(ProcessPreference.ProcessOrderEnum.FULFILLMENT);
        preference.addProcessOrderItem(ProcessPreference.ProcessOrderEnum.TRACKING);

        preferences.addPreferencesItem(preference);
        return null;
    }

    public static ProcessDAO createProcess_DAO(Process body) {
        ProcessDAO processDAO = new ProcessDAO();
        processDAO.setProcessID(body.getProcessID());
        processDAO.setProcessName(body.getProcessName());
        processDAO.setTextContent(body.getTextContent());
        processDAO.setProcessType(ProcessType.fromValue(body.getProcessType().toString()));

        List<Transaction> transactions = body.getTransactions();
        for(Transaction transaction: transactions) {
            TransactionDAO transactionDAO = new TransactionDAO();
            transactionDAO.setInitiatorRole(RoleType.fromValue(transaction.getInitiatorRole().toString()));
            transactionDAO.setResponderRole(RoleType.fromValue(transaction.getResponderRole().toString()));
            transactionDAO.setTransactionID(transaction.getTransactionID());
            transactionDAO.setDocumentType(DocumentType.fromValue(transaction.getDocumentType().toString()));
            processDAO.getTransactions().add(transactionDAO);
        }

        return processDAO;
    }

    public static Process createProcess(ProcessDAO processDAO) {
        Process process = new Process();
        process.setProcessID(processDAO.getProcessID());
        process.setProcessName(processDAO.getProcessName());
        process.setTextContent(processDAO.getTextContent());
        process.setProcessType(Process.ProcessTypeEnum.valueOf(processDAO.getProcessType().value()));
        process.setBpmnContent("");

        List<TransactionDAO> transactionsDAO = processDAO.getTransactions();
        for(TransactionDAO transactionDAO: transactionsDAO) {
            Transaction transaction = new Transaction();
            transaction.setInitiatorRole(Transaction.InitiatorRoleEnum.valueOf(transactionDAO.getInitiatorRole().value()));
            transaction.setResponderRole(Transaction.ResponderRoleEnum.valueOf(transactionDAO.getResponderRole().value()));
            transaction.setTransactionID(transactionDAO.getTransactionID());
            transaction.setDocumentType(Transaction.DocumentTypeEnum.valueOf(transactionDAO.getDocumentType().value()));
            process.getTransactions().add(transaction);
        }

        return process;
    }
}
