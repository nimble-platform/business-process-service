package eu.nimble.service.bp.processor.negotiation;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.util.persistence.DocumentDAOUtility;

import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by yildiray on 6/29/2017.
 */
public class DefaultQuotationCreator  implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ DefaultQuotationCreator: {}", execution);
        final Map<String, Object> variables = execution.getVariables();
        // for debug purposes
        for (String key: variables.keySet()) {
            logger.debug(" $$$ Variable name {}, value {}", key, variables.get(key));
        }
        // get input variables
        String buyer = variables.get("responderID").toString();
        String seller = variables.get("initiatorID").toString();
        RequestForQuotationType requestForQuotation = (RequestForQuotationType) variables.get("requestForQuotation");
        String content = variables.get("content").toString();

        // get application execution configuration of the party
        ExecutionConfiguration executionConfiguration = DocumentDAOUtility.getExecutionConfiguration(seller,
                execution.getProcessInstance().getProcessDefinitionId(), ProcessConfiguration.RoleTypeEnum.SELLER, "QUOTATION", ExecutionConfiguration.ApplicationTypeEnum.DATAADAPTER);
        String applicationURI = executionConfiguration.getExecutionUri();
        ExecutionConfiguration.ExecutionTypeEnum executionType = executionConfiguration.getExecutionType();

        // specify output variables
        QuotationType quotation = null;

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.ExecutionTypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            // Note to the direction of the document (here it is from seller to buyer)
            quotation  = (QuotationType) businessProcessApplication.createDocument(seller, buyer, content, ProcessDocumentMetadata.TypeEnum.QUOTATION);

        } else if(executionType == ExecutionConfiguration.ExecutionTypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }

        // set the corresponding camunda business process variable
        execution.setVariable("quotation", quotation);
    }
}