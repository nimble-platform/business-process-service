package eu.nimble.service.bp.impl.util;

import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.swagger.model.ApplicationConfiguration;
import eu.nimble.service.bp.swagger.model.ExecutionConfiguration;
import eu.nimble.service.bp.swagger.model.ProcessDocumentMetadata;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.despatchadvice.DespatchAdviceType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.JAXBUtility;
import org.camunda.bpm.engine.ProcessEngines;

import java.util.List;

/**
 * Created by yildiray on 6/7/2017.
 */
public class DocumentDAOUtility {
    public static ExecutionConfiguration getExecutionConfiguration(String partnerID, String processID, ApplicationConfiguration.TypeEnum applicationType) {
        String processKey = ProcessEngines.getDefaultProcessEngine().getRepositoryService().getProcessDefinition(processID).getKey();

        ApplicationConfigurationDAO applicationConfigurationDAO = null;
        // Get buyer order application preference for data adapter
        ProcessApplicationConfigurationsDAO processConfigurations = DAOUtility.getProcessApplicationConfigurationsDAOByPartnerID(partnerID, processKey);

        if (processConfigurations != null) { // it is configured
            List<ApplicationConfigurationDAO> configurations = processConfigurations.getApplicationConfigurations();
            for (ApplicationConfigurationDAO configuration : configurations) {
                if (configuration.getType().value().equals(
                        applicationType.toString()
                )) {
                    applicationConfigurationDAO = configuration;
                    break;
                }
            }
        } else { // it is not configured by the partner
            applicationConfigurationDAO = new ApplicationConfigurationDAO();
            applicationConfigurationDAO.setActivityID("ActivityID");
            applicationConfigurationDAO.setName("ActivityName");
            applicationConfigurationDAO.setType(ApplicationType.fromValue(applicationType.toString()));
            applicationConfigurationDAO.setTransactionName("TransactionName");

            ExecutionConfigurationDAO applicationExecutionDAO = new ExecutionConfigurationDAO();
            applicationExecutionDAO.setURI("eu.nimble.service.bp.application.UBLDataAdapterApplication");
            applicationExecutionDAO.setType(ApplicationExecutionType.JAVA);
            applicationConfigurationDAO.setExecution(applicationExecutionDAO);
        }

        ApplicationConfiguration applicationConfiguration = HibernateSwaggerObjectMapper.createApplicationConfiguration(applicationConfigurationDAO);
        return applicationConfiguration.getExecution();
    }

    public static void addDocumentWithMetadata(ProcessDocumentMetadata documentMetadata, Object document) {
        ProcessDocumentMetadataDAO processDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocumentMetadata_DAO(documentMetadata);
        HibernateUtility.getInstance("bp-data-model").persist(processDocumentDAO);

        if (document != null)
            HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(document);
    }

    public static void updateDocumentMetadata(ProcessDocumentMetadata body) {
        ProcessDocumentMetadataDAO storedDocumentDAO = DAOUtility.getProcessDocumentMetadata(body.getDocumentID());

        ProcessDocumentMetadataDAO newDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocumentMetadata_DAO(body);

        newDocumentDAO.setHjid(storedDocumentDAO.getHjid());

        HibernateUtility.getInstance("bp-data-model").update(newDocumentDAO);
    }

    public static ProcessDocumentMetadata getDocumentMetadata(String documentID) {
        ProcessDocumentMetadataDAO processDocumentDAO = DAOUtility.getProcessDocumentMetadata(documentID);
        ProcessDocumentMetadata processDocument = HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
        return processDocument;
    }

    public static void deleteDocumentWithMetadata(String documentID) {
        ProcessDocumentMetadataDAO processDocumentMetadataDAO = DAOUtility.getProcessDocumentMetadata(documentID);
        HibernateUtility.getInstance("bp-data-model").delete(ProcessDocumentMetadataDAO.class, processDocumentMetadataDAO.getHjid());

        String type = processDocumentMetadataDAO.getType().toString().toLowerCase();
        type = Character.toUpperCase(type.charAt(0)) + type.substring(1);

        String query = "SELECT document FROM " + type + "Type document "
                + " WHERE document.ID = '" + documentID + "'";

        List resultSet = HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);

        if(resultSet.size() > 0) {
            Object document = resultSet.get(0);
            switch (processDocumentMetadataDAO.getType()) {
                case ORDER:
                    HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(OrderType.class, ((OrderType) document).getHjid());
                    break;
                case INVOICE:
                    break;
                case CATALOGUE:
                    HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(CatalogueType.class, ((CatalogueType) document).getHjid());
                    break;
                case QUOTATION:
                    HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(QuotationType.class, ((QuotationType) document).getHjid());
                    break;
                case ORDERRESPONSE:
                    HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(OrderResponseSimpleType.class, ((OrderResponseSimpleType) document).getHjid());
                    break;
                case RECEIPTADVICE:
                    HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(ReceiptAdviceType.class, ((ReceiptAdviceType) document).getHjid());
                    break;
                case DESPATCHADVICE:
                    HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(DespatchAdviceType.class, ((DespatchAdviceType) document).getHjid());
                    break;
                case REMITTANCEADVICE:
                    break;
                case APPLICATIONRESPONSE:
                    break;
                case REQUESTFORQUOTATION:
                    HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(RequestForQuotationType.class, ((RequestForQuotationType) document).getHjid());
                    break;
                case TRANSPORTATIONSTATUS:
                    break;
                default:
                    break;
            }
        }
    }

    public static String createJsonContent(String documentID) {
        ProcessDocumentMetadataDAO processDocumentMetadataDAO = DAOUtility.getProcessDocumentMetadata(documentID);
        HibernateUtility.getInstance("bp-data-model").delete(ProcessDocumentMetadataDAO.class, processDocumentMetadataDAO.getHjid());

        String type = processDocumentMetadataDAO.getType().toString().toLowerCase();
        type = Character.toUpperCase(type.charAt(0)) + type.substring(1);

        String query = "SELECT document FROM " + type + "Type document "
                + " WHERE document.ID = '" + documentID + "'";

        List resultSet = HibernateUtility.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .loadAll(query);

        // TODO: Create the JSON content to be sent
        String jsonContent = null;
        if(resultSet.size() > 0) {
            Object document = resultSet.get(0);
            switch (processDocumentMetadataDAO.getType()) {
                case ORDER:
                    break;
                case INVOICE:
                    break;
                case CATALOGUE:
                    break;
                case QUOTATION:
                    break;
                case ORDERRESPONSE:
                    break;
                case RECEIPTADVICE:
                    break;
                case DESPATCHADVICE:
                    break;
                case REMITTANCEADVICE:
                    break;
                case APPLICATIONRESPONSE:
                    break;
                case REQUESTFORQUOTATION:
                    break;
                case TRANSPORTATIONSTATUS:
                    break;
                default:
                    break;
            }
        }

        return jsonContent;
    }
}
