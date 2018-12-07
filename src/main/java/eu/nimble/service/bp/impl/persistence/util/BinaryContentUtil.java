package eu.nimble.service.bp.impl.persistence.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonbasiccomponents.BinaryObjectType;
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
import eu.nimble.utility.persistence.binary.BinaryObjectSerializerGetUris;

import java.io.IOException;
import java.util.List;

public class BinaryContentUtil {

    // removes binary contents of the given document from the database
    public static void removeBinaryContentFromDatabase(Object document) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BinaryObjectType.class, SpringBridge.getInstance().getBinaryObjectSerializerDelete());
        objectMapper.registerModule(simpleModule);

        objectMapper.writeValueAsString(document);
    }

    // gets uris of binary contents of the given document
    public static List<String> getBinaryContentUris(Object document) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();

        BinaryObjectSerializerGetUris binaryObjectSerializerGetUris = new BinaryObjectSerializerGetUris();

        simpleModule.addSerializer(BinaryObjectType.class, binaryObjectSerializerGetUris);
        objectMapper.registerModule(simpleModule);

        objectMapper.writeValueAsString(document);

        return binaryObjectSerializerGetUris.getListOfUris();
    }

    // removes binary contents from the document and saves them to binary content database
    public static Object removeBinaryContentFromDocument(Object document) throws IOException {
        String documentContent = new ObjectMapper().writeValueAsString(document);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(BinaryObjectType.class, SpringBridge.getInstance().getBinaryObjectDeserializer());
        objectMapper.registerModule(simpleModule);

        if (document instanceof OrderType) {
            return objectMapper.readValue(documentContent, OrderType.class);
        } else if (document instanceof OrderResponseSimpleType) {
            return objectMapper.readValue(documentContent, OrderResponseSimpleType.class);
        } else if (document instanceof PpapRequestType) {
            return objectMapper.readValue(documentContent, PpapRequestType.class);
        } else if (document instanceof PpapResponseType) {
            return objectMapper.readValue(documentContent, PpapResponseType.class);
        } else if (document instanceof RequestForQuotationType) {
            return objectMapper.readValue(documentContent, RequestForQuotationType.class);
        } else if (document instanceof QuotationType) {
            return objectMapper.readValue(documentContent, QuotationType.class);
        } else if (document instanceof DespatchAdviceType) {
            return objectMapper.readValue(documentContent, DespatchAdviceType.class);
        } else if (document instanceof ReceiptAdviceType) {
            return objectMapper.readValue(documentContent, ReceiptAdviceType.class);
        } else if (document instanceof TransportExecutionPlanRequestType) {
            return objectMapper.readValue(documentContent, TransportExecutionPlanRequestType.class);
        } else if (document instanceof TransportExecutionPlanType) {
            return objectMapper.readValue(documentContent, TransportExecutionPlanType.class);
        } else if (document instanceof ItemInformationRequestType) {
            return objectMapper.readValue(documentContent, ItemInformationRequestType.class);
        } else if (document instanceof ItemInformationResponseType) {
            return objectMapper.readValue(documentContent, ItemInformationResponseType.class);
        }
        return null;
    }

    // removes binary contents with the given ids from the database
    public static void removeBinaryContentFromDatabase(List<String> uris) {
        for (String uri : uris) {
            SpringBridge.getInstance().getBinaryContentService().deleteContent(uri);
        }
    }
}
