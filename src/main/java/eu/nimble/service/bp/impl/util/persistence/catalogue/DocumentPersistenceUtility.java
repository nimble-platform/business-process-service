package eu.nimble.service.bp.impl.util.persistence.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.util.bp.DocumentEnumClassMapper;
import eu.nimble.service.bp.impl.util.persistence.DataIntegratorUtil;
import eu.nimble.service.bp.impl.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import org.camunda.bpm.engine.ProcessEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by yildiray on 6/7/2017.
 */
public class DocumentPersistenceUtility {
    private static Logger logger = LoggerFactory.getLogger(DocumentPersistenceUtility.class);

    private static final String QUERY_GET_ORDER_IDS_FOR_PARTY = "SELECT order_.ID from OrderType order_ join order_.orderLine line where line.lineItem.item.manufacturerParty.ID = :partyId AND line.lineItem.item.manufacturersItemIdentification.ID = :itemId";
    private static final String QUERY_GET_ORDER_RESPONSE_ID = "SELECT orderResponse.ID FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = :documentId";
    private static final String QUERY_GET_ORDER_RESPONSE_BY_ORDER_ID = "SELECT orderResponse FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = :documentId";
    private static final String QUERY_GET_DOCUMENT = "SELECT document FROM %s document WHERE document.ID = :documentId";

    public static List<String> getOrderIds(String partyId, String itemId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_ORDER_IDS_FOR_PARTY, new String[]{"partyId", "itemId"}, new Object[]{partyId, itemId});
    }

    public static String getOrderResponseIdByOrderId(String orderId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_ORDER_RESPONSE_ID, new String[]{"documentId"}, new Object[]{orderId});
    }

    public static OrderResponseSimpleType getOrderResponseDocumentByOrderId(String documentId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_ORDER_RESPONSE_BY_ORDER_ID, new String[]{"documentId"}, new Object[]{documentId});
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

    public static void updateDocument(String processContextId, Object document, String documentId, DocumentType documentType, String partyId) {
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId);

        Object existingDocument = getUBLDocument(documentId, documentType);
        businessProcessContext.setPreviousDocument(existingDocument);
        EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(partyId);
        DataIntegratorUtil.checkExistingParties(document);
        document = repositoryWrapper.updateEntity(document);
        businessProcessContext.setDocument(document);
    }

    public static void addDocumentWithMetadata(String processContextId, ProcessDocumentMetadata documentMetadata, Object document) {
        ProcessDocumentMetadataDAO processDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocumentMetadata_DAO(documentMetadata);
        new JPARepositoryFactory().forBpRepository().persistEntity(processDocumentDAO);
        // save ProcessDocumentMetadataDAO
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId);
        businessProcessContext.setMetadataDAO(processDocumentDAO);

        if (document != null) {
            // remove binary content from the document
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(documentMetadata.getInitiatorID());
            repositoryWrapper.updateEntityForPersistCases(document);
            // save Object
            businessProcessContext.setDocument(document);
        }

    }

    public static void deleteDocumentWithMetadata(String documentId) {
        ProcessDocumentMetadataDAO processDocumentMetadataDAO = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);

        Object document = getUBLDocument(documentId, processDocumentMetadataDAO.getType());

        if (document != null) {
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper(processDocumentMetadataDAO.getInitiatorID());
            repositoryWrapper.deleteEntity(document);
        }

        new JPARepositoryFactory().forBpRepository().deleteEntityByHjid(ProcessDocumentMetadataDAO.class, processDocumentMetadataDAO.getHjid());
    }

    public static Object getUBLDocument(String documentID, DocumentType documentType) {
        Class documentClass = DocumentEnumClassMapper.getDocumentClass(documentType);
        String hibernateEntityName = documentClass.getSimpleName();
        String query = String.format(QUERY_GET_DOCUMENT, hibernateEntityName);
        Object document = new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(query, new String[]{"documentId"}, new Object[]{documentID});
        return document;
    }

    public static Object getUBLDocument(String documentId) {
        ProcessDocumentMetadataDAO processDocumentMetadataDAO = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
        logger.debug(" $$$ Document metadata for {} is {}...", documentId, processDocumentMetadataDAO);
        return getUBLDocument(documentId, processDocumentMetadataDAO.getType());
    }
}
