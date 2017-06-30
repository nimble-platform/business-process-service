package eu.nimble.service.bp.processor.negotiation;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.sun.org.apache.regexp.internal.RE;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.util.DocumentDAOUtility;
import eu.nimble.service.bp.swagger.model.ApplicationConfiguration;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by yildiray on 6/29/2017.
 */
public class DefaultRFQProcessor implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ DefaultRFQProcessor: {}", execution);
        final Map<String, Object> variables = execution.getVariables();
        // for debug purposes
        for (String key : variables.keySet()) {
            logger.debug(" $$$ Variable name {}, value {}", key, variables.get(key));
        }

        // get process instance id
        String processInstanceId = execution.getProcessInstance().getProcessInstanceId();

        // get input variables
        String buyer = variables.get("initiatorID").toString();
        String seller = variables.get("responderID").toString();
        RequestForQuotationType requestForQuotation = (RequestForQuotationType) variables.get("requestForQuotation");

        // get application execution configuration
        ExecutionConfiguration executionConfiguration = DocumentDAOUtility.getExecutionConfiguration(buyer, execution.getProcessInstance().getProcessDefinitionId(),
                ApplicationConfiguration.TypeEnum.DATAPROCESSOR);
        String applicationURI = executionConfiguration.getURI();
        ExecutionConfiguration.TypeEnum executionType = executionConfiguration.getType();

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.TypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            businessProcessApplication.saveDocument(processInstanceId, buyer, seller, requestForQuotation);
        } else if(executionType == ExecutionConfiguration.TypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }
    }
}