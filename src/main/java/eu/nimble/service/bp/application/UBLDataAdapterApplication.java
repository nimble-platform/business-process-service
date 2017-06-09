package eu.nimble.service.bp.application;

import eu.nimble.service.bp.impl.util.DocumentDAOUtility;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.order.ObjectFactory;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.utility.DateUtility;
import eu.nimble.utility.JAXBUtility;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yildiray on 6/5/2017.
 */
public class UBLDataAdapterApplication implements IBusinessProcessApplication {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Object createDocument(String initiatorID, String responderID, String content, ProcessDocumentMetadata.TypeEnum documentType, String responseToDocumentID) {
        if(documentType == ProcessDocumentMetadata.TypeEnum.ORDER) {
            OrderType order = UBLUtility.createOrderDocument(initiatorID, responderID, content);

            ObjectFactory factory = new ObjectFactory();
            logger.debug(" $$$ Created document: {}", JAXBUtility.serialize(order, factory.createOrder(order)));

            return order;
        } else if(documentType == ProcessDocumentMetadata.TypeEnum.ORDERRESPONSE) {
            OrderResponseSimpleType orderResponse = UBLUtility.createOrderResponse(initiatorID, responderID, content, responseToDocumentID);

            eu.nimble.service.model.ubl.orderresponsesimple.ObjectFactory factory = new eu.nimble.service.model.ubl.orderresponsesimple.ObjectFactory();
            logger.debug(" $$$ Created document: {}", JAXBUtility.serialize(orderResponse, factory.createOrderResponseSimple(orderResponse)));

            return orderResponse;
        }

        return null;
    }

    @Override
    public void saveDocument(String processInstanceId, String initiatorID, String responderID,
                            Object document, ProcessDocumentMetadata.TypeEnum documentType,
                            ProcessDocumentMetadata.StatusEnum documentStatus) {
        ProcessDocumentMetadata documentMetadata = new ProcessDocumentMetadata();
        documentMetadata.setInitiatorID(initiatorID);
        documentMetadata.setResponderID(responderID);
        documentMetadata.setProcessInstanceID(processInstanceId);
        documentMetadata.setStatus(documentStatus);
        documentMetadata.setType(documentType);

        DateTime submissionDate = new DateTime();
        documentMetadata.setSubmissionDate(DateUtility.convert(submissionDate));

        if(documentType == ProcessDocumentMetadata.TypeEnum.ORDER) {
            OrderType order = (OrderType) document;
            documentMetadata.setDocumentID(order.getID());
        } else if(documentType == ProcessDocumentMetadata.TypeEnum.ORDERRESPONSE) {
            OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) document;
            documentMetadata.setDocumentID(orderResponse.getID());
        }

        // persist the document metadata
        DocumentDAOUtility.addDocumentWithMetadata(documentMetadata, document);
    }

    @Override
    public void sendDocument(String processInstanceId, String initiatorID, String responderID,
                             Object document, ProcessDocumentMetadata.TypeEnum documentType, ProcessDocumentMetadata.StatusEnum documentStatus,
                             String initiatingDocumentID, ProcessDocumentMetadata.StatusEnum responseStatus) {
        // TODO: Send email notification to the responder...

        // if this document is a response to an initiating document, set the response code of the initiating document
        // e.g OrderResponse to an Order
        if(initiatingDocumentID != null) {
            ProcessDocumentMetadata initiatingDocumentMetadata = DocumentDAOUtility.getDocumentMetadata(initiatingDocumentID);
            initiatingDocumentMetadata.setStatus(responseStatus);
            DocumentDAOUtility.updateDocumentMetadata(initiatingDocumentMetadata);
        }
    }

}
