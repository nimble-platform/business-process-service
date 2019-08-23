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

    private static Map<Class, String> classProcessTypeMap = new HashMap<>();

    static {
        classProcessTypeMap.put(OrderResponseSimpleType.class, "Order");
        classProcessTypeMap.put(OrderType.class, "Order");
        classProcessTypeMap.put(RequestForQuotationType.class, "Negotiation");
        classProcessTypeMap.put(QuotationType.class, "Negotiation");
        classProcessTypeMap.put(DespatchAdviceType.class, "Fulfilment");
        classProcessTypeMap.put(ReceiptAdviceType.class, "Fulfilment");
        classProcessTypeMap.put(TransportExecutionPlanRequestType.class, "Transport_Execution_Plan");
        classProcessTypeMap.put(TransportExecutionPlanType.class, "Transport_Execution_Plan");
        classProcessTypeMap.put(PpapRequestType.class, "Ppap");
        classProcessTypeMap.put(PpapResponseType.class,"Ppap");
        classProcessTypeMap.put(ItemInformationRequestType.class, "Item_Information_Request");
        classProcessTypeMap.put(ItemInformationResponseType.class, "Item_Information_Request");
    }

    public static String getProcessType(Class klass){
        return classProcessTypeMap.get(klass);
    }
}
