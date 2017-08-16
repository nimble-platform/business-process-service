package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.swagger.model.*;
import eu.nimble.service.bp.swagger.model.Process;

/**
 * Created by yildiray on 5/24/2017.
 */
public class TestObjectFactory {
    private static String processID="OrderTest";
    private static String processName="OrderTest";
    private static String partnerID="buyer1387";
    private static String documentID="document1387";
    private static String documentSource="SENT";
    private static String documentStatus="WAITINGRESPONSE";
    private static String documentType="ORDER";
    private static String processInstanceID="orderProcessInstance1387";

    public static Process createOrderProcess() {
        Process process = new Process();
        process.setProcessName(processName);
        process.setProcessID(processID);
        process.setBpmnContent(getOrderBPMNContent());
        process.setTextContent("Title: ORDER\\n\" +\n" +
                "                \"Buyer -> Seller: Order\\n\" +\n" +
                "                \"Note right of Seller: Evaluate Order\\n\" +\n" +
                "                \"Seller -> Buyer: Order Response");
        process.setProcessType(Process.ProcessTypeEnum.ORDER);
        return process;
    }

    public static Process createNegotiationProcess() {
        Process process = new Process();
        process.setProcessName("NegotiationTest");
        process.setProcessID("NegotiationTest");
        process.setBpmnContent(getNegotiationBPMNContent());
        process.setTextContent("Title: NEGOTIATION\\n\" +\n" +
                "                \"Buyer -> Seller: Request For Quotation\\n\" +\n" +
                "                \"Note right of Seller: Evaluate RfQ\\n\" +\n" +
                "                \"Seller -> Buyer: Quotation");
        process.setProcessType(Process.ProcessTypeEnum.NEGOTIATION);
        return process;
    }

    public static Process updateProcess() {
        Process process = createOrderProcess();
        return process;
    }

    public static ProcessConfiguration createProcessConfiguration() {
        ProcessConfiguration configurations = new ProcessConfiguration();
        configurations.setPartnerID(partnerID);
        configurations.setProcessID(processID);
        configurations.setRoleType(ProcessConfiguration.RoleTypeEnum.BUYER);

        ExecutionConfiguration execution = new ExecutionConfiguration();
        execution.setExecutionType(ExecutionConfiguration.ExecutionTypeEnum.JAVA);
        execution.setExecutionUri("eu.nimble.service.EndUserDatabaseService");
        execution.setApplicationType(ExecutionConfiguration.ApplicationTypeEnum.DATAADAPTER);

        TransactionConfiguration configuration = new TransactionConfiguration();
        configuration.getExecutionConfigurations().add(execution);

        configuration.setTransactionID("Create Order");
        configurations.getTransactionConfigurations().add(configuration);

        execution = new ExecutionConfiguration();
        execution.setExecutionType(ExecutionConfiguration.ExecutionTypeEnum.JAVA);
        execution.setExecutionUri("eu.nimble.service.EndUserChannelService");
        execution.setApplicationType(ExecutionConfiguration.ApplicationTypeEnum.DATACHANNEL);

        configuration = new TransactionConfiguration();
        configuration.getExecutionConfigurations().add(execution);
        configuration.setTransactionID("Send Order");

        configurations.getTransactionConfigurations().add(configuration);

        return configurations;
    }

