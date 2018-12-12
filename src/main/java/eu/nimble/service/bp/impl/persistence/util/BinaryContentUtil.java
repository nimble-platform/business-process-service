package eu.nimble.service.bp.impl.persistence.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.nimble.service.bp.impl.util.serialization.Serializer;
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
    public static List<String> getBinaryContentUris(Object document) {

        ObjectMapper objectMapper = Serializer.getDefaultObjectMapper();
        SimpleModule simpleModule = new SimpleModule();

        BinaryObjectSerializerGetUris binaryObjectSerializerGetUris = new BinaryObjectSerializerGetUris();

        simpleModule.addSerializer(BinaryObjectType.class, binaryObjectSerializerGetUris);
        objectMapper.registerModule(simpleModule);

        try {
            objectMapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            String msg = String.format("Failed to get binary content uris from document: %s", document.getClass());
            throw new RuntimeException(msg, e);
        }

        return binaryObjectSerializerGetUris.getListOfUris();
    }

    // removes binary contents from the document and saves them to binary content database
    public static Object removeBinaryContentFromDocument(Object document) {
        String documentContent;
        try {
            documentContent = Serializer.getDefaultObjectMapper().writeValueAsString(document);

            ObjectMapper objectMapper = Serializer.getDefaultObjectMapper();
            SimpleModule simpleModule = new SimpleModule();
            simpleModule.addDeserializer(BinaryObjectType.class, SpringBridge.getInstance().getBinaryObjectDeserializer());
            objectMapper.registerModule(simpleModule);

            document = objectMapper.readValue(documentContent, document.getClass());

        } catch (IOException e) {
            String msg = String.format("Failed to remove binary content from document: %s", document.getClass());
            throw new RuntimeException(msg, e);
        }

        return document;
    }

    // removes binary contents with the given ids from the database
    public static void removeBinaryContentFromDatabase(List<String> uris) {
        for (String uri : uris) {
            SpringBridge.getInstance().getBinaryContentService().deleteContent(uri);
        }
    }
}
