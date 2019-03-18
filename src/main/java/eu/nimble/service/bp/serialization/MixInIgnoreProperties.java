package eu.nimble.service.bp.serialization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value = {"content", "despatchAdvice", "receiptAdvice", "itemInformationRequest", "itemInformationResponse", "quotation", "requestForQuotation", "order", "orderResponse", "ppapRequest", "ppapResponse", "transportExecutionPlan", "transportExecutionPlanRequest"})
public class MixInIgnoreProperties {
}