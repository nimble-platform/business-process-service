/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util;

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
            applicationConfigurationDAO.setActivityID(applicationConfiguration.getActivityID());
            applicationConfigurationDAO.setName(applicationConfiguration.getName());
            applicationConfigurationDAO.setType(ApplicationType.fromValue(applicationConfiguration.getType().toString()));
            applicationConfigurationDAO.setTransactionName(applicationConfiguration.getTransactionName());

            ExecutionConfigurationDAO executionConfigurationDAO = new ExecutionConfigurationDAO();
            executionConfigurationDAO.setType(ApplicationExecutionType.fromValue(applicationConfiguration.getExecution().getType().toString()));
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
        processVariablesDAO.setContent(body.getVariables().getContent());
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
            ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
            applicationConfiguration.setActivityID(applicationConfigurationDAO.getActivityID());
            applicationConfiguration.setName(applicationConfigurationDAO.getName());
            applicationConfiguration.setTransactionName(applicationConfigurationDAO.getTransactionName());
            applicationConfiguration.setType(ApplicationConfiguration.TypeEnum.valueOf(applicationConfigurationDAO.getType().value()));

            ExecutionConfiguration executionConfiguration = new ExecutionConfiguration();
            executionConfiguration.setURI(applicationConfigurationDAO.getExecution().getURI());
            executionConfiguration.setType(ExecutionConfiguration.TypeEnum.valueOf(applicationConfigurationDAO.getExecution().getType().value()));

            applicationConfiguration.setExecution(executionConfiguration);

            processApplicationConfigurations.getApplicationConfigurations().add(applicationConfiguration);
        }

        return processApplicationConfigurations;
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

    public static ProcessDocument createProcessDocument(ProcessDocumentDAO processDocumentDAO) {
        ProcessDocument processDocument = new ProcessDocument();
        processDocument.setProcessInstanceID(processDocumentDAO.getProcessInstanceID());
        processDocument.setContent(processDocumentDAO.getContent());
        processDocument.setDocumentID(processDocumentDAO.getDocumentID());
        processDocument.setInitiatorID(processDocumentDAO.getInitiatorID());
        processDocument.setResponderID(processDocumentDAO.getResponderID());
        processDocument.setSubmissionDate(processDocumentDAO.getSubmissionDate());
        processDocument.setStatus(ProcessDocument.StatusEnum.valueOf(processDocumentDAO.getStatus().value()));
        processDocument.setType(ProcessDocument.TypeEnum.valueOf(processDocumentDAO.getType().value()));
        return processDocument;
    }


    public static ProcessInstanceDAO createProcessInstance_DAO(ProcessInstance processInstance) {
        ProcessInstanceDAO processInstanceDAO = new ProcessInstanceDAO();
        processInstanceDAO.setProcessID(processInstance.getProcessID());
        processInstanceDAO.setProcessInstanceID(processInstance.getProcessInstanceID());
        processInstanceDAO.setStatus(ProcessInstanceStatus.fromValue(processInstance.getStatus().toString()));
        return processInstanceDAO;
    }

    public static ProcessDocumentDAO createProcessDocument_DAO(ProcessDocument body) {
        ProcessDocumentDAO processDocumentDAO = new ProcessDocumentDAO();
        processDocumentDAO.setProcessInstanceID(body.getProcessInstanceID());
        processDocumentDAO.setContent(body.getContent());
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
}
