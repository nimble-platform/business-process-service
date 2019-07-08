package eu.nimble.service.bp.processor.fulfilment;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.util.persistence.bp.ExecutionConfigurationDAOUtility;
import eu.nimble.service.bp.serialization.MixInIgnoreProperties;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import eu.nimble.utility.JsonSerializationUtility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by yildiray on 6/29/2017.
 */
public class DefaultReceiptAdviceSender implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ DefaultReceiptAdviceSender: {}", execution);
        final Map<String, Object> variables = execution.getVariables();
        // for debug purposes
        logger.debug(JsonSerializationUtility.getObjectMapperWithMixIn(Map.class, MixInIgnoreProperties.class).writeValueAsString(variables));

        // get process instance id
        String processInstanceId = execution.getProcessInstance().getProcessInstanceId();

        // get input variables
        String buyer = variables.get("initiatorID").toString();
        String seller = variables.get("responderID").toString();
        String processContextId = variables.get("processContextId").toString();
        ReceiptAdviceType receiptAdvice = (ReceiptAdviceType) variables.get("receiptAdvice");

        // get application execution configuration
        ExecutionConfiguration executionConfiguration = ExecutionConfigurationDAOUtility.getExecutionConfiguration(buyer,
                execution.getProcessInstance().getProcessDefinitionId(), ProcessConfiguration.RoleTypeEnum.BUYER, "RECEIPTADVICE",
                ExecutionConfiguration.ApplicationTypeEnum.DATACHANNEL);
        String applicationURI = executionConfiguration.getExecutionUri();
        ExecutionConfiguration.ExecutionTypeEnum executionType = executionConfiguration.getExecutionType();

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.ExecutionTypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            businessProcessApplication.sendDocument(processContextId,processInstanceId, buyer, seller, receiptAdvice);
        } else if(executionType == ExecutionConfiguration.ExecutionTypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }
        // remove variables
        String initialDocumentID = (String) execution.getVariable("initialDocumentID");
        execution.removeVariables();
        execution.setVariable("initialDocumentID",initialDocumentID);
        execution.setVariable("responseDocumentID",receiptAdvice.getID());
    }
}