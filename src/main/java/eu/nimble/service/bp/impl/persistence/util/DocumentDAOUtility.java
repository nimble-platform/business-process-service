package eu.nimble.service.bp.impl.persistence.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.util.serialization.Serializer;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
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
import eu.nimble.utility.Configuration;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.resource.EntityIdAwareRepositoryWrapper;
import eu.nimble.utility.persistence.resource.ResourceValidationUtil;
import org.apache.poi.ss.formula.functions.T;
import org.camunda.bpm.engine.ProcessEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.persistence.criteria.Order;
import javax.print.Doc;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yildiray on 6/7/2017.
 */
public class DocumentDAOUtility {
    private static final String QUERY_GET_REQUEST_METADATA = "FROM ProcessDocumentMetadataDAO documentMetadata WHERE documentMetadata.processInstanceID=:processInstanceId AND (" +
            "documentMetadata.type = 'REQUESTFORQUOTATION' OR documentMetadata.type = 'ORDER' OR documentMetadata.type = 'DESPATCHADVICE' OR " +
            "documentMetadata.type = 'PPAPREQUEST' OR documentMetadata.type = 'TRANSPORTEXECUTIONPLANREQUEST' OR documentMetadata.type = 'ITEMINFORMATIONREQUEST')";
    private static final String QUERY_GET_RESPONSE_METADATA = "SELECT documentMetadata FROM ProcessDocumentMetadataDAO documentMetadata WHERE documentMetadata.processInstanceID=:processInstanceId AND (" +
            "documentMetadata.type = 'QUOTATION' OR documentMetadata.type = 'ORDERRESPONSESIMPLE' OR documentMetadata.type = 'RECEIPTADVICE' OR " +
            "documentMetadata.type = 'PPAPRESPONSE' OR documentMetadata.type = 'TRANSPORTEXECUTIONPLAN' OR documentMetadata.type = 'ITEMINFORMATIONRESPONSE')";
    private static final String QUERY_PROCESS_METADATA_DOCUMENT_EXISTS = "SELECT count(*) FROM ProcessDocumentMetadataDAO document WHERE document.documentID = :documentId";

    private static Logger logger = LoggerFactory.getLogger(DocumentDAOUtility.class);

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
//        HibernateUtilityRef.getInstance("bp-data-model").persist(processDocumentDAO);
        SpringBridge.getInstance().getProcessDocumentMetadataDAORepository().save(processDocumentDAO);
        // save ProcessDocumentMetadataDAO
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId);
        businessProcessContext.setMetadataDAO(processDocumentDAO);

        if (document != null) {
//            HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).update(document);
            // remove binary content from the document
            document = BinaryContentUtil.removeBinaryContentFromDocument(document);
            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper((GenericJPARepository) SpringBridge.getInstance().getCatalogueRepository(), documentMetadata.getInitiatorID());
            document = repositoryWrapper.updateEntity(document);
            // save Object
            businessProcessContext.setDocument(document);
        }

    }


    public static void updateDocumentMetadata(String processContextId, ProcessDocumentMetadata body) {
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId);

        ProcessDocumentMetadataDAO storedDocumentDAO = DAOUtility.getProcessDocumentMetadata(body.getDocumentID());

        businessProcessContext.setPreviousDocumentMetadataStatus(storedDocumentDAO.getStatus());

        ProcessDocumentMetadataDAO newDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocumentMetadata_DAO(body);

//        HibernateUtilityRef.getInstance("bp-data-model").delete(storedDocumentDAO);
        SpringBridge.getInstance().getProcessDocumentMetadataDAORepository().delete(storedDocumentDAO.getHjid());
