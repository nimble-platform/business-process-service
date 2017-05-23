/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.service.bp.impl.util;

import eu.nimble.service.bp.swagger.model.BusinessProcessInstance;
import eu.nimble.service.bp.swagger.model.BusinessProcessInstanceInputMessage;
import eu.nimble.service.bp.swagger.model.BusinessProcessVariables;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.runtime.ProcessInstance;
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

	public static BusinessProcessInstance continueProcessInstance(BusinessProcessInstanceInputMessage body) {
		String businessProcessInstanceID = body.getBusinessProcessInstanceID();
		Task task = taskService.createTaskQuery().processInstanceId(businessProcessInstanceID).list().get(0);

		Map<String,Object> data = getVariablesData(body);

		BusinessProcessInstance businessProcessInstance = new BusinessProcessInstance();
		businessProcessInstance.setBusinessProcessID(body.getVariables().getBusinessProcessID());
		businessProcessInstance.setBusinessProcessInstanceID(businessProcessInstanceID);
		businessProcessInstance.setStatus(BusinessProcessInstance.StatusEnum.COMPLETED);

		logger.info(" Completing business process instance {}, with data {}", businessProcessInstanceID, data.toString());
		taskService.complete(task.getId(), data);
		logger.info(" Completed business process instance {}", businessProcessInstanceID);

		return businessProcessInstance;
	}

	public static BusinessProcessInstance startProcessInstance(BusinessProcessInstanceInputMessage body) {
		Map<String,Object> data = getVariablesData(body);
		String businessProcessID  = body.getVariables().getBusinessProcessID();

		logger.info(" Starting business process instance for {}, with data {}", businessProcessID, data.toString());
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(businessProcessID, data);
		logger.info(" Started business process instance for {}, with instance id {}", businessProcessID, processInstance.getProcessInstanceId());

		BusinessProcessInstance businessProcessInstance = new BusinessProcessInstance();
		businessProcessInstance.setBusinessProcessID(businessProcessID);
		businessProcessInstance.setBusinessProcessInstanceID(processInstance.getProcessInstanceId());
		businessProcessInstance.setStatus(BusinessProcessInstance.StatusEnum.STARTED);

		return businessProcessInstance;
	}

	private static Map<String,Object> getVariablesData(BusinessProcessInstanceInputMessage body) {
		BusinessProcessVariables variables = body.getVariables();
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
