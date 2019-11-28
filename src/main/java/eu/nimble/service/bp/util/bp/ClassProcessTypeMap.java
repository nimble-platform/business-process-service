package eu.nimble.service.bp.util.bp;

import eu.nimble.service.model.ubl.despatchadvice.DespatchAdviceType;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.service.model.ubl.iteminformationresponse.ItemInformationResponseType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.ppaprequest.PpapRequestType;
import eu.nimble.service.model.ubl.ppapresponse.PpapResponseType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.service.model.ubl.transportexecutionplan.TransportExecutionPlanType;
import eu.nimble.service.model.ubl.transportexecutionplanrequest.TransportExecutionPlanRequestType;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to find corresponding process type for the document
 */
public class ClassProcessTypeMap {

    public static final String CAMUNDA_PROCESS_ID_ITEM_INFORMATION_REQUEST = "Item_Information_Request";
    public static final String CAMUNDA_PROCESS_ID_PPAP = "Ppap";
    public static final String CAMUNDA_PROCESS_ID_NEGOTIATION = "Negotiation";
    public static final String CAMUNDA_PROCESS_ID_ORDER = "Order";
    public static final String CAMUNDA_PROCESS_ID_FULFILMENT = "Fulfilment";
    public static final String CAMUNDA_PROCESS_ID_TRANSPORT_EXECUTION_PLAN = "Transport_Execution_Plan";

    private static Map<Class, String> classProcessTypeMap = new HashMap<>();

    static {
        classProcessTypeMap.put(OrderResponseSimpleType.class, CAMUNDA_PROCESS_ID_ORDER);
        classProcessTypeMap.put(OrderType.class, CAMUNDA_PROCESS_ID_ORDER);
        classProcessTypeMap.put(RequestForQuotationType.class, CAMUNDA_PROCESS_ID_NEGOTIATION);
        classProcessTypeMap.put(QuotationType.class, CAMUNDA_PROCESS_ID_NEGOTIATION);
        classProcessTypeMap.put(DespatchAdviceType.class, CAMUNDA_PROCESS_ID_FULFILMENT);
        classProcessTypeMap.put(ReceiptAdviceType.class, CAMUNDA_PROCESS_ID_FULFILMENT);
        classProcessTypeMap.put(TransportExecutionPlanRequestType.class, CAMUNDA_PROCESS_ID_TRANSPORT_EXECUTION_PLAN);
        classProcessTypeMap.put(TransportExecutionPlanType.class, CAMUNDA_PROCESS_ID_TRANSPORT_EXECUTION_PLAN);
        classProcessTypeMap.put(PpapRequestType.class, CAMUNDA_PROCESS_ID_PPAP);
        classProcessTypeMap.put(PpapResponseType.class, CAMUNDA_PROCESS_ID_PPAP);
        classProcessTypeMap.put(ItemInformationRequestType.class, CAMUNDA_PROCESS_ID_ITEM_INFORMATION_REQUEST);
        classProcessTypeMap.put(ItemInformationResponseType.class, CAMUNDA_PROCESS_ID_ITEM_INFORMATION_REQUEST);
    }

    public static String getProcessType(Class klass){
        return classProcessTypeMap.get(klass);
    }
}
