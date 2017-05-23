/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.swagger.model.*;

import java.util.List;

/**
 * @author yildiray
 */
public class HibernateSwaggerObjectMapper {

    public static BusinessProcessDAO createBusinessProcess_DAO(BusinessProcess body) {
        BusinessProcessDAO businessProcessDAO = new BusinessProcessDAO();

        businessProcessDAO.setBusinessProcessID(body.getBusinessProcessID());
        businessProcessDAO.setBpmnContent(body.getBpmnContent());
        businessProcessDAO.setBusinessProcessName(body.getBusinessProcessName());
        BusinessProcessType businessProcessTypeDAO = BusinessProcessType.fromValue(body.getBusinessProcessType().toString());
        businessProcessDAO.setBusinessProcessType(businessProcessTypeDAO);

        return businessProcessDAO;
    }

    public static BusinessProcessApplicationConfigurationsDAO createBusinessProcessApplicationConfigurations_DAO(BusinessProcessApplicationConfigurations body) {
        BusinessProcessApplicationConfigurationsDAO businessProcessApplicationConfigurationsDAO = new BusinessProcessApplicationConfigurationsDAO();

        businessProcessApplicationConfigurationsDAO.setBusinessProcessID(body.getBusinessProcessID());
        businessProcessApplicationConfigurationsDAO.setPartnerID(body.getPartnerID());

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

            businessProcessApplicationConfigurationsDAO.getApplicationConfigurations().add(applicationConfigurationDAO);
        }

