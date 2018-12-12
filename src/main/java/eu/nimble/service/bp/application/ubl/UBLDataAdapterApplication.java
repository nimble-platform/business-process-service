package eu.nimble.service.bp.application.ubl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.impl.persistence.util.DataIntegratorUtil;
import eu.nimble.service.bp.impl.persistence.util.DocumentDAOUtility;
import eu.nimble.service.bp.impl.util.controller.HttpResponseUtil;
import eu.nimble.service.bp.impl.util.serialization.Serializer;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.despatchadvice.DespatchAdviceType;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.service.model.ubl.iteminformationresponse.ItemInformationResponseType;
import eu.nimble.service.model.ubl.order.ObjectFactory;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.ppaprequest.PpapRequestType;
import eu.nimble.service.model.ubl.ppapresponse.PpapResponseType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.service.model.ubl.transportexecutionplan.TransportExecutionPlanType;
import eu.nimble.service.model.ubl.transportexecutionplanrequest.TransportExecutionPlanRequestType;
import eu.nimble.utility.DateUtility;
import eu.nimble.utility.JAXBUtility;
import eu.nimble.utility.persistence.resource.ResourceValidationUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

/**
 * Created by yildiray on 6/5/2017.
 */
public class UBLDataAdapterApplication implements IBusinessProcessApplication {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    //TODO pass generic class instead of enumeration to prevent checking document type for each case
    public Object createDocument(String initiatorID, String responderID, String content, ProcessDocumentMetadata.TypeEnum documentType) {
        ObjectMapper mapper = Serializer.getDefaultObjectMapper();
        Object document = null;

        if(documentType == ProcessDocumentMetadata.TypeEnum.ORDER) {
            try {
                OrderType order = mapper.readValue(content, OrderType.class);
                document = order;

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        } else if(documentType == ProcessDocumentMetadata.TypeEnum.ORDERRESPONSESIMPLE) {
            try {
                document = mapper.readValue(content, OrderResponseSimpleType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
        else if(documentType == ProcessDocumentMetadata.TypeEnum.PPAPREQUEST){
            try {
                document = mapper.readValue(content, PpapRequestType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
        else if(documentType == ProcessDocumentMetadata.TypeEnum.PPAPRESPONSE){
            try {
                document = mapper.readValue(content, PpapResponseType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }  else if(documentType == ProcessDocumentMetadata.TypeEnum.REQUESTFORQUOTATION) {
            try {
                document = mapper.readValue(content, RequestForQuotationType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        } else if(documentType == ProcessDocumentMetadata.TypeEnum.QUOTATION) {
            try {
                document = mapper.readValue(content, QuotationType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        } else if(documentType == ProcessDocumentMetadata.TypeEnum.DESPATCHADVICE) {
            try {
                document = mapper.readValue(content, DespatchAdviceType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        } else if(documentType == ProcessDocumentMetadata.TypeEnum.RECEIPTADVICE) {
            try {
                document = mapper.readValue(content, ReceiptAdviceType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }

        } else if(documentType == ProcessDocumentMetadata.TypeEnum.TRANSPORTEXECUTIONPLANREQUEST) {
            try {
                document = mapper.readValue(content, TransportExecutionPlanRequestType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }

        } else if(documentType == ProcessDocumentMetadata.TypeEnum.TRANSPORTEXECUTIONPLAN) {
            try {
                document = mapper.readValue(content, TransportExecutionPlanType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }

        } else if(documentType == ProcessDocumentMetadata.TypeEnum.ITEMINFORMATIONREQUEST) {
            try {
                document = mapper.readValue(content, ItemInformationRequestType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }

        } else if(documentType == ProcessDocumentMetadata.TypeEnum.ITEMINFORMATIONRESPONSE) {
            try {
                document = mapper.readValue(content, ItemInformationResponseType.class);

            } catch (IOException e) {
                String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
                logger.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }

        return document;
    }

    @Override
    public void saveDocument(String businessContextId,String processInstanceId, String initiatorID, String responderID, String creatorUserID,
                            Object document, List<String> relatedProducts, List<String> relatedProductCategories) {
        ProcessDocumentMetadata documentMetadata = new ProcessDocumentMetadata();
        documentMetadata.setInitiatorID(initiatorID);
        documentMetadata.setResponderID(responderID);
        documentMetadata.setProcessInstanceID(processInstanceId);
        documentMetadata.setCreatorUserID(creatorUserID);

        DateTime submissionDate = new DateTime();
        documentMetadata.setSubmissionDate(DateUtility.convert(submissionDate));
        documentMetadata.getRelatedProducts().addAll(relatedProducts);
        documentMetadata.getRelatedProductCategories().addAll(relatedProductCategories);

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

        } else if(document instanceof PpapRequestType){
            PpapRequestType ppapRequestType = (PpapRequestType) document;
            documentMetadata.setDocumentID(ppapRequestType.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.WAITINGRESPONSE);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.PPAPREQUEST);

        } else if(document instanceof PpapResponseType){
            PpapResponseType ppapResponseType = (PpapResponseType) document;
            documentMetadata.setDocumentID(ppapResponseType.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.WAITINGRESPONSE);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.PPAPRESPONSE);
        }  else if(document instanceof  RequestForQuotationType) {
            RequestForQuotationType rfq = (RequestForQuotationType) document;
            documentMetadata.setDocumentID(rfq.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.WAITINGRESPONSE);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.REQUESTFORQUOTATION);

        } else if(document instanceof  QuotationType) {
            QuotationType quotation = (QuotationType) document;
            documentMetadata.setDocumentID(quotation.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.QUOTATION);

        } else if(document instanceof  DespatchAdviceType) {
            DespatchAdviceType despatchAdvice = (DespatchAdviceType) document;
            documentMetadata.setDocumentID(despatchAdvice.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.WAITINGRESPONSE);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.DESPATCHADVICE);

        } else if(document instanceof  ReceiptAdviceType) {
            ReceiptAdviceType receiptAdvice = (ReceiptAdviceType) document;
            documentMetadata.setDocumentID(receiptAdvice.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.RECEIPTADVICE);

        } else if(document instanceof TransportExecutionPlanRequestType) {
            TransportExecutionPlanRequestType transportExecutionPlanRequestType = (TransportExecutionPlanRequestType) document;
            documentMetadata.setDocumentID(transportExecutionPlanRequestType.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.WAITINGRESPONSE);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.TRANSPORTEXECUTIONPLANREQUEST);

        } else if(document instanceof TransportExecutionPlanType) {
            TransportExecutionPlanType transportExecutionPlanType = (TransportExecutionPlanType) document;
            documentMetadata.setDocumentID(transportExecutionPlanType.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.TRANSPORTEXECUTIONPLAN);

        } else if(document instanceof ItemInformationRequestType) {
            ItemInformationRequestType itemInformationRequest = (ItemInformationRequestType) document;
            documentMetadata.setDocumentID(itemInformationRequest.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.WAITINGRESPONSE);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.ITEMINFORMATIONREQUEST);

        } else if(document instanceof ItemInformationResponseType) {
            ItemInformationResponseType itemInformationResponse = (ItemInformationResponseType) document;
            documentMetadata.setDocumentID(itemInformationResponse.getID());
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.ITEMINFORMATIONRESPONSE);
        }
        DataIntegratorUtil.checkExistingParties(document);

        // persist the document metadata
        DocumentDAOUtility.addDocumentWithMetadata(businessContextId,documentMetadata, document);
    }

    @Override
    public void sendDocument(String businessContextId,String processInstanceId, String initiatorID, String responderID, Object document) {
        // TODO: Send email notification to the responder...

        // if this document is a response to an initiating document, set the response code of the initiating document
        // e.g OrderResponse to an Order
        ProcessDocumentMetadata initiatingDocumentMetadata = null;
        if(document instanceof OrderResponseSimpleType) {
            OrderResponseSimpleType orderResponse = (OrderResponseSimpleType) document;
            String orderID = orderResponse.getOrderReference().getDocumentReference().getID();
            boolean isAccepted = orderResponse.isAcceptedIndicator();

            initiatingDocumentMetadata = DocumentDAOUtility.getDocumentMetadata(orderID);
            if(isAccepted)
                initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            else
                initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.DENIED);

        }
        else if(document instanceof PpapResponseType){
            PpapResponseType ppapResponseType = (PpapResponseType) document;
            String ppapREQUESTID = ppapResponseType.getPpapDocumentReference().getID();
            initiatingDocumentMetadata = DocumentDAOUtility.getDocumentMetadata(ppapREQUESTID);
            initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);

        }else if(document instanceof QuotationType) {
            QuotationType quotation = (QuotationType) document;
            String rfqID = quotation.getRequestForQuotationDocumentReference().getID();
            boolean isAccepted = quotation.getDocumentStatusCode().getName().equals("Accepted");

            initiatingDocumentMetadata = DocumentDAOUtility.getDocumentMetadata(rfqID);
            if(isAccepted){
                initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            }
            else {
                initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.DENIED);
            }


        } else if(document instanceof ReceiptAdviceType) {
            ReceiptAdviceType receiptAdvice = (ReceiptAdviceType) document;
            String despatchAdviceID = receiptAdvice.getDespatchDocumentReference().get(0).getID();
            initiatingDocumentMetadata = DocumentDAOUtility.getDocumentMetadata(despatchAdviceID);
            initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);

        } else if(document instanceof TransportExecutionPlanType) {
            TransportExecutionPlanType transportExecutionPlanType = (TransportExecutionPlanType) document;
            String tepDocRefId = transportExecutionPlanType.getTransportExecutionPlanRequestDocumentReference().getID();
            boolean isAccepted = transportExecutionPlanType.getDocumentStatusCode().getName().equals("Accepted");

            initiatingDocumentMetadata = DocumentDAOUtility.getDocumentMetadata(tepDocRefId);
            if (isAccepted){
                initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            }
            else {
                initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.DENIED);
            }

        } else if(document instanceof ItemInformationResponseType) {
            ItemInformationResponseType itemInformationResponse = (ItemInformationResponseType) document;
            String itemInformationRequestId = itemInformationResponse.getItemInformationRequestDocumentReference().getID();
            initiatingDocumentMetadata = DocumentDAOUtility.getDocumentMetadata(itemInformationRequestId);
            initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);

        }

        if(initiatingDocumentMetadata != null) {
            DocumentDAOUtility.updateDocumentMetadata(businessContextId,initiatingDocumentMetadata);
        }
    }
}
