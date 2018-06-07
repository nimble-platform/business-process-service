package eu.nimble.service.bp.impl.util.persistence;


import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.swagger.model.*;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.despatchadvice.DespatchAdviceType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.Configuration;
import org.camunda.bpm.engine.ProcessEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by yildiray on 6/7/2017.
 */
public class DocumentDAOUtility {
    private static Logger logger = LoggerFactory.getLogger(DocumentDAOUtility.class);
    public static ExecutionConfiguration getExecutionConfiguration(String partnerID, String processID, ProcessConfiguration.RoleTypeEnum roleType, String transactionID, ExecutionConfiguration.ApplicationTypeEnum applicationType) {
        String processKey = ProcessEngines.getDefaultProcessEngine().getRepositoryService().getProcessDefinition(processID).getKey();

        TransactionConfigurationDAO transactionConfigurationDAO = null;
        ExecutionConfigurationDAO executionConfigurationDAO = null;
        // Get buyer order application preference for data adapter
        ProcessConfigurationDAO processConfiguration = DAOUtility.getProcessConfiguration(partnerID, processKey, roleType);

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
            transactionConfigurationDAO = new TransactionConfigurationDAO();
            // TODO: Retrieve it from the identity service or context
            transactionConfigurationDAO.setTransactionID(transactionID);

            executionConfigurationDAO = new ExecutionConfigurationDAO();
            executionConfigurationDAO.setExecutionUri("eu.nimble.service.bp.application.ubl.UBLDataAdapterApplication");
            executionConfigurationDAO.setExecutionType(ApplicationExecutionType.JAVA);
            executionConfigurationDAO.setApplicationType(ApplicationType.fromValue(applicationType.toString()));
            transactionConfigurationDAO.getExecutionConfigurations().add(executionConfigurationDAO);
        }

        ExecutionConfiguration executionConfiguration = HibernateSwaggerObjectMapper.createExecutionConfiguration(executionConfigurationDAO);
        return executionConfiguration;
    }

    public static void addDocumentWithMetadata(ProcessDocumentMetadata documentMetadata, Object document) {
        ProcessDocumentMetadataDAO processDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocumentMetadata_DAO(documentMetadata);
        HibernateUtilityRef.getInstance("bp-data-model").persist(processDocumentDAO);

        if (document != null)
            HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(document);
    }



    public static void updateDocumentMetadata(ProcessDocumentMetadata body) {
        ProcessDocumentMetadataDAO storedDocumentDAO = DAOUtility.getProcessDocumentMetadata(body.getDocumentID());

        ProcessDocumentMetadataDAO newDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocumentMetadata_DAO(body);

        newDocumentDAO.setHjid(storedDocumentDAO.getHjid());

        HibernateUtilityRef.getInstance("bp-data-model").update(newDocumentDAO);
    }

    public static ProcessDocumentMetadata getDocumentMetadata(String documentID) {
        ProcessDocumentMetadataDAO processDocumentDAO = DAOUtility.getProcessDocumentMetadata(documentID);
        ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
        return processDocument;
    }

    public static void deleteDocumentWithMetadata(String documentID) {
        ProcessDocumentMetadataDAO processDocumentMetadataDAO = DAOUtility.getProcessDocumentMetadata(documentID);

        Object document = getUBLDocument(documentID, processDocumentMetadataDAO.getType());

        if(document != null) {
            switch (processDocumentMetadataDAO.getType()) {
                case ORDER:
                    HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(OrderType.class, ((OrderType) document).getHjid());
                    break;
                case INVOICE:
                    break;
                case CATALOGUE:
                    HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(CatalogueType.class, ((CatalogueType) document).getHjid());
                    break;
                case QUOTATION:
                    HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(QuotationType.class, ((QuotationType) document).getHjid());
                    break;
                case ORDERRESPONSESIMPLE:
                    HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(OrderResponseSimpleType.class, ((OrderResponseSimpleType) document).getHjid());
                    break;
                case RECEIPTADVICE:
                    HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(ReceiptAdviceType.class, ((ReceiptAdviceType) document).getHjid());
                    break;
                case DESPATCHADVICE:
                    HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(DespatchAdviceType.class, ((DespatchAdviceType) document).getHjid());
                    break;
                case REMITTANCEADVICE:
                    break;
                case APPLICATIONRESPONSE:
                    break;
                case REQUESTFORQUOTATION:
                    HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(RequestForQuotationType.class, ((RequestForQuotationType) document).getHjid());
                    break;
                case TRANSPORTATIONSTATUS:
                    break;
                default:
                    break;
            }
        }
        HibernateUtilityRef.getInstance("bp-data-model").delete(ProcessDocumentMetadataDAO.class, processDocumentMetadataDAO.getHjid());
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
                + " WHERE document.ID = '" + documentID + "'";

        List resultSet = HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);

        if(resultSet.size() > 0) {
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

    public static boolean documentExists(String documentID) {
        String query = "SELECT count(*) FROM ProcessDocumentMetadataDAO document WHERE document.documentID = '" + documentID + "'";
        int count = ((Long) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query)).intValue();
        if(count > 0) {
            return true;
        } else {
            return false;
        }
    }
}
