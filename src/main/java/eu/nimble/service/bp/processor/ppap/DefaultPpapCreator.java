package eu.nimble.service.bp.processor.ppap;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentDAOUtility;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.ppaprequest.PpapRequestType;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * Created by dogukan on 11.10.2017.
 */
public class DefaultPpapCreator implements JavaDelegate{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ DefaultPpapCreator: {}", execution);
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
        ExecutionConfiguration executionConfiguration = DocumentDAOUtility.getExecutionConfiguration(buyer,
                execution.getProcessInstance().getProcessDefinitionId(), ProcessConfiguration.RoleTypeEnum.BUYER, "PPAPREQUEST", ExecutionConfiguration.ApplicationTypeEnum.DATAADAPTER);
        String applicationURI = executionConfiguration.getExecutionUri();
        ExecutionConfiguration.ExecutionTypeEnum executionType = executionConfiguration.getExecutionType();

        // specify output variables
        PpapRequestType ppapRequestType = null;

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.ExecutionTypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            ppapRequestType  = (PpapRequestType) businessProcessApplication.createDocument(buyer, seller, content, ProcessDocumentMetadata.TypeEnum.PPAPREQUEST);
        } else if(executionType == ExecutionConfiguration.ExecutionTypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }

        // set the corresponding camunda business process variable
        execution.setVariable("ppapRequest", ppapRequestType);
    }
}