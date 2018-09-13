package eu.nimble.service.bp.impl.util.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.util.serialization.Serializer;
import eu.nimble.service.bp.processor.BusinessProcessContext;
import eu.nimble.service.bp.processor.BusinessProcessContextHandler;
import eu.nimble.service.bp.swagger.model.*;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.despatchadvice.DespatchAdviceType;
import eu.nimble.service.model.ubl.iteminformationrequest.ItemInformationRequestType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.service.model.ubl.ppaprequest.PpapRequestType;
import eu.nimble.service.model.ubl.quotation.QuotationType;
import eu.nimble.service.model.ubl.receiptadvice.ReceiptAdviceType;
import eu.nimble.service.model.ubl.requestforquotation.RequestForQuotationType;
import eu.nimble.service.model.ubl.transportexecutionplanrequest.TransportExecutionPlanRequestType;
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

    public static void addDocumentWithMetadata(String processContextId,ProcessDocumentMetadata documentMetadata, Object document) {
        ProcessDocumentMetadataDAO processDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocumentMetadata_DAO(documentMetadata);
        HibernateUtilityRef.getInstance("bp-data-model").persist(processDocumentDAO);
        // save ProcessDocumentMetadataDAO
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId);
        businessProcessContext.setMetadataDAO(processDocumentDAO);

        if (document != null)
        {
            HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(document);
            // save Object
            businessProcessContext.setDocument(document);
        }

    }



    public static void updateDocumentMetadata(String processContextId,ProcessDocumentMetadata body) {
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId);

        ProcessDocumentMetadataDAO storedDocumentDAO = DAOUtility.getProcessDocumentMetadata(body.getDocumentID());

        businessProcessContext.setPreviousDocumentMetadataStatus(storedDocumentDAO.getStatus());

        ProcessDocumentMetadataDAO newDocumentDAO = HibernateSwaggerObjectMapper.createProcessDocumentMetadata_DAO(body);

        HibernateUtilityRef.getInstance("bp-data-model").delete(storedDocumentDAO);
        newDocumentDAO = (ProcessDocumentMetadataDAO) HibernateUtilityRef.getInstance("bp-data-model").update(newDocumentDAO);

        businessProcessContext.setUpdatedDocumentMetadata(newDocumentDAO);

    }

    public static void updateDocument(String processContextId, String content, DocumentType documentType) {
        BusinessProcessContext businessProcessContext = BusinessProcessContextHandler.getBusinessProcessContextHandler().getBusinessProcessContext(processContextId);

        ObjectMapper mapper = Serializer.getDefaultObjectMapper();

        if(documentType == DocumentType.ITEMINFORMATIONREQUEST){
            try {
                ItemInformationRequestType itemInformationRequest = mapper.readValue(content, ItemInformationRequestType.class);

                ItemInformationRequestType existingItemInformationRequest = (ItemInformationRequestType) getUBLDocument(itemInformationRequest.getID(),DocumentType.ITEMINFORMATIONREQUEST);
                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(existingItemInformationRequest);

                businessProcessContext.setPreviousDocument(existingItemInformationRequest);

                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(itemInformationRequest);

                businessProcessContext.setDocument(itemInformationRequest);
            }
            catch (Exception e){
                logger.error("",e);
            }
        }
        if(documentType == DocumentType.DESPATCHADVICE){
            try {
                DespatchAdviceType despatchAdviceType = mapper.readValue(content, DespatchAdviceType.class);

                DespatchAdviceType existingDespatchAdvice = (DespatchAdviceType) getUBLDocument(despatchAdviceType.getID(),DocumentType.DESPATCHADVICE);
                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(existingDespatchAdvice);

                businessProcessContext.setPreviousDocument(existingDespatchAdvice);

                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(despatchAdviceType);

                businessProcessContext.setDocument(despatchAdviceType);
            }
            catch (Exception e){
                logger.error("",e);
            }
        }
        if(documentType == DocumentType.REQUESTFORQUOTATION){
            try {
                RequestForQuotationType requestForQuotationType = mapper.readValue(content, RequestForQuotationType.class);

                RequestForQuotationType existingRequestForQuotation = (RequestForQuotationType) getUBLDocument(requestForQuotationType.getID(),DocumentType.REQUESTFORQUOTATION);
                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(existingRequestForQuotation);

                businessProcessContext.setPreviousDocument(existingRequestForQuotation);

                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(requestForQuotationType);

                businessProcessContext.setDocument(requestForQuotationType);
            }
            catch (Exception e){
                logger.error("",e);
            }
        }
        if(documentType == DocumentType.ORDER){
            try {
                OrderType orderType = mapper.readValue(content, OrderType.class);

                OrderType existingOrderType = (OrderType) getUBLDocument(orderType.getID(),DocumentType.ORDER);
                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(existingOrderType);

                businessProcessContext.setPreviousDocument(existingOrderType);

                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(orderType);

                businessProcessContext.setDocument(orderType);
            }
            catch (Exception e){
                logger.error("",e);
            }
        }
        if(documentType == DocumentType.PPAPREQUEST){
            try {
                PpapRequestType ppapRequestType = mapper.readValue(content, PpapRequestType.class);

                PpapRequestType existingPPAPRequest = (PpapRequestType) getUBLDocument(ppapRequestType.getID(),DocumentType.PPAPREQUEST);
                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(existingPPAPRequest);

                businessProcessContext.setPreviousDocument(existingPPAPRequest);

                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(ppapRequestType);

                businessProcessContext.setDocument(ppapRequestType);
            }
            catch (Exception e){
                logger.error("",e);
            }
        }
        if(documentType == DocumentType.TRANSPORTEXECUTIONPLANREQUEST){
            try {
                TransportExecutionPlanRequestType transportExecutionPlanRequestType = mapper.readValue(content, TransportExecutionPlanRequestType.class);

                TransportExecutionPlanRequestType existingTEPRequest = (TransportExecutionPlanRequestType) getUBLDocument(transportExecutionPlanRequestType.getID(),DocumentType.TRANSPORTEXECUTIONPLANREQUEST);
                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(existingTEPRequest);

                businessProcessContext.setPreviousDocument(existingTEPRequest);

                HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).persist(transportExecutionPlanRequestType);

                businessProcessContext.setDocument(transportExecutionPlanRequestType);
            }
            catch (Exception e){
                logger.error("",e);
            }
        }

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

    public static ProcessDocumentMetadata getCorrespondingResponseMetadata(String documentID, DocumentType documentType){
        String id = "";
        if(documentType == DocumentType.ORDER){
            String query = "SELECT orderResponse.ID FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = '"+documentID+"'";
            id = (String) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query);
        }
        return getDocumentMetadata(id);
    }

    public static ProcessDocumentMetadata getRequestMetadata(String processInstanceID){
        String query = "FROM ProcessDocumentMetadataDAO documentMetadata WHERE documentMetadata.processInstanceID='"+processInstanceID+"' AND (" +
                "documentMetadata.type = 'REQUESTFORQUOTATION' OR documentMetadata.type = 'ORDER' OR documentMetadata.type = 'DESPATCHADVICE' OR " +
                "documentMetadata.type = 'PPAPREQUEST' OR documentMetadata.type = 'TRANSPORTEXECUTIONPLANREQUEST' OR documentMetadata.type = 'ITEMINFORMATIONREQUEST')";
        ProcessDocumentMetadataDAO processDocumentDAO = (ProcessDocumentMetadataDAO) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query);
        return HibernateSwaggerObjectMapper.createProcessDocumentMetadata(processDocumentDAO);
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