    public static ProcessConfiguration updateProcessConfiguration() {
        ProcessConfiguration configurations = createProcessConfiguration();
        configurations.setRoleType(ProcessConfiguration.RoleTypeEnum.SELLER);

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

    public static ProcessDocumentMetadata createBusinessDocumentMetadata() {
        ProcessDocumentMetadata processDocument = new ProcessDocumentMetadata();
        processDocument.setType(ProcessDocumentMetadata.TypeEnum.valueOf(documentType));
        processDocument.setStatus(ProcessDocumentMetadata.StatusEnum.valueOf(documentStatus));
        processDocument.setDocumentID(documentID);
        processDocument.setSubmissionDate("2017-05-23");
        processDocument.setInitiatorID(partnerID);
        processDocument.setResponderID("seller1387");
        processDocument.setProcessInstanceID(processInstanceID);
        return processDocument;
    }

    public static ProcessDocumentMetadata updateBusinessDocumentMetadata() {
        ProcessDocumentMetadata processDocument = createBusinessDocumentMetadata();
        processDocument.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
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

    private static String getNegotiationBPMNContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\" exporterVersion=\"1.7.2\"> \n" +
                " <bpmn:process id=\"NegotiationTest\" name=\"NegotiationTest\" isExecutable=\"true\"> \n" +
                "  <bpmn:extensionElements> \n" +
                "   <camunda:properties> \n" +
                "    <camunda:property name=\"businessProcessCategory\" value=\"NEGOTIATION\" /> \n" +
                "   </camunda:properties> \n" +
                "  </bpmn:extensionElements>   \n" +
                "  <bpmn:startEvent id=\"StartEvent_1\"> \n" +
                "   <bpmn:outgoing>SequenceFlow_1foezn2</bpmn:outgoing> \n" +
                "  </bpmn:startEvent> \n" +
                "  <bpmn:sequenceFlow id=\"SequenceFlow_1foezn2\" sourceRef=\"StartEvent_1\" targetRef=\"Task_RFQToSeller\" /> \n" +
                "  <bpmn:sequenceFlow id=\"SequenceFlow_1hpgfmq\" sourceRef=\"Task_RFQToSeller\" targetRef=\"Task_EvaluateRFQ\" /> \n" +
                "  <bpmn:sequenceFlow id=\"SequenceFlow_1rfxe09\" sourceRef=\"Task_EvaluateRFQ\" targetRef=\"Task_QuotationToBuyer\" /> \n" +
                "  <bpmn:endEvent id=\"EndEvent_0mvpfgi\"> \n" +
                "   <bpmn:incoming>SequenceFlow_0am3t90</bpmn:incoming> \n" +
                "  </bpmn:endEvent> \n" +
                "  <bpmn:sequenceFlow id=\"SequenceFlow_0am3t90\" sourceRef=\"Task_QuotationToBuyer\" targetRef=\"EndEvent_0mvpfgi\" /> \n" +
                "  <bpmn:serviceTask id=\"Task_RFQToSeller\" name=\"RFQ to Seller\" camunda:class=\"eu.nimble.service.bp.processor.test.TestProcessor\"> \n" +
                "   <bpmn:extensionElements> \n" +
                "    <camunda:inputOutput> \n" +
                "     <camunda:inputParameter name=\"buyerID\"><![CDATA[${initiatorID} \n" +
                "      ]]></camunda:inputParameter> \n" +
                "     <camunda:inputParameter name=\"sellerID\"><![CDATA[${responderID} \n" +
                "      ]]></camunda:inputParameter> \n" +
                "     <camunda:inputParameter name=\"rfqXML\"><![CDATA[${content} \n" +
                "      ]]></camunda:inputParameter> \n" +
                "    </camunda:inputOutput> \n" +
                "   </bpmn:extensionElements> \n" +
                "   <bpmn:incoming>SequenceFlow_1foezn2</bpmn:incoming> \n" +
                "   <bpmn:outgoing>SequenceFlow_1hpgfmq</bpmn:outgoing> \n" +
                "  </bpmn:serviceTask> \n" +
                "  <bpmn:userTask id=\"Task_EvaluateRFQ\" name=\"Evaluate RFQ\"> \n" +
                "   <bpmn:extensionElements> \n" +
                "    <camunda:inputOutput> \n" +
                "     <camunda:outputParameter name=\"quotationXML\"><![CDATA[${content} \n" +
                "      ]]></camunda:outputParameter> \n" +
                "    </camunda:inputOutput> \n" +
                "   </bpmn:extensionElements> \n" +
                "   <bpmn:incoming>SequenceFlow_1hpgfmq</bpmn:incoming> \n" +
                "   <bpmn:outgoing>SequenceFlow_1rfxe09</bpmn:outgoing> \n" +
                "  </bpmn:userTask> \n" +
                "  <bpmn:serviceTask id=\"Task_QuotationToBuyer\" name=\"Quotation to Buyer\" camunda:class=\"eu.nimble.service.bp.processor.test.TestResponseProcessor\"> \n" +
                "   <bpmn:extensionElements> \n" +
                "    <camunda:inputOutput> \n" +
                "     <camunda:inputParameter name=\"quotationXML\"><![CDATA[${content} \n" +
                "      ]]></camunda:inputParameter> \n" +
                "    </camunda:inputOutput> \n" +
                "   </bpmn:extensionElements> \n" +
                "   <bpmn:incoming>SequenceFlow_1rfxe09</bpmn:incoming> \n" +
                "   <bpmn:outgoing>SequenceFlow_0am3t90</bpmn:outgoing> \n" +
                "  </bpmn:serviceTask> \n" +
                " </bpmn:process> \n" +
                " <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\"> \n" +
                "  <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"NegotiationTest\"> \n" +
                "   <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\"> \n" +
                "    <dc:Bounds x=\"255\" y=\"206\" width=\"36\" height=\"36\" /> \n" +
                "    <bpmndi:BPMNLabel> \n" +
                "     <dc:Bounds x=\"228\" y=\"242\" width=\"90\" height=\"20\" /> \n" +
                "    </bpmndi:BPMNLabel> \n" +
                "   </bpmndi:BPMNShape> \n" +
                "   <bpmndi:BPMNEdge id=\"SequenceFlow_1foezn2_di\" bpmnElement=\"SequenceFlow_1foezn2\"> \n" +
                "    <di:waypoint xsi:type=\"dc:Point\" x=\"291\" y=\"224\" /> \n" +
                "    <di:waypoint xsi:type=\"dc:Point\" x=\"405\" y=\"224\" /> \n" +
                "    <bpmndi:BPMNLabel> \n" +
                "     <dc:Bounds x=\"348\" y=\"203\" width=\"0\" height=\"12\" /> \n" +
                "    </bpmndi:BPMNLabel> \n" +
                "   </bpmndi:BPMNEdge> \n" +
                "   <bpmndi:BPMNEdge id=\"SequenceFlow_1hpgfmq_di\" bpmnElement=\"SequenceFlow_1hpgfmq\"> \n" +
                "    <di:waypoint xsi:type=\"dc:Point\" x=\"505\" y=\"224\" /> \n" +
                "    <di:waypoint xsi:type=\"dc:Point\" x=\"588\" y=\"224\" /> \n" +
                "    <bpmndi:BPMNLabel> \n" +
                "     <dc:Bounds x=\"546.5\" y=\"203\" width=\"0\" height=\"12\" /> \n" +
                "    </bpmndi:BPMNLabel> \n" +
                "   </bpmndi:BPMNEdge> \n" +
                "   <bpmndi:BPMNEdge id=\"SequenceFlow_1rfxe09_di\" bpmnElement=\"SequenceFlow_1rfxe09\"> \n" +
                "    <di:waypoint xsi:type=\"dc:Point\" x=\"688\" y=\"224\" /> \n" +
                "    <di:waypoint xsi:type=\"dc:Point\" x=\"784\" y=\"224\" /> \n" +
                "    <bpmndi:BPMNLabel> \n" +
                "     <dc:Bounds x=\"691\" y=\"203\" width=\"90\" height=\"12\" /> \n" +
                "    </bpmndi:BPMNLabel> \n" +
                "   </bpmndi:BPMNEdge> \n" +
                "   <bpmndi:BPMNShape id=\"EndEvent_0mvpfgi_di\" bpmnElement=\"EndEvent_0mvpfgi\"> \n" +
                "    <dc:Bounds x=\"1022\" y=\"206\" width=\"36\" height=\"36\" /> \n" +
                "    <bpmndi:BPMNLabel> \n" +
                "     <dc:Bounds x=\"995\" y=\"246\" width=\"90\" height=\"12\" /> \n" +
                "    </bpmndi:BPMNLabel> \n" +
                "   </bpmndi:BPMNShape> \n" +
                "   <bpmndi:BPMNEdge id=\"SequenceFlow_0am3t90_di\" bpmnElement=\"SequenceFlow_0am3t90\"> \n" +
                "    <di:waypoint xsi:type=\"dc:Point\" x=\"884\" y=\"224\" /> \n" +
                "    <di:waypoint xsi:type=\"dc:Point\" x=\"1022\" y=\"224\" /> \n" +
                "    <bpmndi:BPMNLabel> \n" +
                "     <dc:Bounds x=\"908\" y=\"203\" width=\"90\" height=\"12\" /> \n" +
                "    </bpmndi:BPMNLabel> \n" +
                "   </bpmndi:BPMNEdge> \n" +
                "   <bpmndi:BPMNShape id=\"ServiceTask_0n1727p_di\" bpmnElement=\"Task_RFQToSeller\"> \n" +
                "    <dc:Bounds x=\"405\" y=\"184\" width=\"100\" height=\"80\" /> \n" +
                "   </bpmndi:BPMNShape> \n" +
                "   <bpmndi:BPMNShape id=\"UserTask_1umcxuf_di\" bpmnElement=\"Task_EvaluateRFQ\"> \n" +
                "    <dc:Bounds x=\"588\" y=\"184\" width=\"100\" height=\"80\" /> \n" +
                "   </bpmndi:BPMNShape> \n" +
                "   <bpmndi:BPMNShape id=\"ServiceTask_0zrh848_di\" bpmnElement=\"Task_QuotationToBuyer\"> \n" +
                "    <dc:Bounds x=\"784\" y=\"184\" width=\"100\" height=\"80\" /> \n" +
                "   </bpmndi:BPMNShape> \n" +
                "  </bpmndi:BPMNPlane> \n" +
                " </bpmndi:BPMNDiagram> \n" +
                "</bpmn:definitions>";
    }

    private static String getOrderBPMNContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\" exporterVersion=\"1.7.2\">\n" +
                "  <bpmn:process id=\"OrderTest\" name=\"OrderTest\" isExecutable=\"true\">\n" +
                "    <bpmn:extensionElements>\n" +
                "      <camunda:properties>\n" +
                "        <camunda:property name=\"businessProcessCategory\" value=\"ORDER\" />\n" +
                "      </camunda:properties>\n" +
                "    </bpmn:extensionElements>  \n" +
                "    <bpmn:startEvent id=\"StartEvent_1\">\n" +
                "      <bpmn:outgoing>SequenceFlow_1foezn2</bpmn:outgoing>\n" +
                "    </bpmn:startEvent>\n" +
                "    <bpmn:sequenceFlow id=\"SequenceFlow_1foezn2\" sourceRef=\"StartEvent_1\" targetRef=\"Task_OrderToSeller\" />\n" +
                "    <bpmn:sequenceFlow id=\"SequenceFlow_1hpgfmq\" sourceRef=\"Task_OrderToSeller\" targetRef=\"Task_EvaluateOrder\" />\n" +
                "    <bpmn:sequenceFlow id=\"SequenceFlow_1rfxe09\" sourceRef=\"Task_EvaluateOrder\" targetRef=\"Task_OrderResponseToBuyer\" />\n" +
                "    <bpmn:endEvent id=\"EndEvent_0mvpfgi\">\n" +
                "      <bpmn:incoming>SequenceFlow_0am3t90</bpmn:incoming>\n" +
                "    </bpmn:endEvent>\n" +
                "    <bpmn:sequenceFlow id=\"SequenceFlow_0am3t90\" sourceRef=\"Task_OrderResponseToBuyer\" targetRef=\"EndEvent_0mvpfgi\" />\n" +
                "    <bpmn:serviceTask id=\"Task_OrderToSeller\" name=\"Order to Seller\" camunda:class=\"eu.nimble.service.bp.processor.test.TestProcessor\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <camunda:inputOutput>\n" +
                "          <camunda:inputParameter name=\"buyerID\"><![CDATA[${initiatorID}\n" +
                "]]></camunda:inputParameter>\n" +
                "          <camunda:inputParameter name=\"sellerID\"><![CDATA[${responderID}\n" +
                "]]></camunda:inputParameter>\n" +
                "          <camunda:inputParameter name=\"orderXML\"><![CDATA[${content}\n" +
                "]]></camunda:inputParameter>\n" +
                "        </camunda:inputOutput>\n" +
                "      </bpmn:extensionElements>\n" +
                "      <bpmn:incoming>SequenceFlow_1foezn2</bpmn:incoming>\n" +
                "      <bpmn:outgoing>SequenceFlow_1hpgfmq</bpmn:outgoing>\n" +
                "    </bpmn:serviceTask>\n" +
                "    <bpmn:userTask id=\"Task_EvaluateOrder\" name=\"Evaluate Order\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <camunda:inputOutput>\n" +
                "          <camunda:outputParameter name=\"orderResponseXML\"><![CDATA[${content}\n" +
                "]]></camunda:outputParameter>\n" +
                "        </camunda:inputOutput>\n" +
                "      </bpmn:extensionElements>\n" +
                "      <bpmn:incoming>SequenceFlow_1hpgfmq</bpmn:incoming>\n" +
                "      <bpmn:outgoing>SequenceFlow_1rfxe09</bpmn:outgoing>\n" +
                "    </bpmn:userTask>\n" +
                "    <bpmn:serviceTask id=\"Task_OrderResponseToBuyer\" name=\"Order Response to Buyer\" camunda:class=\"eu.nimble.service.bp.processor.test.TestResponseProcessor\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <camunda:inputOutput>\n" +
                "          <camunda:inputParameter name=\"orderResponseXML\"><![CDATA[${content}\n" +
                "]]></camunda:inputParameter>\n" +
                "        </camunda:inputOutput>\n" +
                "      </bpmn:extensionElements>\n" +
                "      <bpmn:incoming>SequenceFlow_1rfxe09</bpmn:incoming>\n" +
                "      <bpmn:outgoing>SequenceFlow_0am3t90</bpmn:outgoing>\n" +
                "    </bpmn:serviceTask>\n" +
                "  </bpmn:process>\n" +
                "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n" +
                "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"OrderTest\">\n" +
                "      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n" +
                "        <dc:Bounds x=\"255\" y=\"206\" width=\"36\" height=\"36\" />\n" +
                "        <bpmndi:BPMNLabel>\n" +
                "          <dc:Bounds x=\"228\" y=\"242\" width=\"90\" height=\"20\" />\n" +
                "        </bpmndi:BPMNLabel>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNEdge id=\"SequenceFlow_1foezn2_di\" bpmnElement=\"SequenceFlow_1foezn2\">\n" +
                "        <di:waypoint xsi:type=\"dc:Point\" x=\"291\" y=\"224\" />\n" +
                "        <di:waypoint xsi:type=\"dc:Point\" x=\"405\" y=\"224\" />\n" +
                "        <bpmndi:BPMNLabel>\n" +
                "          <dc:Bounds x=\"348\" y=\"203\" width=\"0\" height=\"12\" />\n" +
                "        </bpmndi:BPMNLabel>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"SequenceFlow_1hpgfmq_di\" bpmnElement=\"SequenceFlow_1hpgfmq\">\n" +
                "        <di:waypoint xsi:type=\"dc:Point\" x=\"505\" y=\"224\" />\n" +
                "        <di:waypoint xsi:type=\"dc:Point\" x=\"588\" y=\"224\" />\n" +
                "        <bpmndi:BPMNLabel>\n" +
                "          <dc:Bounds x=\"546.5\" y=\"203\" width=\"0\" height=\"12\" />\n" +
                "        </bpmndi:BPMNLabel>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"SequenceFlow_1rfxe09_di\" bpmnElement=\"SequenceFlow_1rfxe09\">\n" +
                "        <di:waypoint xsi:type=\"dc:Point\" x=\"688\" y=\"224\" />\n" +
                "        <di:waypoint xsi:type=\"dc:Point\" x=\"784\" y=\"224\" />\n" +
                "        <bpmndi:BPMNLabel>\n" +
                "          <dc:Bounds x=\"691\" y=\"203\" width=\"90\" height=\"12\" />\n" +
                "        </bpmndi:BPMNLabel>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNShape id=\"EndEvent_0mvpfgi_di\" bpmnElement=\"EndEvent_0mvpfgi\">\n" +
                "        <dc:Bounds x=\"1022\" y=\"206\" width=\"36\" height=\"36\" />\n" +
                "        <bpmndi:BPMNLabel>\n" +
                "          <dc:Bounds x=\"995\" y=\"246\" width=\"90\" height=\"12\" />\n" +
                "        </bpmndi:BPMNLabel>\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNEdge id=\"SequenceFlow_0am3t90_di\" bpmnElement=\"SequenceFlow_0am3t90\">\n" +
                "        <di:waypoint xsi:type=\"dc:Point\" x=\"884\" y=\"224\" />\n" +
                "        <di:waypoint xsi:type=\"dc:Point\" x=\"1022\" y=\"224\" />\n" +
                "        <bpmndi:BPMNLabel>\n" +
                "          <dc:Bounds x=\"908\" y=\"203\" width=\"90\" height=\"12\" />\n" +
                "        </bpmndi:BPMNLabel>\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNShape id=\"ServiceTask_0n1727p_di\" bpmnElement=\"Task_OrderToSeller\">\n" +
                "        <dc:Bounds x=\"405\" y=\"184\" width=\"100\" height=\"80\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"UserTask_1umcxuf_di\" bpmnElement=\"Task_EvaluateOrder\">\n" +
                "        <dc:Bounds x=\"588\" y=\"184\" width=\"100\" height=\"80\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"ServiceTask_0zrh848_di\" bpmnElement=\"Task_OrderResponseToBuyer\">\n" +
                "        <dc:Bounds x=\"784\" y=\"184\" width=\"100\" height=\"80\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "    </bpmndi:BPMNPlane>\n" +
                "  </bpmndi:BPMNDiagram>\n" +
                "</bpmn:definitions>\n";
    }

    public static ProcessInstanceInputMessage createStartProcessInstanceInputMessageForNegotiation() {
        ProcessInstanceInputMessage inputMessage = new ProcessInstanceInputMessage();
        inputMessage.setProcessInstanceID("negotiationProcessInstance1387");

        ProcessVariables variables = new ProcessVariables();
        variables.setProcessID("NegotiationTest");
        variables.setContent("JSON Content for Negotiation");
        variables.setInitiatorID(partnerID);
        variables.setResponderID("seller1387");

        inputMessage.setVariables(variables);

        return inputMessage;
    }

    public static ProcessInstanceInputMessage createContinueProcessInstanceInputMessageForNegotiation() {
        ProcessInstanceInputMessage inputMessage = createStartProcessInstanceInputMessageForNegotiation();
        inputMessage.getVariables().setInitiatorID("seller1387");
        inputMessage.getVariables().setResponderID(partnerID);

        return inputMessage;
    }
}
