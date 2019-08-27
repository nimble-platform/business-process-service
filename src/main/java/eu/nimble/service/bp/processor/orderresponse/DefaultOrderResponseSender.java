package eu.nimble.service.bp.processor.orderresponse;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.util.persistence.bp.ExecutionConfigurationDAOUtility;
import eu.nimble.service.bp.util.serialization.MixInIgnoreProperties;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.utility.JsonSerializationUtility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by yildiray on 5/26/2017.
 * Sends the order response document to the buyer. If it is realized in nimble, it matches the order response with previous order
 */
public class DefaultOrderResponseSender  implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ DefaultOrderResponseSender: {}", execution);
        final Map<String, Object> variables = execution.getVariables();
        // for debug purposes
        logger.debug(JsonSerializationUtility.getObjectMapperWithMixIn(Map.class, MixInIgnoreProperties.class).writeValueAsString(variables));

        // get process instance id
        String processInstanceId = execution.getProcessInstance().getProcessInstanceId();

        // get input variables
        String buyer = variables.get("responderID").toString();
        String seller = variables.get("initiatorID").toString();
        String processContextId = variables.get("processContextId").toString();
        OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) variables.get("orderResponse");

        // get application execution configuration
        ExecutionConfiguration executionConfiguration = ExecutionConfigurationDAOUtility.getExecutionConfiguration(seller,
                execution.getProcessInstance().getProcessDefinitionId(), ProcessConfiguration.RoleTypeEnum.SELLER, "ORDERRESPONSE",
                ExecutionConfiguration.ApplicationTypeEnum.DATACHANNEL);
        String applicationURI = executionConfiguration.getExecutionUri();
        ExecutionConfiguration.ExecutionTypeEnum executionType = executionConfiguration.getExecutionType();

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.ExecutionTypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            // note the direction of the document (here it is from seller to buyer)
            businessProcessApplication.sendDocument(processContextId,processInstanceId, seller, buyer, orderResponse);
        } else if(executionType == ExecutionConfiguration.ExecutionTypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }
        // remove variables
        String initialDocumentID = (String) execution.getVariable("initialDocumentID");
        execution.removeVariables();
        execution.setVariable("initialDocumentID",initialDocumentID);
        execution.setVariable("responseDocumentID",orderResponse.getID());
    }
}
