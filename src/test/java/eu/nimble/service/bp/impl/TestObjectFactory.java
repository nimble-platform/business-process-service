package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.swagger.model.*;
import eu.nimble.service.bp.swagger.model.Process;

/**
 * Created by yildiray on 5/24/2017.
 */
public class TestObjectFactory {
    private static String processID="OrderTest";
    private static String partnerID="buyer1387";
    private static String documentID="document1387";
    private static String documentSource="SENT";
    private static String documentStatus="WAITINGRESPONSE";
    private static String documentType="ORDER";
    private static String processInstanceID="orderProcessInstance1387";

    public static Process createProcess() {
        Process process = new Process();
        process.setProcessName("order");
        process.setProcessID(processID);
        process.setBpmnContent("<BPMN></BPMN>");
        process.setProcessType(Process.ProcessTypeEnum.valueOf(documentType));
        return process;
    }

    public static Process updateProcess() {
        Process process = createProcess();
        process.setBpmnContent("<BPMN>1387</BPMN>");
        return process;
    }

    public static ProcessApplicationConfigurations createProcessApplicationConfigurations() {
        ProcessApplicationConfigurations configurations = new ProcessApplicationConfigurations();
        configurations.setPartnerID(partnerID);
        configurations.setProcessID(processID);

        ExecutionConfiguration execution = new ExecutionConfiguration();
        execution.setType(ExecutionConfiguration.TypeEnum.JAVA);
        execution.setURI("eu.nimble.service.EndUserDatabaseService");

        ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setExecution(execution);
        configuration.setType(ApplicationConfiguration.TypeEnum.DATAADAPTER);
        configuration.setTransactionName("Create Order");
        configuration.setName("Create Order");
        configuration.setActivityID("Create Order 1387");

        configurations.getApplicationConfigurations().add(configuration);

        execution = new ExecutionConfiguration();
        execution.setType(ExecutionConfiguration.TypeEnum.JAVA);
        execution.setURI("eu.nimble.service.EndUserChannelService");

        configuration = new ApplicationConfiguration();
        configuration.setExecution(execution);
        configuration.setType(ApplicationConfiguration.TypeEnum.DATACHANNEL);
        configuration.setTransactionName("Send Order");
        configuration.setName("Send Order");
        configuration.setActivityID("Send Order 1387");

        configurations.getApplicationConfigurations().add(configuration);

        return configurations;
    }

    public static ProcessApplicationConfigurations updateProcessApplicationConfigurations() {
        ProcessApplicationConfigurations configurations = createProcessApplicationConfigurations();
        configurations.getApplicationConfigurations().get(0).setActivityID("Create Order 1388");

        return configurations;
    }

    public static ProcessPreferences createProcessPreferences() {
        ProcessPreferences processPreferences = new ProcessPreferences();
        processPreferences.setPartnerID(partnerID);

        ProcessPreference preference = new ProcessPreference();
        preference.setTargetPartnerID("DEFAULT");
        preference.getProcessOrder().add(ProcessPreference.ProcessOrderEnum.CATALOGUE);
        preference.getProcessOrder().add(ProcessPreference.ProcessOrderEnum.ORDER);
        preference.getProcessOrder().add(ProcessPreference.ProcessOrderEnum.INVOICE);
        processPreferences.getPreferences().add(preference);

        preference = new ProcessPreference();
        preference.setTargetPartnerID("seller1387");
        preference.getProcessOrder().add(ProcessPreference.ProcessOrderEnum.CATALOGUE);
        preference.getProcessOrder().add(ProcessPreference.ProcessOrderEnum.NEGOTIATION);
        preference.getProcessOrder().add(ProcessPreference.ProcessOrderEnum.ORDER);
        preference.getProcessOrder().add(ProcessPreference.ProcessOrderEnum.TRACKING);
        preference.getProcessOrder().add(ProcessPreference.ProcessOrderEnum.FULFILLMENT);
        preference.getProcessOrder().add(ProcessPreference.ProcessOrderEnum.INVOICE);
        processPreferences.getPreferences().add(preference);


        return processPreferences;
    }

    public static ProcessPreferences updateProcessPreferences() {
        ProcessPreferences processPreferences = createProcessPreferences();
        processPreferences.getPreferences().get(1).setTargetPartnerID("seller1388");

        return processPreferences;
    }

    public static ProcessInstanceInputMessage createStartProcessInstanceInputMessage() {
        ProcessInstanceInputMessage inputMessage = new ProcessInstanceInputMessage();
        inputMessage.setProcessInstanceID(processInstanceID);

        ProcessVariables variables = new ProcessVariables();
        variables.setProcessID(processID);
        variables.setContent("JSON Content");
        variables.setInitiatorID(partnerID);
        variables.setResponderID("seller1387");

        inputMessage.setVariables(variables);

        return inputMessage;
    }

    public static ProcessInstanceInputMessage createContinueProcessInstanceInputMessage() {
        ProcessInstanceInputMessage inputMessage = createStartProcessInstanceInputMessage();
        inputMessage.getVariables().setInitiatorID("seller1387");
        inputMessage.getVariables().setResponderID(partnerID);

        return inputMessage;
    }

    public static ProcessDocument createBusinessDocument() {
        ProcessDocument processDocument = new ProcessDocument();
        processDocument.setType(ProcessDocument.TypeEnum.valueOf(documentType));
        processDocument.setStatus(ProcessDocument.StatusEnum.valueOf(documentStatus));
        processDocument.setDocumentID(documentID);
        processDocument.setSubmissionDate("2017-05-23");
        processDocument.setInitiatorID(partnerID);
        processDocument.setResponderID("seller1387");
        processDocument.setProcessInstanceID(processInstanceID);
        processDocument.setContent("<Order></Order>");
        return processDocument;
    }

    public static ProcessDocument updateBusinessDocument() {
        ProcessDocument processDocument = createBusinessDocument();
        processDocument.setContent("<Order>1387</Order>");
        return processDocument;
    }

    public static String getProcessID() {
        return processID;
    }

    public static String getPartnerID() {
        return partnerID;
    }

    public static String getDocumentID() {
        return documentID;
    }

    public static String getDocumentSource() {
        return documentSource;
    }

    public static String getDocumentStatus() {
        return documentStatus;
    }

    public static String getDocumentType() {
        return documentType;
    }
}
