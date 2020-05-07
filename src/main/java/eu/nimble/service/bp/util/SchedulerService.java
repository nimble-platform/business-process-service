package eu.nimble.service.bp.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequestWithBody;
import eu.nimble.service.bp.impl.StartWithDocumentController;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.tt.OrderEPC;
import eu.nimble.service.bp.model.tt.OrderTrackingAnalysis;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.bp.util.email.IEmailSenderUtil;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DeliveryType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.OrderLineType;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.utility.HttpResponseUtil;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import feign.Response;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.xml.datatype.XMLGregorianCalendar;

@Service
public class SchedulerService implements SchedulingConfigurer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String trackingAnalysisToken = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJVU2VseVdBQzhWXzh3WTJIN3pVenNpN0dKWDRycEhHdzRkRGxRNGphYUhJIn0.eyJqdGkiOiIxZTdmYTIxYi05OGUwLTRiMmItODhiMy1lOTYzOWI4MjZhZjkiLCJleHAiOjE1NzM1NTk3MDEsIm5iZiI6MCwiaWF0IjoxNTczNTU5NjQxLCJpc3MiOiJodHRwOi8va2V5Y2xvYWs6ODA4MC9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJhdWQiOiJuaW1ibGVfY2xpZW50Iiwic3ViIjoiYmE0MzBkOWEtMjdmOS00MzkwLTg4MTMtOTQ4YjgzYjQyMmUzIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibmltYmxlX2NsaWVudCIsImF1dGhfdGltZSI6MCwic2Vzc2lvbl9zdGF0ZSI6ImEzNGU0ODkwLTU2OWEtNDgyYi1hNjI2LTdjNGU3ZDUwODJmYyIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsibmltYmxlX3VzZXIiLCJ1bWFfYXV0aG9yaXphdGlvbiIsInNhbGVzX29mZmljZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJuYW1lIjoiUXVhbiBEZW5nIiwicHJlZmVycmVkX3VzZXJuYW1lIjoiZHF1MUBiaWJhLnVuaS1icmVtZW4uZGUiLCJnaXZlbl9uYW1lIjoiUXVhbiIsImZhbWlseV9uYW1lIjoiRGVuZyIsImVtYWlsIjoiZHF1MUBiaWJhLnVuaS1icmVtZW4uZGUifQ.ZecJbZIQorfdBixaXQJHnp-vhyjwCMbPDsmWILtO45L4fXYCJZ1Dg7yrqPenN4NNXXBO72HrQsDsc7FIjTKl4MGu2vZkvDx3JfQ1AbZChApdM7NIaFdu445g9TfdF3P_14YE8aKopwpQGpFuHu_QGHkDZwewUP-jWlrdTgDX4my_upivnXMnLdnjCVmr2ocn_a_S-WlxUmMqrz2H4kxPCBcTysJkjwX_0wWXN4k1LwHhBpEuq2A_movDXyHi2mSNG11L_NI1hx2koAahp8T_1yXXvwbPPd1l0w2hDCgjrTydAmJMcQeyEwyPMwc269M9OIwkJbIKF_5qOfLxuKpVsw";
    @Autowired
    private IEmailSenderUtil emailSenderUtil;
    @Autowired
    private CredentialsUtil credentialsUtil;

    // every day at 6 am
    private String cronExpression = "0 0 6 ? * *";

    private TaskScheduler taskScheduler;

    private ScheduledFuture scheduledFuture;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.initialize();
        taskRegistrar.setScheduler(taskScheduler);

        this.taskScheduler = taskScheduler;

        this.schedule();
    }

    private void schedule(){
        scheduledFuture = this.taskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                logger.info("Running cron job to send emails to buyers for the delayed orders");
                // get all unshipped order ids
                List<String> unshippedOrderIds = ProcessDocumentMetadataDAOUtility.getUnshippedOrderIds();
                // get EPC codes for those order ids
                ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
                Response response = SpringBridge.getInstance().getDataChannelClient().getEPCCodesForOrders(credentialsUtil.getBearerToken(), unshippedOrderIds);
                List<OrderEPC> epcCodes;
                try {
                    String responseBody = HttpResponseUtil.extractBodyFromFeignClientResponse(response);
                    epcCodes = objectMapper.readValue(responseBody,new TypeReference<List<OrderEPC>>(){});
                } catch (IOException e) {
                    logger.error("Failed to get EPC codes for unshipped orders while running cron job",e);
                    return;
                }

                for (OrderEPC epcCode : epcCodes) {
                    OrderType order = (OrderType) DocumentPersistenceUtility.getUBLDocument(epcCode.getOrderId(), DocumentType.ORDER);
                    OrderResponseSimpleType orderResponse = DocumentPersistenceUtility.getOrderResponseDocumentByOrderId(epcCode.getOrderId());

                    // only consider the order having a Production Process Template Json Document
                    String processTemplateUri = getProcessTemplateUri(orderResponse.getAdditionalDocumentReference());

                    // if we have the corresponding process template, then use it to get expected production end time
                    if(processTemplateUri != null){
                        String endpoint = SpringBridge.getInstance().getGenericConfig().getTrackingAnalysisServiceUrl() + "/estimateProcEndTimeWithGivenDuration";
                        try {
                            BinaryObjectType processTemplateBinaryObject = new BinaryContentService().retrieveContent(processTemplateUri);

                            String processTemplate = new String(processTemplateBinaryObject.getValue());

                            HttpRequestWithBody httpRequestWithBody = (HttpRequestWithBody) Unirest.post(endpoint)
                                    .queryString("itemIDList",epcCode.getCodes())
                                    .header("Content-Type", "application/json")
                                    .header("accept", "*/*")
                                    .header("Authorization",trackingAnalysisToken);
                            HttpResponse<String> httpResponse = httpRequestWithBody.body(processTemplate).asString();
                            List<OrderTrackingAnalysis> orderTrackingAnalyses = objectMapper.readValue(httpResponse.getBody(),new TypeReference<List<OrderTrackingAnalysis>>(){});

                            if(orderTrackingAnalyses.size() > 0){
                                DateTimeFormatter bpFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

                                // get max estimated delivery date
                                long maxEstimatedDeliveryDateLong = Long.parseLong(orderTrackingAnalyses.get(0).getProductionEndTime());
                                for (int i = 1; i < orderTrackingAnalyses.size(); i++) {
                                    if(Long.parseLong(orderTrackingAnalyses.get(i).getProductionEndTime()) > maxEstimatedDeliveryDateLong){
                                        maxEstimatedDeliveryDateLong = Long.parseLong(orderTrackingAnalyses.get(i).getProductionEndTime());
                                    }
                                }
                                Date maxEstimatedDeliveryDate = new Date(maxEstimatedDeliveryDateLong);

                                // get the date when the order is accepted
                                ProcessDocumentMetadata processDocumentMetadata = ProcessDocumentMetadataDAOUtility.getOrderResponseMetadataByOrderId(epcCode.getOrderId());
                                DateTime submissionDate = bpFormatter.parseDateTime(processDocumentMetadata.getSubmissionDate());

                                // get the minimum promised delivery date
                                Date minPromisedDeliveryDate = getPromisedDeliveryDate(order.getOrderLine().get(0),submissionDate);
                                for(int i = 1; i < order.getOrderLine().size(); i++){
                                    Date promisedDeliveryDate = getPromisedDeliveryDate(order.getOrderLine().get(i),submissionDate);
                                    if(promisedDeliveryDate != null){
                                        if(minPromisedDeliveryDate == null){
                                            minPromisedDeliveryDate = promisedDeliveryDate;
                                        }
                                        else if(promisedDeliveryDate.compareTo(minPromisedDeliveryDate) < 0){
                                            minPromisedDeliveryDate = promisedDeliveryDate;
                                        }
                                    }
                                }

                                // send an email to buyer if the seller misses the promised delivery date
                                if(minPromisedDeliveryDate != null && maxEstimatedDeliveryDate.compareTo(minPromisedDeliveryDate) > 0){
                                    emailSenderUtil.sendNewDeliveryDateEmail(credentialsUtil.getBearerToken(),maxEstimatedDeliveryDate,order.getBuyerPartyId(),order.getBuyerParty().getFederationInstanceID(),order.getSellerParty().getFederationInstanceID(),processDocumentMetadata.getProcessInstanceID());
                                }
                            }

                        } catch (Exception e) {
                            logger.error("Failed to get expected production end date for order {} while running cron job",epcCode.getOrderId(),e);
                        }
                    }
                }
                logger.info("Completed the cron job to send emails to buyers for delayed orders");
            }
        }, new Trigger() {
            @Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
                CronTrigger trigger = new CronTrigger(cronExpression);
                return trigger.nextExecutionTime(triggerContext);
            }
        });
    }

    // getter & setter for cron expression
    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
        scheduledFuture.cancel(true);
        // if no cron expression is specified, do not start scheduler
        if(this.cronExpression != null){
            this.schedule();
        }
    }

    // helper functions

    private String getProcessTemplateUri(List<DocumentReferenceType> documentReferences){
        for (DocumentReferenceType documentReference : documentReferences) {
            if(documentReference.getDocumentType() != null && documentReference.getDocumentType().contentEquals("PRODUCTIONTEMPLATE")){
                return documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri();
            }
        }
        return null;
    }

    private Date getPromisedDeliveryDate(OrderLineType orderLine, DateTime submissionDate){
        Date promisedDeliveryDate = null;
        // get delivery date for order
        List<DeliveryType> deliveryTypes = orderLine.getLineItem().getDelivery();
        // product has multiple delivery date
        // get the minimum delivery date
        if(deliveryTypes.size() > 1 || deliveryTypes.get(0).getRequestedDeliveryPeriod().getEndDate() != null){
            XMLGregorianCalendar minPromisedDeliveryDate = deliveryTypes.get(0).getRequestedDeliveryPeriod().getEndDate();
            for (int i = 1; i < deliveryTypes.size(); i++) {
                if(deliveryTypes.get(i).getRequestedDeliveryPeriod().getEndDate().compare(minPromisedDeliveryDate) < 0){
                    minPromisedDeliveryDate = deliveryTypes.get(i).getRequestedDeliveryPeriod().getEndDate();
                }
            }

            promisedDeliveryDate = minPromisedDeliveryDate.toGregorianCalendar().getTime();
        }
        // product has delivery period
        // calculate the exact delivery date
        else if(deliveryTypes.get(0).getRequestedDeliveryPeriod().getDurationMeasure() != null){
            String unit = deliveryTypes.get(0).getRequestedDeliveryPeriod().getDurationMeasure().getUnitCode();
            BigDecimal value = deliveryTypes.get(0).getRequestedDeliveryPeriod().getDurationMeasure().getValue();

            if(value != null){
                if(unit.contentEquals("week(s)")){
                    promisedDeliveryDate = submissionDate.plusWeeks(value.intValue()).toDate();
                } else if(unit.contentEquals("day(s)")){
                    promisedDeliveryDate = submissionDate.plusDays(value.intValue()).toDate();
                }
            }

        }
        return promisedDeliveryDate;
    }
}