package eu.nimble.service.bp.processor.orderresponse;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.util.DocumentDAOUtility;
import eu.nimble.service.bp.swagger.model.ApplicationConfiguration;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by yildiray on 5/26/2017.
 * Receives the order response document saves it to seller space
 */
public class DefaultOrderResponseProcessor  implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ DefaultOrderResponseProcessor: {}", execution);
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

        // get application execution configuration
        ExecutionConfiguration executionConfiguration = DocumentDAOUtility.getExecutionConfiguration(seller,
                execution.getProcessInstance().getProcessDefinitionId(),
                ApplicationConfiguration.TypeEnum.DATAPROCESSOR);
        String applicationURI = executionConfiguration.getURI();
        ExecutionConfiguration.TypeEnum executionType = executionConfiguration.getType();

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.TypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            // NOTE: Pay attention to the direction of the document. Here it is from seller to buyer
            businessProcessApplication.saveDocument(processInstanceId, seller, buyer,
                    orderResponse, ProcessDocumentMetadata.TypeEnum.ORDERRESPONSESIMPLE,
                    ProcessDocumentMetadata.StatusEnum.APPROVED);
        } else if(executionType == ExecutionConfiguration.TypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }
    }
}