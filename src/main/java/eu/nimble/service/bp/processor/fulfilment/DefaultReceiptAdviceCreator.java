package eu.nimble.service.bp.processor.fulfilment;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentDAOUtility;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by yildiray on 6/29/2017.
 */
public class DefaultReceiptAdviceCreator implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ DefaultReceiptAdviceCreator: {}", execution);
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
                execution.getProcessInstance().getProcessDefinitionId(), ProcessConfiguration.RoleTypeEnum.BUYER, "RECEIPTADVICE", ExecutionConfiguration.ApplicationTypeEnum.DATAADAPTER);
        String applicationURI = executionConfiguration.getExecutionUri();
        ExecutionConfiguration.ExecutionTypeEnum executionType = executionConfiguration.getExecutionType();

        // specify output variables
        ReceiptAdviceType receiptAdvice = null;

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.ExecutionTypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            receiptAdvice  = (ReceiptAdviceType) businessProcessApplication.createDocument(buyer, seller, content, ProcessDocumentMetadata.TypeEnum.RECEIPTADVICE);
        } else if(executionType == ExecutionConfiguration.ExecutionTypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }

        // set the corresponding camunda business process variable
        execution.setVariable("receiptAdvice", receiptAdvice);
    }
}