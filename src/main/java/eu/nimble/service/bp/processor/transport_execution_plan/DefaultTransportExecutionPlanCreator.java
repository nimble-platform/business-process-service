package eu.nimble.service.bp.processor.transport_execution_plan;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.transportexecutionplan.TransportExecutionPlanType;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DefaultTransportExecutionPlanCreator implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {

        logger.info(" $$$ DefaultTransportExecutionPlanCreator: {}", execution);
        final Map<String, Object> variables = execution.getVariables();
        // for debug purposes
        for (String key: variables.keySet()) {
            logger.debug(" $$$ Variable name {}, value {}", key, variables.get(key));
        }
        // get input variables
        String buyer = variables.get("initiatorID").toString();
        String seller = variables.get("responderID").toString();
        String content = variables.get("content").toString();

        // get application execution configuration
        ExecutionConfiguration executionConfiguration = DocumentPersistenceUtility.getExecutionConfiguration(buyer,
                execution.getProcessInstance().getProcessDefinitionId(), ProcessConfiguration.RoleTypeEnum.SELLER, "TRANSPORT_EXECUTION_PLAN", ExecutionConfiguration.ApplicationTypeEnum.DATAADAPTER);
        String applicationURI = executionConfiguration.getExecutionUri();
        ExecutionConfiguration.ExecutionTypeEnum executionType = executionConfiguration.getExecutionType();

         // specify output variables
        TransportExecutionPlanType transportExecutionPlan = null;

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.ExecutionTypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            transportExecutionPlan  = (TransportExecutionPlanType) businessProcessApplication.createDocument(buyer, seller, content, ProcessDocumentMetadata.TypeEnum.TRANSPORTEXECUTIONPLAN);
        } else if(executionType == ExecutionConfiguration.ExecutionTypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }

        // set the corresponding camunda business process variable
        execution.setVariable("transportExecutionPlan", transportExecutionPlan);
    }
}