package eu.nimble.service.bp.processor.orderresponse;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.util.DocumentDAOUtility;
import eu.nimble.service.bp.swagger.model.ApplicationConfiguration;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
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
        for (String key : variables.keySet()) {
            logger.debug(" $$$ Variable name {}, value {}", key, variables.get(key));
        }

        // get process instance id
        String processInstanceId = execution.getProcessInstance().getProcessInstanceId();

        // get input variables
        String buyer = variables.get("responderID").toString();
        String seller = variables.get("initiatorID").toString();
        OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) variables.get("orderResponse");
        OrderType order = (OrderType) variables.get("order");

        // get application execution configuration
        ExecutionConfiguration executionConfiguration = DocumentDAOUtility.getExecutionConfiguration(seller,
                execution.getProcessInstance().getProcessDefinitionId(),
                ApplicationConfiguration.TypeEnum.DATACHANNEL);
        String applicationURI = executionConfiguration.getURI();
        ExecutionConfiguration.TypeEnum executionType = executionConfiguration.getType();

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.TypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            // get the response status from the order response. So in the next step you will set the status of the order accordingly.
            ProcessDocumentMetadata.StatusEnum responseStatus = orderResponse.isAcceptedIndicator() ? ProcessDocumentMetadata.StatusEnum.APPROVED : ProcessDocumentMetadata.StatusEnum.DENIED;

            // note the direction of the document (here it is from seller to buyer)
            businessProcessApplication.sendDocument(processInstanceId, seller, buyer,
                    orderResponse, ProcessDocumentMetadata.TypeEnum.ORDERRESPONSE,
                    ProcessDocumentMetadata.StatusEnum.APPROVED,
                    order.getID(), responseStatus);
        } else if(executionType == ExecutionConfiguration.TypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }
    }
}