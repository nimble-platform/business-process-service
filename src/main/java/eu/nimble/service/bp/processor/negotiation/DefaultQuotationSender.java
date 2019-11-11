package eu.nimble.service.bp.processor.negotiation;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.util.HttpResponseUtil;
import eu.nimble.service.bp.util.persistence.bp.ExecutionConfigurationDAOUtility;
import eu.nimble.service.bp.util.persistence.bp.ProcessInstanceGroupDAOUtility;
import eu.nimble.service.bp.util.serialization.MixInIgnoreProperties;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QuotationLineType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import feign.Response;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import request.CreateChannel;

import java.util.List;
import java.util.Map;

/**
 * Created by yildiray on 6/29/2017.
 */
public class DefaultQuotationSender  implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ DefaultQuotationSender: {}", execution);
        final Map<String, Object> variables = execution.getVariables();
        // for debug purposes
        logger.debug(JsonSerializationUtility.getObjectMapperWithMixIn(Map.class, MixInIgnoreProperties.class).writeValueAsString(variables));

        // get process instance id
        String processInstanceId = execution.getProcessInstance().getProcessInstanceId();

        // get input variables
        String buyer = variables.get("responderID").toString();
        String seller = variables.get("initiatorID").toString();
        String processContextId = variables.get("processContextId").toString();
        String bearerToken = (String) variables.get("bearer_token");
        QuotationType quotation = (QuotationType) variables.get("quotation");

        // get application execution configuration
        ExecutionConfiguration executionConfiguration = ExecutionConfigurationDAOUtility.getExecutionConfiguration(seller,
                execution.getProcessInstance().getProcessDefinitionId(), ProcessConfiguration.RoleTypeEnum.SELLER, "QUOTATION",
                ExecutionConfiguration.ApplicationTypeEnum.DATACHANNEL);
        String applicationURI = executionConfiguration.getExecutionUri();
        ExecutionConfiguration.ExecutionTypeEnum executionType = executionConfiguration.getExecutionType();

        // Call that configured application with the variables
        if(executionType == ExecutionConfiguration.ExecutionTypeEnum.JAVA) {
            Class applicationClass = Class.forName(applicationURI);
            Object instance = applicationClass.newInstance();

            IBusinessProcessApplication businessProcessApplication = (IBusinessProcessApplication) instance;

            // note the direction of the document (here it is from seller to buyer)
            businessProcessApplication.sendDocument(processContextId,processInstanceId, seller, buyer, quotation);

            // create the data channel
            // TODO: this method creates a data channel only for one of the products included in the negotiation. Update it later
            createDataChannel(quotation.getDocumentStatusCode().getName(),quotation.getQuotationLine(), bearerToken, buyer, seller, processInstanceId, processContextId);
        } else if(executionType == ExecutionConfiguration.ExecutionTypeEnum.MICROSERVICE) {
            // TODO: How to call a microservice
        } else {
            // TODO: think other types of execution possibilities
        }
        // remove variables
        String initialDocumentID = (String) execution.getVariable("initialDocumentID");
        execution.removeVariables();
        execution.setVariable("initialDocumentID",initialDocumentID);
        execution.setVariable("responseDocumentID",quotation.getID());
    }

    /**
     * Creates the data channel for Negotiation process if the following conditions are satisfied:
     *  - there is no data channel created for the process instance groups which contain the specified process instance
     *  - data monitoring is promised and status of the negotiation is not equal to 'Rejected'
     */
    private void createDataChannel(String status, List<QuotationLineType> quotationLines, String bearerToken, String buyerId, String sellerId,String processInstanceId, String processContextId){
        // get the process instance groups containing the given process instance
        List<ProcessInstanceGroupDAO> processInstanceGroupDAOs = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAOs(processInstanceId, BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId).getBpRepository());

        for (QuotationLineType quotationLine : quotationLines) {
            boolean createDataChannel = !status.contentEquals("Rejected") && quotationLine.getLineItem().isDataMonitoringRequested();
            if(!createDataChannel) {
                continue;
            }

            String productName = quotationLine.getLineItem().getItem().getName().get(0).getValue();

            logger.info("Creating data channel for processInstanceId: {}, buyerId: {}, sellerId: {}", processInstanceId, buyerId, sellerId);

            try {
                // create the request
                CreateChannel.Request request = new CreateChannel.Request(buyerId, sellerId, String.format("Data channel for product %s", productName), processInstanceId,"");
                String serializedRequest = JsonSerializationUtility.getObjectMapper().writeValueAsString(request);

                Response response = SpringBridge.getInstance().getDataChannelClient().createChannel(bearerToken,serializedRequest);
                String responseBody = HttpResponseUtil.extractBodyFromFeignClientResponse(response);

                if(response.status() != 200){
                    logger.error("Error from data channel service for processInstanceId: {}, buyerId: {}, sellerId: {}. Response code: {}", processInstanceId, buyerId, sellerId, response.status());
                }
                else{
                    logger.info("Created data channel for processInstanceId: {}, buyerId: {}, sellerId: {} successfully", processInstanceId, buyerId, sellerId);

                    GenericJPARepository repo = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId).getBpRepository();

                    CreateChannel.Response channelResponse = JsonSerializationUtility.getObjectMapper().readValue(responseBody,CreateChannel.Response.class);
                    // set data channel id for each process instance group
                    for (ProcessInstanceGroupDAO processInstanceGroupDAO : processInstanceGroupDAOs) {
                        processInstanceGroupDAO.setDataChannelId(channelResponse.getChannelID());
                        // update process instance group dao
                        repo.updateEntity(processInstanceGroupDAO);
                    }
                }
            }
            catch (Exception e){
                logger.error("Failed to create data channel for processInstanceId: {}, buyerId: {}, sellerId: {}", processInstanceId, buyerId, sellerId, e);
            }
        }

    }
}