        return businessProcessApplicationConfigurationsDAO;
    }

    public static BusinessProcessPreferencesDAO createBusinessProcessPreferences_DAO(BusinessProcessPreferences body) {
        BusinessProcessPreferencesDAO businessProcessPreferencesDAO = new BusinessProcessPreferencesDAO();

        businessProcessPreferencesDAO.setPartnerID(body.getPartnerID());
        List<BusinessProcessPreference> preferences = body.getPreferences();
        for (BusinessProcessPreference preference : preferences) {
            BusinessProcessPreferenceDAO businessProcessPreferenceDAO = new BusinessProcessPreferenceDAO();

            businessProcessPreferenceDAO.setTargetPartnerID(preference.getTargetPartnerID());

            List<BusinessProcessPreference.ProcessOrderEnum> processOrder = preference.getProcessOrder();
            for (BusinessProcessPreference.ProcessOrderEnum processType : processOrder) {
                BusinessProcessType businessProcessType = BusinessProcessType.fromValue(processType.toString());
                businessProcessPreferenceDAO.getProcessOrder().add(businessProcessType);
            }
            businessProcessPreferencesDAO.getPreferences().add(businessProcessPreferenceDAO);
        }

        return businessProcessPreferencesDAO;
    }

    public static BusinessProcessInstanceInputMessageDAO createBusinessProcessInstanceInputMessage_DAO(BusinessProcessInstanceInputMessage body) {
        BusinessProcessInstanceInputMessageDAO businessProcessInstanceInputMessageDAO = new BusinessProcessInstanceInputMessageDAO();

        businessProcessInstanceInputMessageDAO.setBusinessProcessInstanceID(body.getBusinessProcessInstanceID());

        BusinessProcessVariablesDAO businessProcessVariablesDAO = new BusinessProcessVariablesDAO();
        businessProcessVariablesDAO.setBusinessProcessID(body.getVariables().getBusinessProcessID());
        businessProcessVariablesDAO.setContent(body.getVariables().getContent());
        businessProcessVariablesDAO.setInitiatorID(body.getVariables().getInitiatorID());
        businessProcessVariablesDAO.setResponderID(body.getVariables().getResponderID());

        businessProcessInstanceInputMessageDAO.setVariables(businessProcessVariablesDAO);
        return businessProcessInstanceInputMessageDAO;
    }

    public static BusinessProcess createBusinessProcess(BusinessProcessDAO businessProcessDAO) {
        BusinessProcess businessProcess = new BusinessProcess();
        businessProcess.setBpmnContent(businessProcessDAO.getBpmnContent());
        businessProcess.setBusinessProcessID(businessProcessDAO.getBusinessProcessID());
        businessProcess.setBusinessProcessName(businessProcessDAO.getBusinessProcessName());
        businessProcess.setBusinessProcessType(BusinessProcess.BusinessProcessTypeEnum.valueOf(businessProcessDAO.getBusinessProcessType().value()));
        return businessProcess;
    }

    public static BusinessProcessApplicationConfigurations createBusinessProcessApplicationConfigurations(BusinessProcessApplicationConfigurationsDAO businessProcessApplicationConfigurationsDAO) {
        BusinessProcessApplicationConfigurations businessProcessApplicationConfigurations = new BusinessProcessApplicationConfigurations();

        businessProcessApplicationConfigurations.setPartnerID(businessProcessApplicationConfigurationsDAO.getPartnerID());
        businessProcessApplicationConfigurations.setBusinessProcessID(businessProcessApplicationConfigurationsDAO.getBusinessProcessID());
        List<ApplicationConfigurationDAO> applicationConfigurationDAOS = businessProcessApplicationConfigurationsDAO.getApplicationConfigurations();
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

            businessProcessApplicationConfigurations.getApplicationConfigurations().add(applicationConfiguration);
        }

        return businessProcessApplicationConfigurations;
    }

    public static BusinessProcessPreferences createBusinessProcessPreferences(BusinessProcessPreferencesDAO businessProcessPreferencesDAO) {
        BusinessProcessPreferences businessProcessPreferences = new BusinessProcessPreferences();
        businessProcessPreferences.setPartnerID(businessProcessPreferencesDAO.getPartnerID());
        List<BusinessProcessPreferenceDAO> preferences = businessProcessPreferencesDAO.getPreferences();
        for (BusinessProcessPreferenceDAO preference : preferences) {
            BusinessProcessPreference businessProcessPreference = new BusinessProcessPreference();
            businessProcessPreference.setTargetPartnerID(preference.getTargetPartnerID());

            List<BusinessProcessType> processOrder = preference.getProcessOrder();
            for (BusinessProcessType process : processOrder) {
                businessProcessPreference.getProcessOrder().add(BusinessProcessPreference.ProcessOrderEnum.valueOf(process.value()));
            }

            businessProcessPreferences.getPreferences().add(businessProcessPreference);
        }

        return businessProcessPreferences;
    }

    public static BusinessProcessDocument createBusinessProcessDocument(BusinessProcessDocumentDAO businessProcessDocumentDAO) {
        BusinessProcessDocument businessProcessDocument = new BusinessProcessDocument();
        businessProcessDocument.setBusinessProcessInstanceID(businessProcessDocumentDAO.getBusinessProcessInstanceID());
        businessProcessDocument.setContent(businessProcessDocumentDAO.getContent());
        businessProcessDocument.setDocumentID(businessProcessDocumentDAO.getDocumentID());
        businessProcessDocument.setInitiatorID(businessProcessDocumentDAO.getInitiatorID());
        businessProcessDocument.setResponderID(businessProcessDocumentDAO.getResponderID());
        businessProcessDocument.setSubmissionDate(businessProcessDocumentDAO.getSubmissionDate());
        businessProcessDocument.setStatus(BusinessProcessDocument.StatusEnum.valueOf(businessProcessDocumentDAO.getStatus().value()));
        businessProcessDocument.setType(BusinessProcessDocument.TypeEnum.valueOf(businessProcessDocumentDAO.getType().value()));
        return businessProcessDocument;
    }


    public static BusinessProcessInstanceDAO createBusinessProcessInstance_DAO(BusinessProcessInstance businessProcessInstance) {
        BusinessProcessInstanceDAO businessProcessInstanceDAO = new BusinessProcessInstanceDAO();
        businessProcessInstanceDAO.setBusinessProcessID(businessProcessInstance.getBusinessProcessID());
        businessProcessInstanceDAO.setBusinessProcessInstanceID(businessProcessInstance.getBusinessProcessInstanceID());
        businessProcessInstanceDAO.setStatus(BusinessProcessInstanceStatus.fromValue(businessProcessInstance.getStatus().toString()));
        return businessProcessInstanceDAO;
    }
}
