package eu.nimble.service.bp.impl.util.jssequence;

import eu.nimble.service.bp.swagger.model.Process;
import eu.nimble.service.bp.swagger.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class JSSequenceDiagramParser {
    private String processID;
    private String content;
    private String processName;
    private Process.ProcessTypeEnum processType;
    private String bpmnContent;
    private String diagramContent;
    private int flowCount = 0;
    private List<Transaction> transactions; // create this

    public JSSequenceDiagramParser(Process body) {
        this.processID = body.getProcessID();
        this.content = body.getTextContent();
        this.processName = body.getProcessName();
        this.processType = body.getProcessType();
        this.transactions = new ArrayList<>();
    }

    private void initialize() {
        bpmnContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions \n" +
                "\txmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" \n" +
                "\txmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" \n" +
                "\txmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\"\n" +
                "\txmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" \n" +
                "\txmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" \n" +
                "\txmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "\tid=\"Definitions_1\"\n" +
                "\ttargetNamespace=\"http://bpmn.io/schema/bpmn\" \n" +
                "\texporter=\"Camunda Modeler\" \n" +
                "\texporterVersion=\"1.7.2\">\n" +
                "\t<bpmn:process id=\"" + processID + "\" name=\"" + processName + "\" isExecutable=\"true\">\n" +
                "\t\t<bpmn:extensionElements>\n" +
                "\t\t\t<camunda:properties>\n" +
                "\t\t\t\t<camunda:property name=\"businessProcessCategory\" value=\"" + processType.toString() + "\" />\n" +
                "\t\t\t</camunda:properties>\n" +
                "\t\t</bpmn:extensionElements>\n" +
                "\t\t<bpmn:startEvent id=\"StartEvent\">\n" +
                "\t\t\t<bpmn:outgoing>Flow" + (++flowCount) + "</bpmn:outgoing>\n" +
                "\t\t</bpmn:startEvent>\n";

        diagramContent = "\t<bpmndi:BPMNDiagram id=\"BPMNDiagram\">\n" +
                "\t\t<bpmndi:BPMNPlane id=\"BPMNPlane\" bpmnElement=\"" + processID + "\">\n" +
                "\t\t\t<bpmndi:BPMNShape id=\"StartEventShape\" bpmnElement=\"StartEvent\">\n" +
                "\t\t\t\t<dc:Bounds x=\"100\" y=\"200\" width=\"50\" height=\"50\" />\n" +
                "\t\t\t</bpmndi:BPMNShape>\n";
    }

    public String getBPMNContent() {
        initialize();
        parse();
        return bpmnContent;
    }

    private void parse() {
        String[] lines = content.split("\n");
        String normalizedDocumentName = "";
        String lastTaskID = "StartEvent";
        int xIndex = 150;

        for (String line : lines) {

            if (line.startsWith("Title")) {
                String[] parts = line.split(":");
                String processName = parts[1].trim();
            } else if (line.startsWith("Note")) {
                if (line.contains("right"))
                    line = line.replaceAll("Note right of ", "");
                else
                    line = line.replaceAll("Note left of ", "");

                String[] parts = line.split(":");

                String role = parts[0].trim().toUpperCase();
                String userTaskName = parts[1].trim();

                lastTaskID = userTaskName.replaceAll(" ", "").trim();
                bpmnContent += "\t\t<bpmn:sequenceFlow id=\"Flow" + flowCount + "\" sourceRef=\"Send" + normalizedDocumentName + "Documentto" + role + "\" targetRef=\"" + lastTaskID + "\" />\n" +
                        "\t\t<bpmn:userTask id=\"" + lastTaskID + "\" name=\"" + userTaskName + "\">\n" +
                        "\t\t\t<bpmn:incoming>Flow" + flowCount + "</bpmn:incoming>\n" +
                        "\t\t\t<bpmn:outgoing>Flow" + (++flowCount) + "</bpmn:outgoing>\n" +
                        "\t\t</bpmn:userTask>\n";

                diagramContent += "<bpmndi:BPMNEdge id=\"Flow" + (flowCount - 1) + "Shape\" bpmnElement=\"Flow" + (flowCount - 1) + "\">\n" +
                        "\t\t\t\t<di:waypoint xsi:type=\"dc:Point\" x=\"" + xIndex + "\" y=\"225\" />\n" +
                        "\t\t\t\t<di:waypoint xsi:type=\"dc:Point\" x=\"" + (xIndex + 50) + "\" y=\"225\" />\n" +
                        "\t\t\t</bpmndi:BPMNEdge>\n" +
                        "\t\t\t<bpmndi:BPMNShape id=\"" + lastTaskID + "Shape\" bpmnElement=\"" + lastTaskID + "\">\n" +
                        "\t\t\t\t<dc:Bounds x=\"" + (xIndex + 50) + "\" y=\"185\" width=\"100\" height=\"80\" />\n" +
                        "\t\t\t</bpmndi:BPMNShape>\n";

                xIndex += 150;

            } else if (line.contains("->")) {
                String[] parts = line.split(":");
                String documentName = parts[1].trim();
                normalizedDocumentName = documentName.replaceAll(" ", "").trim();

                String[] roles = parts[0].split("->");
                String initiator = roles[0].trim().toUpperCase();
                String responder = roles[1].trim().toUpperCase();

                Transaction transaction = new Transaction();
                transaction.setInitiatorRole(Transaction.InitiatorRoleEnum.valueOf(initiator));
                transaction.setResponderRole(Transaction.ResponderRoleEnum.valueOf(responder));
                transaction.setDocumentType(Transaction.DocumentTypeEnum.valueOf(normalizedDocumentName.toUpperCase()));
                transaction.setTransactionID(normalizedDocumentName);
                transactions.add(transaction);

                bpmnContent += "\t\t<bpmn:sequenceFlow id=\"Flow" + flowCount + "\" sourceRef=\"" + lastTaskID + "\" targetRef=\"Create" + normalizedDocumentName + "Document\" />\n" +
                        "\t\t<bpmn:serviceTask id=\"Create" + normalizedDocumentName + "Document\" name=\"Create " + normalizedDocumentName + " Document (Data Adapter)\" camunda:class=\"eu.nimble.service.bp.processor." + normalizedDocumentName.toLowerCase() + ".Default" + normalizedDocumentName + "Creator\">\n" +
                        "\t\t\t<bpmn:incoming>Flow" + flowCount + "</bpmn:incoming>\n" +
                        "\t\t\t<bpmn:outgoing>Flow" + (++flowCount) + "</bpmn:outgoing>\n" +
                        "\t\t</bpmn:serviceTask>\n" +
                        "\t\t<bpmn:sequenceFlow id=\"Flow" + flowCount + "\" sourceRef=\"Create" + normalizedDocumentName + "Document\" targetRef=\"Process" + normalizedDocumentName + "Document\" />\n" +
                        "\t\t<bpmn:serviceTask id=\"Process" + normalizedDocumentName + "Document\" name=\"Process " + normalizedDocumentName + " Document (Data Processor)\" camunda:class=\"eu.nimble.service.bp.processor." + normalizedDocumentName.toLowerCase() + ".Default" + normalizedDocumentName + "Processor\">\n" +
                        "\t\t\t<bpmn:incoming>Flow" + flowCount + "</bpmn:incoming>\n" +
                        "\t\t\t<bpmn:outgoing>Flow" + (++flowCount) + "</bpmn:outgoing>\n" +
                        "\t\t</bpmn:serviceTask>\n" +
                        "\t\t<bpmn:sequenceFlow id=\"Flow" + flowCount + "\" sourceRef=\"Process" + normalizedDocumentName + "Document\" targetRef=\"Send" + normalizedDocumentName + "Documentto" + responder + "\" />\n" +
                        "\t\t<bpmn:serviceTask id=\"Send" + normalizedDocumentName + "Documentto" + responder + "\" name=\"Send " + normalizedDocumentName + " Document to " + responder + " (Data Channel)\" camunda:class=\"eu.nimble.service.bp.processor." + normalizedDocumentName.toLowerCase() + ".Default" + normalizedDocumentName + "Sender\">\n" +
                        "\t\t\t<bpmn:incoming>Flow" + flowCount + "</bpmn:incoming>\n" +
                        "\t\t\t<bpmn:outgoing>Flow" + (++flowCount) + "</bpmn:outgoing>\n" +
                        "\t\t</bpmn:serviceTask>\n";

                diagramContent += "\t\t\t<bpmndi:BPMNEdge id=\"Flow" + (flowCount - 3) + "Shape\" bpmnElement=\"Flow" + (flowCount - 3) + "\">\n" +
                        "\t\t\t\t<di:waypoint xsi:type=\"dc:Point\" x=\"" + xIndex + "\" y=\"225\" />\n" +
                        "\t\t\t\t<di:waypoint xsi:type=\"dc:Point\" x=\"" + (xIndex + 50) + "\" y=\"225\" />\n" +
                        "\t\t\t</bpmndi:BPMNEdge>\n" +
                        "\t\t\t<bpmndi:BPMNShape id=\"Create" + normalizedDocumentName + "DocumentShape\" bpmnElement=\"Create" + normalizedDocumentName + "Document\">\n" +
                        "\t\t\t\t<dc:Bounds x=\"" + (xIndex + 50) + "\" y=\"185\" width=\"100\" height=\"80\" />\n" +
                        "\t\t\t</bpmndi:BPMNShape>\n" +
                        "\t\t\t<bpmndi:BPMNEdge id=\"Flow" + (flowCount - 2) + "Shape\" bpmnElement=\"Flow" + (flowCount - 2) + "\">\n" +
                        "\t\t\t\t<di:waypoint xsi:type=\"dc:Point\" x=\"" + (xIndex + 150) + "\" y=\"225\" />\n" +
                        "\t\t\t\t<di:waypoint xsi:type=\"dc:Point\" x=\"" + (xIndex + 200) + "\" y=\"225\" />\n" +
                        "\t\t\t</bpmndi:BPMNEdge>\n" +
                        "\t\t\t<bpmndi:BPMNShape id=\"Process" + normalizedDocumentName + "DocumentShape\" bpmnElement=\"Process" + normalizedDocumentName + "Document\">\n" +
                        "\t\t\t\t<dc:Bounds x=\"" + (xIndex + 200) + "\" y=\"185\" width=\"100\" height=\"80\" />\n" +
                        "\t\t\t</bpmndi:BPMNShape>\n" +
                        "\t\t\t<bpmndi:BPMNEdge id=\"Flow" + (flowCount - 1) + "Shape\" bpmnElement=\"Flow" + (flowCount - 1) + "\">\n" +
                        "\t\t\t\t<di:waypoint xsi:type=\"dc:Point\" x=\"" + (xIndex + 300) + "\" y=\"225\" />\n" +
                        "\t\t\t\t<di:waypoint xsi:type=\"dc:Point\" x=\"" + (xIndex + 350) + "\" y=\"225\" />\n" +
                        "\t\t\t</bpmndi:BPMNEdge>\n" +
                        "\t\t\t<bpmndi:BPMNShape id=\"Send" + normalizedDocumentName + "Documentto" + responder + "Shape\" bpmnElement=\"Send" + normalizedDocumentName + "Documentto" + responder + "\">\n" +
                        "\t\t\t\t<dc:Bounds x=\"" + (xIndex + 350) + "\" y=\"185\" width=\"100\" height=\"80\" />\n" +
                        "\t\t\t</bpmndi:BPMNShape>\n";

                xIndex += 450;
                lastTaskID = "Send" + normalizedDocumentName + "Documentto" + responder;
            }
        }

        bpmnContent += "<bpmn:sequenceFlow id=\"Flow" + flowCount + "\" sourceRef=\"" + lastTaskID + "\" targetRef=\"EndEvent\" />\n" +
                "\t\t<bpmn:endEvent id=\"EndEvent\">\n" +
                "\t\t\t<bpmn:incoming>Flow" + flowCount + "</bpmn:incoming>\n" +
                "\t\t</bpmn:endEvent>\n" +
                "\t</bpmn:process>\n";

        diagramContent += "<bpmndi:BPMNEdge id=\"Flow" + flowCount + "Shape\" bpmnElement=\"Flow" + flowCount + "\">\n" +
                "\t\t\t\t<di:waypoint xsi:type=\"dc:Point\" x=\"" + xIndex + "\" y=\"225\" />\n" +
                "\t\t\t\t<di:waypoint xsi:type=\"dc:Point\" x=\"" + (xIndex + 50) + "\" y=\"225\" />\n" +
                "\t\t\t</bpmndi:BPMNEdge>\n" +
                "\t\t\t<bpmndi:BPMNShape id=\"EndEventShape\" bpmnElement=\"EndEvent\">\n" +
                "\t\t\t\t<dc:Bounds x=\"" + (xIndex + 50) + "\" y=\"200\" width=\"50\" height=\"50\" />\n" +
                "\t\t\t</bpmndi:BPMNShape>\n" +
                "\t\t</bpmndi:BPMNPlane>\n" +
                "\t</bpmndi:BPMNDiagram>\n";
        bpmnContent += diagramContent +
                "</bpmn:definitions>";
    }

    public static void main(String argv[]) {
        String text = "Title: ORDER\n" +
                "Buyer -> Seller: Order\n" +
                "Note right of Seller: Evaluate Order\n" +
                "Seller -> Buyer: Order Response Simple";

        Process process = new Process();
        process.setTextContent(text);
        process.setProcessID("Order");
        process.setProcessName("Order");
        process.setProcessType(Process.ProcessTypeEnum.ORDER);

        JSSequenceDiagramParser parser = new JSSequenceDiagramParser(process);
        System.out.println(parser.getBPMNContent());
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}
