package eu.nimble.service.bp.util.bp;

import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by suat on 03-Jan-19.
 */
public class DocumentEnumClassMapper {
    private static Logger logger = LoggerFactory.getLogger(DocumentPersistenceUtility.class);
    private static Map<DocumentType, Class> documentClassEnumMap = new HashMap<>();

    static {
        documentClassEnumMap.put(DocumentType.CATALOGUE, CatalogueType.class);
        documentClassEnumMap.put(DocumentType.ORDER, OrderType.class);
        documentClassEnumMap.put(DocumentType.ORDERRESPONSESIMPLE, OrderResponseSimpleType.class);
        documentClassEnumMap.put(DocumentType.REQUESTFORQUOTATION, RequestForQuotationType.class);
        documentClassEnumMap.put(DocumentType.QUOTATION, QuotationType.class);
        documentClassEnumMap.put(DocumentType.DESPATCHADVICE, DespatchAdviceType.class);
        documentClassEnumMap.put(DocumentType.RECEIPTADVICE, ReceiptAdviceType.class);
        documentClassEnumMap.put(DocumentType.TRANSPORTEXECUTIONPLANREQUEST, TransportExecutionPlanRequestType.class);
        documentClassEnumMap.put(DocumentType.TRANSPORTEXECUTIONPLAN, TransportExecutionPlanType.class);
        documentClassEnumMap.put(DocumentType.PPAPREQUEST, PpapRequestType.class);
        documentClassEnumMap.put(DocumentType.PPAPRESPONSE, PpapResponseType.class);
        documentClassEnumMap.put(DocumentType.ITEMINFORMATIONREQUEST, ItemInformationRequestType.class);
        documentClassEnumMap.put(DocumentType.ITEMINFORMATIONRESPONSE, ItemInformationResponseType.class);
    }

    public static Class getDocumentClass(DocumentType documentType) {
        Class klass = documentClassEnumMap.get(documentType);
        if(klass == null) {
            String msg = String.format("Unknown document type: %s", documentType.toString());
            logger.warn(msg);
            throw new RuntimeException(msg);
        }
        return klass;
    }

    public static DocumentType getDocumentTypeForClass(Class klass) {
        for (Map.Entry entry : documentClassEnumMap.entrySet()) {
            if (Objects.equals(klass, entry.getValue())) {
                return (DocumentType) entry.getKey();
            }
        }
        String msg = String.format("Unknown class: %s", klass.getName());
        logger.warn(msg);
        throw new RuntimeException(msg);
    }

    public static boolean isBusinessProcessDocument(Class klass) {
        // although documentClassEnumMap contains CatalogueType.class, it's not a business process document type
        // therefore,we check it first
        if(Objects.equals(klass, CatalogueType.class)){
            return false;
        }
        for (Map.Entry entry : documentClassEnumMap.entrySet()) {
            if (Objects.equals(klass, entry.getValue())) {
                return true;
            }
        }
        return false;
    }
}
