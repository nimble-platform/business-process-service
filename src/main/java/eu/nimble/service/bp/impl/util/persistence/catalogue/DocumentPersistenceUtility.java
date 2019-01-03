package eu.nimble.service.bp.impl.util.persistence.catalogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.util.bp.BusinessProcessUtility;
import eu.nimble.service.bp.impl.util.bp.DocumentEnumClassMapper;
import eu.nimble.service.bp.impl.util.camunda.CamundaEngine;
import eu.nimble.service.bp.impl.util.persistence.DataIntegratorUtil;
import eu.nimble.service.bp.impl.util.persistence.bp.HibernateSwaggerObjectMapper;
import eu.nimble.service.bp.impl.util.persistence.bp.ProcessDocumentMetadataDAOUtility;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.bp.swagger.model.Transaction;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.utility.JsonSerializationUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import org.camunda.bpm.engine.ProcessEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yildiray on 6/7/2017.
 */
public class DocumentPersistenceUtility {
    private static Logger logger = LoggerFactory.getLogger(DocumentPersistenceUtility.class);

    /**
     * the query below is completed in the {@code getRequestMetadata} method
     */
    private static final String QUERY_GET_METADATA = "SELECT documentMetadata FROM ProcessDocumentMetadataDAO documentMetadata WHERE documentMetadata.processInstanceID=:processInstanceId AND %s";
    private static final String QUERY_GET_ORDER_IDS_FOR_PARTY = "SELECT order_.ID from OrderType order_ join order_.orderLine line where line.lineItem.item.manufacturerParty.ID = :partyId AND line.lineItem.item.manufacturersItemIdentification.ID = :itemId";
    private static final String QUERY_GET_ORDER_RESPONSE_ID = "SELECT orderResponse.ID FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = :documentId";
    private static final String QUERY_GET_ORDER_RESPONSE_BY_ID = "SELECT orderResponse FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = :documentId";
    private static final String QUERY_GET_DOCUMENT = "SELECT document FROM %s document WHERE document.ID = :documentId";

    public static List<String> getOrderIds(String partyId, String itemId) {
        return new JPARepositoryFactory().forCatalogueRepository().getEntities(QUERY_GET_ORDER_IDS_FOR_PARTY, new String[]{"partyId", "itemId"}, new Object[]{partyId, itemId});
    }

