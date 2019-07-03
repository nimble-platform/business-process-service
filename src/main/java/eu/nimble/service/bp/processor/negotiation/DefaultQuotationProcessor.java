package eu.nimble.service.bp.processor.negotiation;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.util.persistence.bp.ExecutionConfigurationDAOUtility;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.bp.serialization.MixInIgnoreProperties;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.TradingTermType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
import eu.nimble.service.model.ubl.digitalagreement.DigitalAgreementType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.utility.JsonSerializationUtility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by yildiray on 6/29/2017.
 */
public class DefaultQuotationProcessor  implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ DefaultQuotationProcessor: {}", execution);
        final Map<String, Object> variables = execution.getVariables();
        // for debug purposes
        logger.debug(JsonSerializationUtility.getObjectMapperWithMixIn(Map.class, MixInIgnoreProperties.class).writeValueAsString(variables));

        // get process instance id
        String processInstanceId = execution.getProcessInstance().getProcessInstanceId();

        // get input variables
        String buyer = variables.get("responderID").toString();
        String seller = variables.get("initiatorID").toString();
        String creatorUserID = variables.get("creatorUserID").toString();
        String processContextId = variables.get("processContextId").toString();
        List<String> relatedProducts = (List<String>) variables.get("relatedProducts");
        List<String> relatedProductCategories = (List<String>) variables.get("relatedProductCategories");
        QuotationType quotation = (QuotationType) variables.get("quotation");

        // get application execution configuration
        ExecutionConfiguration executionConfiguration = ExecutionConfigurationDAOUtility.getExecutionConfiguration(seller,
                execution.getProcessInstance().getProcessDefinitionId(), ProcessConfiguration.RoleTypeEnum.SELLER,"QUOTATION",
                ExecutionConfiguration.ApplicationTypeEnum.DATAPROCESSOR);
        String applicationURI = executionConfiguration.getExecutionUri();
        ExecutionConfiguration.ExecutionTypeEnum executionType = executionConfiguration.getExecutionType();

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.ExecutionTypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            // NOTE: Pay attention to the direction of the document. Here it is from seller to buyer
            businessProcessApplication.saveDocument(processContextId,processInstanceId, seller, buyer,creatorUserID, quotation, relatedProducts, relatedProductCategories);

            // check the conditions related to frame contracts
            createOrUpdateFrameContract(quotation);

        } else if(executionType == ExecutionConfiguration.ExecutionTypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }
    }

    /**
     * Creates frame contract if the quotation includes a term related to frame contract duration and if the quotation
     * is accepted. If there exists a frame contract already update its duration if it's changed.
     */
    private DigitalAgreementType createOrUpdateFrameContract(QuotationType quotation) {
        TradingTermType frameContractDurationTerm = getFrameContractTerm(quotation);
        // there is no terms regarding the frame contract duration
        if(frameContractDurationTerm == null) {
            return null;
        }

        String sellerId = quotation.getSellerSupplierParty().getParty().getPartyIdentification().get(0).getID();
        String buyerId = quotation.getBuyerCustomerParty().getParty().getPartyIdentification().get(0).getID();
        ItemType item = quotation.getQuotationLine().get(0).getLineItem().getItem();
        QuantityType duration = frameContractDurationTerm.getValue().getValueQuantity().get(0);

        DigitalAgreementType frameContract = SpringBridge.getInstance().getFrameContractService().createOrUpdateFrameContract(sellerId, buyerId, item, duration, quotation.getID());
        return frameContract;
    }

    private TradingTermType getFrameContractTerm(QuotationType quotation) {
        List<TradingTermType> tradingTerms = quotation.getTradingTerms();
        for(TradingTermType term : tradingTerms) {
            if (term.getID().contentEquals("FRAME_CONTRACT_DURATION")) {
                return term;
            }
        }
        return null;
    }
}