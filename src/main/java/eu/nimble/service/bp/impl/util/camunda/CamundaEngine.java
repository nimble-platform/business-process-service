/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util.camunda;

import eu.nimble.service.bp.swagger.model.Process;
import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.swagger.model.ProcessVariables;
import eu.nimble.utility.XMLUtility;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.dom.DOMSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author yildiray
 */
public class CamundaEngine {
	private static ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
	private static RepositoryService repositoryService = processEngine.getRepositoryService();
	private static RuntimeService runtimeService = processEngine.getRuntimeService();
	private static TaskService taskService = processEngine.getTaskService();

	private static Logger logger = LoggerFactory.getLogger(CamundaEngine.class);

	public static ProcessInstance continueProcessInstance(ProcessInstanceInputMessage body) {
		String processInstanceID = body.getProcessInstanceID();
		Task task = taskService.createTaskQuery().processInstanceId(processInstanceID).list().get(0);

		Map<String,Object> data = getVariablesData(body);

		ProcessInstance processInstance = new ProcessInstance();
		processInstance.setProcessID(body.getVariables().getProcessID());
		processInstance.setProcessInstanceID(processInstanceID);
		//processInstance.setProcessInstanceID("prc124");
		processInstance.setStatus(ProcessInstance.StatusEnum.COMPLETED);

		logger.info(" Completing business process instance {}, with data {}", processInstanceID, data.toString());
		taskService.complete(task.getId(), data);
		logger.info(" Completed business process instance {}", processInstanceID);

		return processInstance;
	}

	public static ProcessInstance startProcessInstance(ProcessInstanceInputMessage body) {
		Map<String,Object> data = getVariablesData(body);
		String processID  = body.getVariables().getProcessID();

		logger.info(" Starting business process instance for {}, with data {}", processID, data.toString());
		org.camunda.bpm.engine.runtime.ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processID, data);
		logger.info(" Started business process instance for {}, with instance id {}", processID, processInstance.getProcessInstanceId());

		ProcessInstance businessProcessInstance = new ProcessInstance();
		businessProcessInstance.setProcessID(processID);
		//businessProcessInstance.setProcessInstanceID("prc124");
		businessProcessInstance.setProcessInstanceID(processInstance.getProcessInstanceId());
		businessProcessInstance.setStatus(ProcessInstance.StatusEnum.STARTED);

		return businessProcessInstance;
	}

	public static List<Process> getProcessDefinitions() {
		List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
		List<Process> processes = new ArrayList<>();
		for(ProcessDefinition processDefinition: processDefinitions) {
			Process process = mapProcess(processDefinition);
			processes.add(process);
		}

		return processes;
	}

	private static Process mapProcess(ProcessDefinition processDefinition) {
		String key = processDefinition.getKey();
		String name = processDefinition.getName();
		String processDefinitionId = processDefinition.getId();

		BpmnModelInstance bpmnModel = repositoryService.getBpmnModelInstance(processDefinitionId);

		DOMSource domSource = bpmnModel.getDocument().getDomSource();
		String bpmnContent = XMLUtility.nodeToString(domSource.getNode());
		String type = XMLUtility.evaluateXPathAndGetAttributeValue(domSource.getNode(), "//camunda:property[@name = 'businessProcessCategory']/@value");
		if(type == null)
			type = "OTHER";

		logger.info(" $$$ Getting BPMN {} {} {} {}", processDefinitionId, key, name, type);

		Process process = new Process();
		process.setProcessID(key);
		process.setProcessName(name);
		process.setBpmnContent(bpmnContent);
		process.setProcessType(Process.ProcessTypeEnum.valueOf(type));
		return  process;
	}

	private static Map<String,Object> getVariablesData(ProcessInstanceInputMessage body) {
		ProcessVariables variables = body.getVariables();
		String content = variables.getContent();
		String initiatorID = variables.getInitiatorID();
		String responderID = variables.getResponderID();

		Map<String,Object> data = new HashMap<String,Object>();
		data.put("initiatorID", initiatorID);
		data.put("responderID", responderID);
		data.put("content", content);
		return data;
	}

	public static void addProcessDefinition(String processID, String bpmnContent) {
		repositoryService.createDeployment().addString(processID +".bpmn", bpmnContent).deploy();
		//getProcessDefinitions();
	}

	public static void deleteProcessDefinition(String processID) {
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processID).singleResult();
		repositoryService.deleteProcessDefinition(processDefinition.getId(), true);
	}

	public static Process getProcessDefinition(String processID) {
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processID).singleResult();
		Process process = mapProcess(processDefinition);
		return process;
	}

	public static void updateProcessDefinition(String processID, String bpmnContent) {
		deleteProcessDefinition(processID);
		addProcessDefinition(processID, bpmnContent);
	}
}
