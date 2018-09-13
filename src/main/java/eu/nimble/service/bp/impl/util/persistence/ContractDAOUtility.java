package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.swagger.model.Process;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.service.model.ubl.transportexecutionplanrequest.TransportExecutionPlanRequestType;
import eu.nimble.utility.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by suat on 26-Apr-18.
 */
public class ContractDAOUtility {
    public static boolean clauseExists(String clauseId) {
        String query = "SELECT count(*) FROM ClauseType contract WHERE clause.ID = '" + clauseId + "'";
        int count = ((Long) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query)).intValue();
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean contractHasClause(String contractId, String clauseId) {
        String query = "SELECT count(*) FROM ContractType contract join contract.clause clause WHERE contract.ID = '" + contractId + "' and clause.ID = '" + clauseId + "'";
        int count = ((Long) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query)).intValue();
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static ClauseType getBaseClause(String clauseId) {
        String query = "SELECT clause FROM ClauseType clause WHERE clause.ID = '" + clauseId + "'";
        ClauseType clauseType = (ClauseType) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query);
        return clauseType;
    }

    public static ClauseType getClause(String clauseId) {
        ClauseType baseClause = getBaseClause(clauseId);
        ClauseType clause = null;
        if (baseClause != null) {
            if (baseClause.getType().contentEquals(eu.nimble.service.bp.impl.model.ClauseType.DATA_MONITORING.toString())) {
                clause = getDataMonitoringClause(baseClause.getID());

            } else if (baseClause.getType().contentEquals(eu.nimble.service.bp.impl.model.ClauseType.ITEM_DETAILS.toString()) ||
                    baseClause.getType().contentEquals(eu.nimble.service.bp.impl.model.ClauseType.NEGOTIATION.toString()) ||
                    baseClause.getType().contentEquals(eu.nimble.service.bp.impl.model.ClauseType.PPAP.toString())) {
                clause = getDocumentClause(baseClause.getID());
            }
        }
        return clause;
    }

    public static List<ClauseType> getClause(String documentId, DocumentType documentType, eu.nimble.service.bp.impl.model.ClauseType clauseType) {
        List<ClauseType> clauseTypes = new ArrayList<>();
        if(documentType.equals(DocumentType.ORDER)) {
            OrderType orderType = (OrderType) DocumentDAOUtility.getUBLDocument(documentId,DocumentType.ORDER);
            for(ContractType contractType : orderType.getContract()){
                for(ClauseType clause : contractType.getClause()){
                    if(eu.nimble.service.bp.impl.model.ClauseType.valueOf(clause.getType()) == clauseType){
                        clauseTypes.add(clause);
                    }
                }
            }

        } else if(documentType.equals(DocumentType.TRANSPORTEXECUTIONPLANREQUEST)) {
            TransportExecutionPlanRequestType transportExecutionPlanRequestType = (TransportExecutionPlanRequestType) DocumentDAOUtility.getUBLDocument(documentId,DocumentType.TRANSPORTEXECUTIONPLANREQUEST);
            ContractType contractType = transportExecutionPlanRequestType.getTransportContract();
            if(contractType == null){
                return null;
            }
            for(ClauseType clause : contractType.getClause()){
                if(eu.nimble.service.bp.impl.model.ClauseType.valueOf(clause.getType()) == clauseType){
                    clauseTypes.add(clause);
                }
            }
        } else {
            return null;
        }

        return clauseTypes;
    }

    public static ClauseType getContractClause(String contractId, String clauseId) {
        String query = "SELECT clause FROM ContractType contract join contract.clause clause WHERE contract.ID = '" + contractId + "' and clause.ID = '" + clauseId + "'";
        ClauseType clause = (ClauseType) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query);
        return clause;
    }

    public static DataMonitoringClauseType getDataMonitoringClause(String clauseId) {
        String query = "SELECT clause from DataMonitoringClauseType clause where clause.ID = '" + clauseId + "'";
        DataMonitoringClauseType clause = (DataMonitoringClauseType) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query);
        return clause;
    }

    public static DocumentClauseType getDocumentClause(String clauseId) {
        String query = "SELECT clause from DocumentClauseType clause where clause.ID = '" + clauseId + "'";
        DocumentClauseType clause = (DocumentClauseType) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query);
        return clause;
    }

    public static void deleteClause(ClauseType clause) {
        HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).delete(clause);
    }

    public static boolean contractExists(String contractID) {
        String query = "SELECT count(*) FROM ContractType contract WHERE contract.ID = '" + contractID + "'";
        int count = ((Long) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query)).intValue();
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    public static ContractType getContract(String contractId) {
        String query = "SELECT contract from ContractType contract where contract.ID = '" + contractId + "'";
        ContractType contract = (ContractType) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query);
        return contract;
    }

    /**
     * Starting from the specified {@link ProcessInstanceDAO}, this method traverses the previous
     * process instances until the beginning and add the response document of each process instances as a
     * {@link DocumentClauseType}s.
     *
     * @param processInstance
     * @return
     */
    public static ContractType constructContractForProcessInstances(ProcessInstanceDAO processInstance) {
        ContractType realContract = null;

        ContractType contract = new ContractType();
        contract.setID(UUID.randomUUID().toString());

        boolean negotiationClauseFound = false;
        boolean ppapClauseFound = false;

        do {
            List<ProcessDocumentMetadataDAO> documents = DAOUtility.getProcessDocumentMetadataByProcessInstanceID(processInstance.getProcessInstanceID());

            // if the process is completed
            if(documents.size() > 1) {
                ProcessDocumentMetadataDAO docMetadata = documents.get(1);
                ProcessDocumentMetadataDAO reqMetadata = documents.get(0);
                // if the second document has a future submission date
                if (documents.get(0).getSubmissionDate().compareTo(documents.get(1).getSubmissionDate()) > 0) {
                    docMetadata = documents.get(0);
                    reqMetadata = documents.get(1);
                }

                // Check whether a contract already exists or not
                if(reqMetadata.getType().equals(DocumentType.ORDER)){
                    OrderType orderType = (OrderType) DocumentDAOUtility.getUBLDocument(reqMetadata.getDocumentID(),DocumentType.ORDER);
                    if(orderType.getContract().size() > 0){
                        realContract = orderType.getContract().get(0);
                    }
                    break;
                }
                else if(reqMetadata.getType().equals(DocumentType.TRANSPORTEXECUTIONPLANREQUEST)){
                    TransportExecutionPlanRequestType transportExecutionPlanRequestType = (TransportExecutionPlanRequestType) DocumentDAOUtility.getUBLDocument(reqMetadata.getDocumentID(),DocumentType.TRANSPORTEXECUTIONPLANREQUEST);
                    realContract = transportExecutionPlanRequestType.getTransportContract();
                    break;
                }

                DocumentType documentType = docMetadata.getType();
                if(documentType.equals(DocumentType.ITEMINFORMATIONRESPONSE) ||
                   documentType.equals(DocumentType.QUOTATION) ||
                   documentType.equals(DocumentType.PPAPRESPONSE)) {

                    DocumentClauseType clause = new DocumentClauseType();
                    contract.getClause().add(clause);
                    DocumentReferenceType docRef = new DocumentReferenceType();
                    clause.setClauseDocumentRef(docRef);

                    clause.setID(UUID.randomUUID().toString());
                    docRef.setID(docMetadata.getDocumentID());

                    if (docMetadata.getType().equals(DocumentType.ITEMINFORMATIONRESPONSE)) {
                        clause.setType(eu.nimble.service.bp.impl.model.ClauseType.ITEM_DETAILS.toString());
                        docRef.setDocumentType(DocumentType.ITEMINFORMATIONRESPONSE.toString());

                    } else if (docMetadata.getType().equals(DocumentType.QUOTATION)) {
                        clause.setType(eu.nimble.service.bp.impl.model.ClauseType.NEGOTIATION.toString());
                        docRef.setDocumentType(DocumentType.QUOTATION.toString());
                        negotiationClauseFound = true;

                    } else if (docMetadata.getType().equals(DocumentType.PPAPRESPONSE)) {
                        clause.setType(eu.nimble.service.bp.impl.model.ClauseType.PPAP.toString());
                        docRef.setDocumentType(DocumentType.PPAPRESPONSE.toString());
                        ppapClauseFound = true;
                    }
                }
            }

            // keep only the latest negotiation and ppap clauses
            boolean skipNextInstance;
            do {
                skipNextInstance = false;
                processInstance = processInstance.getPrecedingProcess();
                if (processInstance != null) {
                    if (negotiationClauseFound && processInstance.getProcessID().equalsIgnoreCase(Process.ProcessTypeEnum.NEGOTIATION.toString())) {
                        skipNextInstance = true;
                    } else if (ppapClauseFound && processInstance.getProcessID().equalsIgnoreCase(Process.ProcessTypeEnum.PPAP.toString())) {
                        skipNextInstance = true;
                    }
                }
            } while (skipNextInstance == true) ;

        } while (processInstance != null);

        // Add new clauses to the contract
        if(realContract != null){
            for (ClauseType clause : contract.getClause()){
                realContract.getClause().add(clause);
            }
        }
        else {
            return contract;
        }
        return realContract;
    }
}