//        newDocumentDAO = (ProcessDocumentMetadataDAO) HibernateUtilityRef.getInstance("bp-data-model").update(newDocumentDAO);
        newDocumentDAO = SpringBridge.getInstance().getProcessDocumentMetadataDAORepository().save(newDocumentDAO);

        businessProcessContext.setUpdatedDocumentMetadata(newDocumentDAO);

    }

    public static <T> Class<T> getDocumentClass(DocumentType documentType) {
        Class<T> klass = null;
        switch (documentType) {
            case CATALOGUE:
                klass = (Class<T>) CatalogueType.class;
                break;
            case ORDER:
                klass = (Class<T>) OrderType.class;
                break;
            case ORDERRESPONSESIMPLE:
                klass = (Class<T>) OrderResponseSimpleType.class;
                break;
            case REQUESTFORQUOTATION:
                klass = (Class<T>) RequestForQuotationType.class;
                break;
            case QUOTATION:
                klass = (Class<T>) QuotationType.class;
                break;
            case DESPATCHADVICE:
                klass = (Class<T>) DespatchAdviceType.class;
                break;
            case RECEIPTADVICE:
                klass = (Class<T>) ReceiptAdviceType.class;
                break;
            case TRANSPORTEXECUTIONPLANREQUEST:
                klass = (Class<T>) TransportExecutionPlanRequestType.class;
                break;
            case TRANSPORTEXECUTIONPLAN:
                klass = (Class<T>) TransportExecutionPlanType.class;
                break;
            case PPAPREQUEST:
                klass = (Class<T>) PpapRequestType.class;
                break;
            case PPAPRESPONSE:
                klass = (Class<T>) PpapResponseType.class;
                break;
            case ITEMINFORMATIONREQUEST:
                klass = (Class<T>) ItemInformationRequestType.class;
                break;
            case ITEMINFORMATIONRESPONSE:
                klass = (Class<T>) ItemInformationResponseType.class;
                break;
        }
        if(klass == null) {
            String msg = String.format("Unknown document type: %s", documentType.toString());
            logger.warn(msg);
            throw new RuntimeException(msg);
        }
        return klass;
    }

    public static <T> T readDocument(DocumentType documentType, String content) {
        Class<T> klass = getDocumentClass(documentType);
        ObjectMapper objectMapper = Serializer.getDefaultObjectMapper();
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

        // while updating the document, be sure that we update binary contents as well
        // get uris of binary contents of existing item information request
        List<String> existingUris = BinaryContentUtil.getBinaryContentUris(existingDocument);
        // get uris of binary contents of updated item information request
        List<String> uris = BinaryContentUtil.getBinaryContentUris(document);
        // delete binary contents which do not exist in the updated one
        List<String> urisToBeDeleted = new ArrayList<>();
        for (String uri : existingUris) {
            if (!uris.contains(uri)) {
                urisToBeDeleted.add(uri);
            }
        }
        BinaryContentUtil.removeBinaryContentFromDatabase(urisToBeDeleted);
        // remove binary content from the document
        document = BinaryContentUtil.removeBinaryContentFromDocument(document);
        EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper((GenericJPARepository) SpringBridge.getInstance().getCatalogueRepository(), partyId);
        document = repositoryWrapper.updateEntity(document);
        businessProcessContext.setDocument(document);
    }

    public static ProcessDocumentMetadata getDocumentMetadata(String documentID) {
        ProcessDocumentMetadataDAO processDocumentDAO = DAOUtility.getProcessDocumentMetadata(documentID);
        ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
        return processDocument;
    }

    public static void deleteDocumentWithMetadata(String documentID) {
        ProcessDocumentMetadataDAO processDocumentMetadataDAO = DAOUtility.getProcessDocumentMetadata(documentID);

        Object document = getUBLDocument(documentID, processDocumentMetadataDAO.getType());

        if (document != null) {
            // delete binary contents of the document from the database
            try {
                BinaryContentUtil.removeBinaryContentFromDatabase(document);
            } catch (IOException e) {
                logger.error("Failed to delete binary contents of the document with id:{} ", documentID, e);
            }

            EntityIdAwareRepositoryWrapper repositoryWrapper = new EntityIdAwareRepositoryWrapper((JpaRepository) SpringBridge.getInstance().getCatalogueRepository(), processDocumentMetadataDAO.getInitiatorID());
            repositoryWrapper.delete(documentID);
        }

//        HibernateUtilityRef.getInstance("bp-data-model").delete(ProcessDocumentMetadataDAO.class, processDocumentMetadataDAO.getHjid());
        SpringBridge.getInstance().getBusinessProcessRepository().deleteEntityByHjid(ProcessDocumentMetadataDAO.class, processDocumentMetadataDAO.getHjid());
    }

    public static Object getUBLDocument(String documentID, DocumentType documentType) {
        String hibernateEntityName = "";
        switch (documentType) {
            case ORDER:
                hibernateEntityName = "OrderType";
                break;
            case INVOICE:
                hibernateEntityName = "InvoiceType";
                break;
            case CATALOGUE:
                hibernateEntityName = "CatalogueType";
                break;
            case QUOTATION:
                hibernateEntityName = "QuotationType";
                break;
            case ORDERRESPONSESIMPLE:
                hibernateEntityName = "OrderResponseSimpleType";
                break;
            case RECEIPTADVICE:
                hibernateEntityName = "ReceiptAdviceType";
                break;
            case DESPATCHADVICE:
                hibernateEntityName = "DespatchAdviceType";
                break;
            case REMITTANCEADVICE:
                hibernateEntityName = "RemittanceAdviceType";
                break;
            case APPLICATIONRESPONSE:
                hibernateEntityName = "ApplicationResponseType";
                break;
            case REQUESTFORQUOTATION:
                hibernateEntityName = "RequestForQuotationType";
                break;
            case TRANSPORTATIONSTATUS:
                hibernateEntityName = "TransportationStatusType";
                break;
            case TRANSPORTEXECUTIONPLANREQUEST:
                hibernateEntityName = "TransportExecutionPlanRequestType";
                break;
            case TRANSPORTEXECUTIONPLAN:
                hibernateEntityName = "TransportExecutionPlanType";
                break;
            case PPAPREQUEST:
                hibernateEntityName = "PpapRequestType";
                break;
            case PPAPRESPONSE:
                hibernateEntityName = "PpapResponseType";
                break;
            case ITEMINFORMATIONREQUEST:
                hibernateEntityName = "ItemInformationRequestType";
                break;
            case ITEMINFORMATIONRESPONSE:
                hibernateEntityName = "ItemInformationResponseType";
                break;
            default:
                break;
        }
        String query = "SELECT document FROM " + hibernateEntityName + " document "
                + " WHERE document.ID = :documentId";

//        List resultSet = HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
//                .loadAll(query);
        List resultSet = SpringBridge.getInstance().getCatalogueRepository().getEntities(query, new String[]{"documentId"}, new Object[]{documentID});

        if (resultSet.size() > 0) {
            Object document = resultSet.get(0);

            return document;
        }

        return null;
    }

    public static Object getUBLDocument(String documentID) {
        ProcessDocumentMetadataDAO processDocumentMetadataDAO = DAOUtility.getProcessDocumentMetadata(documentID);
        logger.debug(" $$$ Document metadata for {} is {}...", documentID, processDocumentMetadataDAO);
        return getUBLDocument(documentID, processDocumentMetadataDAO.getType());
    }

    public static ProcessDocumentMetadata getCorrespondingResponseMetadata(String documentID, DocumentType documentType) {
        String id = "";
        if (documentType == DocumentType.ORDER) {
//            String query = "SELECT orderResponse.ID FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = '"+documentID+"'";
//            id = (String) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query);
            id = SpringBridge.getInstance().getCatalogueRepository().getOrderResponseId(documentID);
        }
        return getDocumentMetadata(id);
    }

    public static Object getResponseDocument(String documentID, DocumentType documentType) {
        Object document = null;
        if (documentType == DocumentType.ORDER) {
//            String query = "SELECT orderResponse FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = '"+documentID+"'";
//            document = (OrderRes,ponseSimpleType) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query);
            document = SpringBridge.getInstance().getCatalogueRepository().getOrderResponseSimple(documentID);
        }
        return document;
    }

    public static ProcessDocumentMetadata getRequestMetadata(String processInstanceID) {
//        ProcessDocumentMetadataDAO processDocumentDAO = (ProcessDocumentMetadataDAO) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query);
        ProcessDocumentMetadataDAO processDocumentDAO = (ProcessDocumentMetadataDAO) SpringBridge.getInstance().getBusinessProcessRepository().getEntities(QUERY_GET_REQUEST_METADATA, new String[]{"processInstanceId"}, new Object[]{processInstanceID}).get(0);
        return HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
    }

    public static ProcessDocumentMetadata getResponseMetadata(String processInstanceID) {
//        ProcessDocumentMetadataDAO processDocumentDAO = (ProcessDocumentMetadataDAO) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query);
        List<ProcessDocumentMetadataDAO> processDocumentDAO = SpringBridge.getInstance().getBusinessProcessRepository().getEntities(QUERY_GET_RESPONSE_METADATA, new String[]{"processInstanceId"}, new Object[]{processInstanceID});
        if (processDocumentDAO == null || processDocumentDAO.size() == 0) {
            return null;
        }
        return HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO.get(0));
    }

    public static boolean documentExists(String documentID) {
//        String query = "SELECT count(*) FROM ProcessDocumentMetadataDAO document WHERE document.documentID = '" + documentID + "'";
//        int count = ((Long) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query)).intValue();
        int count = ((Long) SpringBridge.getInstance().getBusinessProcessRepository().getSingleEntity(QUERY_PROCESS_METADATA_DOCUMENT_EXISTS, new String[]{"documentId"}, new Object[]{documentID})).intValue();
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }
}
