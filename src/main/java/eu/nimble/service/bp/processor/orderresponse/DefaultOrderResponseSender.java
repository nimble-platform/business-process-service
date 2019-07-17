package eu.nimble.service.bp.processor.orderresponse;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.util.persistence.bp.ExecutionConfigurationDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.serialization.MixInIgnoreProperties;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.utility.JsonSerializationUtility;
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
        logger.debug(JsonSerializationUtility.getObjectMapperWithMixIn(Map.class, MixInIgnoreProperties.class).writeValueAsString(variables));

        // get process instance id
        String processInstanceId = execution.getProcessInstance().getProcessInstanceId();

        // get input variables
        String buyer = variables.get("responderID").toString();
        String seller = variables.get("initiatorID").toString();
        String processContextId = variables.get("processContextId").toString();
        OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) variables.get("orderResponse");
        OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument((String) variables.get("initialDocumentID"));

        // get application execution configuration
        ExecutionConfiguration executionConfiguration = ExecutionConfigurationDAOUtility.getExecutionConfiguration(seller,
                execution.getProcessInstance().getProcessDefinitionId(), ProcessConfiguration.RoleTypeEnum.SELLER, "ORDERRESPONSE",
                ExecutionConfiguration.ApplicationTypeEnum.DATACHANNEL);
        String applicationURI = executionConfiguration.getExecutionUri();
        ExecutionConfiguration.ExecutionTypeEnum executionType = executionConfiguration.getExecutionType();

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.ExecutionTypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            // note the direction of the document (here it is from seller to buyer)
            businessProcessApplication.sendDocument(processContextId,processInstanceId, seller, buyer, orderResponse);

//            // create a data channel if the order is approved
//            createDataChannel(order, orderResponse, buyer, seller, processInstanceId, (String) variables.get("bearer_token"));

        } else if(executionType == ExecutionConfiguration.ExecutionTypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }
        // remove variables
        String initialDocumentID = (String) execution.getVariable("initialDocumentID");
        execution.removeVariables();
        execution.setVariable("initialDocumentID",initialDocumentID);
        execution.setVariable("responseDocumentID",orderResponse.getID());
    }

//    private boolean needToCreateDataChannel(OrderType order, OrderResponseSimpleType orderResponse) {
//        boolean dataMonitoringDemanded = false;
//        ContractType contract = ContractGenerator.getNonTermOrConditionContract(order);
//        if(contract != null){
//            List<ClauseType> clauses = contract.getClause();
//            for(ClauseType clause : clauses) {
//                if(clause.getType().contentEquals(eu.nimble.service.model.ubl.extension.ClauseType.DOCUMENT.toString())) {
//                    DocumentClauseType docClause = (DocumentClauseType) clause;
//                    if(docClause.getClauseDocumentRef().getDocumentType().contentEquals(DocumentType.QUOTATION.toString())) {
//                        QuotationType quotation = (QuotationType) DocumentPersistenceUtility.getUBLDocument(docClause.getClauseDocumentRef().getID(), DocumentType.QUOTATION);
//                        if (quotation.isDataMonitoringPromised()) {
//                            dataMonitoringDemanded = true;
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//
//        return dataMonitoringDemanded && orderResponse.isAcceptedIndicator();
//    }

//    private void createDataChannel(OrderType order, OrderResponseSimpleType orderResponse, String buyerId, String sellerId, String processInstanceId, String bearerToken) {
//        boolean createDataChannel = needToCreateDataChannel(order, orderResponse);
//        if(!createDataChannel) {
//            return;
//        }
//
//        // create url
//        URL dataChannelServiceUrl;
//        String dataChannelServiceUrlStr = null;
//        try {
//            GenericConfig config = SpringBridge.getInstance().getGenericConfig();
//            dataChannelServiceUrlStr = config.getDataChannelServiceUrl();
//            dataChannelServiceUrl = new URL(dataChannelServiceUrlStr + "/channel/");
//        } catch (IOException e) {
//            logger.error("Failed to create a URL from {}", dataChannelServiceUrlStr, e);
//            return;
//        }
//
//        HttpURLConnection conn;
//        try {
//            conn = (HttpURLConnection) dataChannelServiceUrl.openConnection();
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/json");
//            conn.setRequestProperty("Accept", "application/json");
//            conn.setRequestProperty("Authorization", bearerToken);
//            conn.setDoOutput(true);
//
//            OutputStream os = conn.getOutputStream();
//            CreateChannel.Request request = new CreateChannel.Request(buyerId, sellerId, String.format("Data channel for product %s", order.getOrderLine().get(0).getLineItem().getItem().getName().get(0).getValue()), processInstanceId);
//
//            JsonSerializationUtility.getObjectMapper().writeValue(os, request);
//            os.flush();
//
//            logger.info("Data channel request has been sent for processInstanceId: {}, buyerId: {}, sellerId: {}, received HTTP response: {}", processInstanceId, buyerId, sellerId, conn.getResponseCode());
//            if (conn.getResponseCode() != 200) {
//                InputStream error = conn.getErrorStream();
//                if(error != null) {
//                    String msg = IOUtils.toString(error);
//                    logger.error("Error from data channel service for processInstanceId: {}, buyerId: {}, sellerId: {}. Response code: {}, error: {}", processInstanceId, buyerId, sellerId, conn.getResponseCode(), msg);
//                } else {
//                    logger.error("Error from data channel service for processInstanceId: {}, buyerId: {}, sellerId: {}. Response code: {}", processInstanceId, buyerId, sellerId, conn.getResponseCode());
//                }
//
//            }
//            conn.disconnect();
//        } catch (IOException e) {
//            logger.error("Failed to create data channel for processInstanceId: {}, buyerId: {}, sellerId: {}", processInstanceId, buyerId, sellerId, e);
//        }
//    }
}
