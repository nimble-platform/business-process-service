package eu.nimble.service.bp.application.ubl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.util.persistence.DocumentDAOUtility;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.order.ObjectFactory;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.DateUtility;
import eu.nimble.utility.JAXBUtility;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by yildiray on 6/5/2017.
 */
public class UBLDataAdapterApplication implements IBusinessProcessApplication {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Object createDocument(String initiatorID, String responderID, String content, ProcessDocumentMetadata.TypeEnum documentType) {
        ObjectMapper mapper = new ObjectMapper();

        if(documentType == ProcessDocumentMetadata.TypeEnum.ORDER) {
            try {
                OrderType order = mapper.readValue(content, OrderType.class);

                ObjectFactory factory = new ObjectFactory();
                logger.debug(" $$$ Created document: {}", JAXBUtility.serialize(order, factory.createOrder(order)));

                return order;
            } catch (IOException e) {
                logger.error("", e);
            }
        } else if(documentType == ProcessDocumentMetadata.TypeEnum.ORDERRESPONSESIMPLE) {
            try {
                OrderResponseSimpleType orderResponse = mapper.readValue(content, OrderResponseSimpleType.class);

                eu.nimble.service.model.ubl.orderresponsesimple.ObjectFactory factory = new eu.nimble.service.model.ubl.orderresponsesimple.ObjectFactory();
                logger.debug(" $$$ Created document: {}", JAXBUtility.serialize(orderResponse, factory.createOrderResponseSimple(orderResponse)));

                return orderResponse;
            } catch (IOException e) {
                logger.error("", e);
            }
        } else if(documentType == ProcessDocumentMetadata.TypeEnum.REQUESTFORQUOTATION) {
            try {
                RequestForQuotationType rfq = mapper.readValue(content, RequestForQuotationType.class);

                eu.nimble.service.model.ubl.requestforquotation.ObjectFactory factory = new eu.nimble.service.model.ubl.requestforquotation.ObjectFactory();
                logger.debug(" $$$ Created document: {}", JAXBUtility.serialize(rfq, factory.createRequestForQuotation(rfq)));

                return rfq;
            } catch (IOException e) {
                logger.error("", e);
            }
        } else if(documentType == ProcessDocumentMetadata.TypeEnum.QUOTATION) {
            try {
                QuotationType quotation = mapper.readValue(content, QuotationType.class);

                eu.nimble.service.model.ubl.quotation.ObjectFactory factory = new eu.nimble.service.model.ubl.quotation.ObjectFactory();
                logger.debug(" $$$ Created document: {}", JAXBUtility.serialize(quotation, factory.createQuotation(quotation)));

                return quotation;
            } catch (IOException e) {
                logger.error("", e);
            }
        }

        return null;
    }

    @Override
    public void saveDocument(String processInstanceId, String initiatorID, String responderID,
                            Object document) {
        ProcessDocumentMetadata documentMetadata = new ProcessDocumentMetadata();
        documentMetadata.setInitiatorID(initiatorID);
        documentMetadata.setResponderID(responderID);
        documentMetadata.setProcessInstanceID(processInstanceId);

        DateTime submissionDate = new DateTime();
        documentMetadata.setSubmissionDate(DateUtility.convert(submissionDate));

        if(document instanceof OrderType) {
            OrderType order = (OrderType) document;
            documentMetadata.setDocumentID(order.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.WAITINGRESPONSE);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.ORDER);
        } else if(document instanceof OrderResponseSimpleType) {
            OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) document;
            documentMetadata.setDocumentID(orderResponse.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.ORDERRESPONSESIMPLE);
        } else if(document instanceof  RequestForQuotationType) {
            RequestForQuotationType rfq = (RequestForQuotationType) document;
            documentMetadata.setDocumentID(rfq.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.WAITINGRESPONSE);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.REQUESTFORQUOTATION);
        } else if(document instanceof  QuotationType) {
            QuotationType quotation = (QuotationType) document;
            documentMetadata.setDocumentID(quotation.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.QUOTATION);
        }

        // persist the document metadata
        DocumentDAOUtility.addDocumentWithMetadata(documentMetadata, document);
    }

    @Override
    public void sendDocument(String processInstanceId, String initiatorID, String responderID, Object document) {
        // TODO: Send email notification to the responder...

        // if this document is a response to an initiating document, set the response code of the initiating document
        // e.g OrderResponse to an Order
        if(document instanceof OrderResponseSimpleType) {
            OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) document;
            String orderID = orderResponse.getOrderReference().getID();
            boolean isAccepted = orderResponse.isAcceptedIndicator();

            ProcessDocumentMetadata initiatingDocumentMetadata = DocumentDAOUtility.getDocumentMetadata(orderID);
            if(isAccepted)
                initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            else
                initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.DENIED);

            DocumentDAOUtility.updateDocumentMetadata(initiatingDocumentMetadata);
        } else if(document instanceof QuotationType) {
            QuotationType quotation = (QuotationType) document;
            String rfqID = quotation.getRequestForQuotationDocumentReference().getID();
            ProcessDocumentMetadata initiatingDocumentMetadata = DocumentDAOUtility.getDocumentMetadata(rfqID);
            initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
        }
    }

}