    public static String getOrderResponseId(String documentId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_ORDER_RESPONSE_ID, new String[]{"documentId"}, new Object[]{documentId});
    }

    public static OrderResponseSimpleType getOrderResponseSimple(String documentId) {
        return new JPARepositoryFactory().forCatalogueRepository().getSingleEntity(QUERY_GET_ORDER_RESPONSE_BY_ID, new String[]{"documentId"}, new Object[]{documentId});
    }

    public static ExecutionConfiguration getExecutionConfiguration(String partnerID, String processID, ProcessConfiguration.RoleTypeEnum roleType, String transactionID, ExecutionConfiguration.ApplicationTypeEnum applicationType) {
        String processKey = ProcessEngines.getDefaultProcessEngine().getRepositoryService().getProcessDefinition(processID).getKey();

        TransactionConfigurationDAO transactionConfigurationDAO = null;
        ExecutionConfigurationDAO executionConfigurationDAO = null;
        // Get buyer order application preference for data adapter
        /*ProcessConfigurationDAO processConfiguration = DAOUtility.getProcessConfiguration(partnerID, processKey, roleType);

        if (processConfiguration != null) { // it is configured
            List<TransactionConfigurationDAO> configurations = processConfiguration.getTransactionConfigurations();
            for (TransactionConfigurationDAO configuration : configurations) {
                if(configuration.getTransactionID().equals(transactionID)) {
                    transactionConfigurationDAO = configuration;
                    for (ExecutionConfigurationDAO executionConfiguration : configuration.getExecutionConfigurations()) {
                        if (executionConfiguration.getApplicationType().value().equals(applicationType.toString())) {
                            executionConfigurationDAO = executionConfiguration;
                            break;
                        }
                    }
                }
            }
        } else { // it is not configured by the partner
            */
        transactionConfigurationDAO = new TransactionConfigurationDAO();
        // TODO: Retrieve it from the identity service or context
        transactionConfigurationDAO.setTransactionID(transactionID);

        executionConfigurationDAO = new ExecutionConfigurationDAO();
        executionConfigurationDAO.setExecutionUri("eu.nimble.service.bp.application.ubl.UBLDataAdapterApplication");
        executionConfigurationDAO.setExecutionType(ApplicationExecutionType.JAVA);
        executionConfigurationDAO.setApplicationType(ApplicationType.fromValue(applicationType.toString()));
        transactionConfigurationDAO.getExecutionConfigurations().add(executionConfigurationDAO);
        //}

        ExecutionConfiguration executionConfiguration = HibernateSwaggerObjectMapper.createExecutionConfiguration(executionConfigurationDAO);
        return executionConfiguration;
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


    public static void updateDocumentMetadata(String processContextId, ProcessDocumentMetadata body) {
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId);

        ProcessDocumentMetadataDAO storedDocumentDAO = ProcessDocumentMetadataDAOUtility.findByDocumentID(body.getDocumentID());

        businessProcessContext.setPreviousDocumentMetadataStatus(storedDocumentDAO.getStatus());

        ProcessDocumentMetadataDAO newDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocumentMetadata_DAO(body);

        GenericJPARepository repo = new JPARepositoryFactory().forBpRepository();
        repo.deleteEntityByHjid(ProcessDocumentMetadataDAO.class, storedDocumentDAO.getHjid());
        repo.persistEntity(newDocumentDAO);

        businessProcessContext.setUpdatedDocumentMetadata(newDocumentDAO);
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

    public static ProcessDocumentMetadata getDocumentMetadata(String documentId) {
        ProcessDocumentMetadataDAO processDocumentDAO = ProcessDocumentMetadataDAOUtility.findByDocumentID(documentId);
        ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
        return processDocument;
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

    public static ProcessDocumentMetadata getCorrespondingResponseMetadata(String documentID, DocumentType documentType) {
        String id = "";
        if (documentType == DocumentType.ORDER) {
            id = getOrderResponseId(documentID);
        }
        return getDocumentMetadata(id);
    }

    public static Object getResponseDocument(String documentID, DocumentType documentType) {
        Object document = null;
        if (documentType == DocumentType.ORDER) {
            document = getOrderResponseSimple(documentID);
        }
        return document;
    }

    public static ProcessDocumentMetadata getRequestMetadata(String processInstanceId) {
        List<Transaction.DocumentTypeEnum> documentTypes = BusinessProcessUtility.getInitialDocumentsForAllProcesses();
        List<String> parameterNames = new ArrayList<>();
        List<Object> parameterValues = new ArrayList<>();
        parameterNames.add("processInstanceId");
        parameterValues.add(processInstanceId);
        String query = String.format(QUERY_GET_METADATA, createConditionsForMetadataQuery(documentTypes, parameterNames, parameterValues));
        ProcessDocumentMetadataDAO processDocumentDAO = new JPARepositoryFactory().forBpRepository().getSingleEntity(query, parameterNames.toArray(new String[parameterNames.size()]), parameterValues.toArray());
        return HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
    }

    public static ProcessDocumentMetadata getResponseMetadata(String processInstanceId) {
        List<Transaction.DocumentTypeEnum> documentTypes = BusinessProcessUtility.getResponseDocumentsForAllProcesses();
        List<String> parameterNames = new ArrayList<>();
        List<Object> parameterValues = new ArrayList<>();
        parameterNames.add("processInstanceId");
        parameterValues.add(processInstanceId);
        String query = String.format(QUERY_GET_METADATA, createConditionsForMetadataQuery(documentTypes, parameterNames, parameterValues));
        ProcessDocumentMetadataDAO processDocumentDAO = new JPARepositoryFactory().forBpRepository().getSingleEntity(query, parameterNames.toArray(new String[parameterNames.size()]), parameterValues.toArray());
        if (processDocumentDAO == null) {
            return null;
        }
        return HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
    }

    private static String createConditionsForMetadataQuery(List<Transaction.DocumentTypeEnum> documentTypes, List<String> parameterNames, List<Object> parameterValues) {
        StringBuilder sb = new StringBuilder("(");
        int i=0;
        for(; i<documentTypes.size()-1; i++) {
            sb.append("documentMetadata.type = :doc").append(i).append(" OR ");
            parameterNames.add("doc" + i);
            parameterValues.add(DocumentType.valueOf(documentTypes.get(i).toString()));
        }
        sb.append("documentMetadata.type = :doc").append(i);
        parameterNames.add("doc" + i);
        parameterValues.add(DocumentType.valueOf(documentTypes.get(i).toString()));
        sb.append(")");
        return sb.toString();
    }
}
