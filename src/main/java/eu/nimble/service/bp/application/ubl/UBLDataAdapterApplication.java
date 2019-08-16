package eu.nimble.service.bp.application.ubl;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.application.IBusinessProcessApplication;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.util.UBLUtility;
import eu.nimble.service.bp.util.bp.DocumentEnumClassMapper;
import eu.nimble.service.bp.util.bp.BusinessProcessUtility;
import eu.nimble.service.bp.util.persistence.DataIntegratorUtil;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.utility.DateUtility;
import eu.nimble.utility.JsonSerializationUtility;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by yildiray on 6/5/2017.
 */
public class UBLDataAdapterApplication implements IBusinessProcessApplication {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Object createDocument(String initiatorID, String responderID, String content, ProcessDocumentMetadata.TypeEnum documentType) {
        ObjectMapper mapper = JsonSerializationUtility.getObjectMapper();
        Class documentClass = DocumentEnumClassMapper.getDocumentClass(DocumentType.valueOf(documentType.toString()));
        Object document;
        try {
            document = mapper.readValue(content, documentClass);
            return document;

        } catch (IOException e) {
            String msg = String.format("Failed to deserialize document. initiator id: %s, responder id: %s, content: %s", initiatorID, responderID, content);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    @Override
    public void saveDocument(String businessContextId,String processInstanceId, String initiatorID, String responderID, String creatorUserID,
                            Object document, List<String> relatedProducts, List<String> relatedProductCategories) {
        ProcessDocumentMetadata documentMetadata = new ProcessDocumentMetadata();
        boolean initialDocument = BusinessProcessUtility.isInitialDocument(document.getClass());
        DocumentType documentType = DocumentEnumClassMapper.getDocumentTypeForClass(document.getClass());
        DateTime submissionDate = new DateTime();

        documentMetadata.setInitiatorID(initiatorID);
        documentMetadata.setResponderID(responderID);
        documentMetadata.setProcessInstanceID(processInstanceId);
        documentMetadata.setCreatorUserID(creatorUserID);
        documentMetadata.setSubmissionDate(DateUtility.convert(submissionDate));
        documentMetadata.getRelatedProducts().addAll(relatedProducts);
        documentMetadata.getRelatedProductCategories().addAll(relatedProductCategories);
        documentMetadata.setDocumentID(UBLUtility.getDocumentId(document));
        documentMetadata.setType(ProcessDocumentMetadata.TypeEnum.valueOf(documentType.toString()));

        if(initialDocument) {
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.WAITINGRESPONSE);

        } else {
            // response documents are always set to approved
            documentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
        }

        //replace parties with the ones persisted in the database
        DataIntegratorUtil.checkExistingParties(document);

        // persist the document metadata
        DocumentPersistenceUtility.addDocumentWithMetadata(businessContextId,documentMetadata, document);
    }

    @Override
    public void sendDocument(String businessContextId,String processInstanceId, String initiatorID, String responderID, Object document) {
        List<ProcessDocumentMetadataDAO> documentMetadata = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(processInstanceId);

        if(documentMetadata.size() > 1) {
            ProcessDocumentMetadata initiatingDocumentMetadata = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(documentMetadata.get(0));

            // if this document is a response to an initiating document, set the response code of the initiating document
            // e.g OrderResponse to an Order
            boolean positiveResponse = UBLUtility.documentIndicatesPositiveResponse(document);
            if(positiveResponse) {
                initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.APPROVED);
            } else {
                initiatingDocumentMetadata.setStatus(ProcessDocumentMetadata.StatusEnum.DENIED);
            }

            ProcessDocumentMetadataDAOUtility.updateDocumentMetadata(businessContextId,initiatingDocumentMetadata);
        }
    }
}
