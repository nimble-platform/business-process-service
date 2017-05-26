/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util;

import eu.nimble.service.bp.swagger.model.ProcessInstance;
import eu.nimble.service.bp.swagger.model.ProcessInstanceInputMessage;
import eu.nimble.service.bp.swagger.model.ProcessVariables;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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

}
