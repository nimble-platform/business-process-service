package eu.nimble.service.bp.processor.negotiation;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.util.persistence.bp.ExecutionConfigurationDAOUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.bp.util.serialization.MixInIgnoreProperties;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ItemType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QuotationLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.TradingTermType;
import eu.nimble.service.model.ubl.commonbasiccomponents.QuantityType;
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
        String initiatorFederationId = variables.get("initiatorFederationId").toString();
        String responderFederationId = variables.get("responderFederationId").toString();
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
            businessProcessApplication.saveDocument(processContextId,processInstanceId, seller, buyer,creatorUserID, quotation, relatedProducts, relatedProductCategories, initiatorFederationId, responderFederationId);

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
    private void createOrUpdateFrameContract(QuotationType quotation) {

        // if the quotation is rejected, do not create/update the frame contract
        if(quotation.getDocumentStatusCode().getName().contentEquals("Rejected")){
            return;
        }

        String sellerId = quotation.getSellerSupplierParty().getParty().getPartyIdentification().get(0).getID();
        String sellerFederationId = quotation.getSellerSupplierParty().getParty().getFederationInstanceID();
        String buyerId = quotation.getBuyerCustomerParty().getParty().getPartyIdentification().get(0).getID();
        String buyerFederationId = quotation.getBuyerCustomerParty().getParty().getFederationInstanceID();

        for (QuotationLineType quotationLine: quotation.getQuotationLine()) {
            List<TradingTermType> tradingTermTypes = quotationLine.getLineItem().getTradingTerms();
            for(TradingTermType term : tradingTermTypes) {
                if (term.getID().contentEquals("FRAME_CONTRACT_DURATION")) {
                    ItemType item = quotationLine.getLineItem().getItem();
                    QuantityType duration = term.getValue().getValueQuantity().get(0);

                    SpringBridge.getInstance().getFrameContractService().createOrUpdateFrameContract(sellerId, buyerId,sellerFederationId,buyerFederationId, item, duration, quotation.getID());
                    break;
                }
            }
        }
    }

}