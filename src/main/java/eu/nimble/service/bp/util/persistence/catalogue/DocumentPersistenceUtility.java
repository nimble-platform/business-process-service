package eu.nimble.service.bp.util.persistence.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.model.hyperjaxb.DocumentType;
import eu.nimble.service.bp.model.hyperjaxb.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.util.bp.DocumentEnumClassMapper;
import eu.nimble.service.bp.util.persistence.DataIntegratorUtil;
import eu.nimble.service.bp.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.document.IDocument;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by yildiray on 6/7/2017.
 */
public class DocumentPersistenceUtility {
    private static Logger logger = LoggerFactory.getLogger(DocumentPersistenceUtility.class);

    private static final String QUERY_GET_ORDER_IDS_FOR_PARTY = "SELECT order_.ID from OrderType order_ join order_.orderLine line JOIN line.lineItem.item.manufacturerParty.partyIdentification partyIdentification where partyIdentification.ID = :partyId AND line.lineItem.item.manufacturersItemIdentification.ID = :itemId";
    private static final String QUERY_GET_ORDER_RESPONSE_ID = "SELECT orderResponse.ID FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = :documentId";
    private static final String QUERY_GET_ORDER_RESPONSE_BY_ORDER_ID = "SELECT orderResponse FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = :documentId";
    private static final String QUERY_GET_DOCUMENT = "SELECT document FROM %s document WHERE document.ID = :documentId";
    private static final String QUERY_GET_ADDITIONAL_DOCUMENT_TYPES_FROM_RFQ = "SELECT doc.ID, doc.documentType FROM RequestForQuotationType rfq join rfq.additionalDocumentReference doc WHERE rfq.ID = :documentId";
    private static final String QUERY_GET_ADDITIONAL_DOCUMENT_TYPES_FROM_IIR = "SELECT doc.ID, doc.documentType FROM ItemInformationRequestType iir join iir.additionalDocumentReference doc WHERE iir.ID = :documentId";

    public static List<String> getOrderIds(String partyId, String itemId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_ORDER_IDS_FOR_PARTY, new String[]{"partyId", "itemId"}, new Object[]{partyId, itemId});
    }

    public static String getOrderResponseIdByOrderId(String orderId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_ORDER_RESPONSE_ID, new String[]{"documentId"}, new Object[]{orderId});
    }

    public static OrderResponseSimpleType getOrderResponseDocumentByOrderId(String documentId) {
        // get document from cache
        Object document = SpringBridge.getInstance().getCacheHelper().getDocument(documentId);
        // if it does not exist in the cache, retrieve it from the database and update the cache
        if(document == null){
            document = new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(QUERY_GET_ORDER_RESPONSE_BY_ORDER_ID, new String[]{"documentId"}, new Object[]{documentId});
            // update the cache
            SpringBridge.getInstance().getCacheHelper().putDocument(document);
        }

        return (OrderResponseSimpleType) document;
    }

    public static <T> T readDocument(DocumentType documentType, String content) {
        Class<T> klass = DocumentEnumClassMapper.getDocumentClass(documentType);
        ObjectMapper objectMapper = JsonSerializationUtility.getObjectMapper();
        try {
            return objectMapper.readValue(content, klass);

        } catch (IOException e) {
            String msg = String.format("Failed to deserialize document. type: %s, content: %s", klass.getName(), content);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public static void updateDocument(String processContextId, Object document, String partyId) {
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId);

        EntityIdAwareRepositoryWrapper repositoryWrapper = businessProcessContext.getEntityIdAwareRepository(partyId);
        DataIntegratorUtil.checkExistingParties(document,processContextId);
        repositoryWrapper.updateEntity(document);
        // update the cache
        SpringBridge.getInstance().getCacheHelper().putDocument(document);
    }

    public static void addDocumentWithMetadata(String processContextId, ProcessDocumentMetadata documentMetadata, Object document) {
        ProcessDocumentMetadataDAO processDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocumentMetadata_DAO(documentMetadata);

        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId);

        businessProcessContext.getBpRepository().persistEntity(processDocumentDAO);

        if (document != null) {
            // remove binary content from the document
            EntityIdAwareRepositoryWrapper repositoryWrapper = businessProcessContext.getEntityIdAwareRepository(documentMetadata.getInitiatorID());
            repositoryWrapper.updateEntityForPersistCases(document);
            // update the cache
            SpringBridge.getInstance().getCacheHelper().putDocument(document);
        }
    }

    public static void deleteDocumentsWithMetadatas(List<String> documentIds) {
        for(String documentId: documentIds){
            ProcessDocumentMetadataDAO processDocumentMetadataDAO = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);

            Object document = getUBLDocument(documentId, processDocumentMetadataDAO.getType());

            if (document != null) {
                EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(processDocumentMetadataDAO.getInitiatorID());
                repositoryWrapper.deleteEntity(document);
                // remove document from the cache
                SpringBridge.getInstance().getCacheHelper().removeDocument(document);
            }

            new JPARepositoryFactory().forBpRepository().deleteEntityByHjid(ProcessDocumentMetadataDAO.class, processDocumentMetadataDAO.getHjid());
        }
    }

    public static IDocument getUBLDocument(String documentID, DocumentType documentType) {
        Class documentClass = DocumentEnumClassMapper.getDocumentClass(documentType);
        String hibernateEntityName = documentClass.getSimpleName();
        String query = String.format(QUERY_GET_DOCUMENT, hibernateEntityName);
        // get document from cache
        Object document = SpringBridge.getInstance().getCacheHelper().getDocument(documentID);
        // if it does not exist in the cache, retrieve it from the database and update the cache
        if(document == null){
            document = new JPARepositoryFactory().forCatalogueRepository(true).getSingleEntity(query, new String[]{"documentId"}, new Object[]{documentID});
            // update the cache
            SpringBridge.getInstance().getCacheHelper().putDocument(document);
        }
        return (IDocument) document;
    }

    public static IDocument getUBLDocument(String documentId) {
        ProcessDocumentMetadataDAO processDocumentMetadataDAO = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
        logger.debug(" $$$ Document metadata for {} is {}...", documentId, processDocumentMetadataDAO);
        // there is no UBL document when metadata is null
        if(processDocumentMetadataDAO == null){
            return null;
        }
        return getUBLDocument(documentId, processDocumentMetadataDAO.getType());
    }

    /**
     * @return string tuples including the (additional document id - additional document type) information associated to an {@link eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType}
     */
    public static List<Object> getAdditionalDocumentTypesFromIir(String iirId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_ADDITIONAL_DOCUMENT_TYPES_FROM_IIR, new String[]{"documentId"}, new Object[]{iirId});
    }

    /**
     * @return string tuples including the (additional document id - additional document type) information associated to an {@link eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType}
     */
    public static List<Object> getAdditionalDocumentTypesFromRfq(String rfqId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_ADDITIONAL_DOCUMENT_TYPES_FROM_RFQ, new String[]{"documentId"}, new Object[]{rfqId});
    }
}
