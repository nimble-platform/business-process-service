package eu.nimble.service.bp.processor.fulfilment;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.util.persistence.DocumentDAOUtility;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.model.ubl.despatchadvice.DespatchAdviceType;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by yildiray on 6/29/2017.
 */
public class DefaultDespatchAdviceProcessor  implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ DefaultDespatchAdviceProcessor: {}", execution);
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
        List<String> relatedProducts = (List<String>) variables.get("relatedProducts");
        DespatchAdviceType despatchAdvice = (DespatchAdviceType) variables.get("despatchAdvice");

        // get application execution configuration
        ExecutionConfiguration executionConfiguration = DocumentDAOUtility.getExecutionConfiguration(seller,
                execution.getProcessInstance().getProcessDefinitionId(), ProcessConfiguration.RoleTypeEnum.SELLER,"DESPATCHADVICE",
                ExecutionConfiguration.ApplicationTypeEnum.DATAPROCESSOR);
        String applicationURI = executionConfiguration.getExecutionUri();
        ExecutionConfiguration.ExecutionTypeEnum executionType = executionConfiguration.getExecutionType();

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.ExecutionTypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            // NOTE: Pay attention to the direction of the document. Here it is from seller to buyer
            businessProcessApplication.saveDocument(processInstanceId, seller, buyer, despatchAdvice, relatedProducts);
        } else if(executionType == ExecutionConfiguration.ExecutionTypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }
    }
}