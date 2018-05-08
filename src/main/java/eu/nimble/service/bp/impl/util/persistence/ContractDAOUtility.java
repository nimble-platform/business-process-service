package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ClauseType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ContractType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DataMonitoringClauseType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentClauseType;
import eu.nimble.utility.Configuration;
import net.sf.saxon.expr.flwor.Clause;

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
            if (baseClause.getType().contentEquals(eu.nimble.service.bp.impl.clause.ClauseType.DATA_MONITORING.toString())) {
                clause = getDataMonitoringClause(baseClause.getID());

            } else if (baseClause.getType().contentEquals(eu.nimble.service.bp.impl.clause.ClauseType.ITEM_DETAILS.toString()) ||
                    baseClause.getType().contentEquals(eu.nimble.service.bp.impl.clause.ClauseType.NEGOTIATION.toString()) ||
                    baseClause.getType().contentEquals(eu.nimble.service.bp.impl.clause.ClauseType.PPAP.toString())) {
                clause = getDocumentClause(baseClause.getID());
            }
        }
        return clause;
    }

    public static ClauseType getClause(String documentId, DocumentType documentType, eu.nimble.service.bp.impl.clause.ClauseType clauseType) {
        String query;
        if(documentType.equals(DocumentType.ORDER)) {
            query = "SELECT clause " +
                    "FROM OrderType order " +
                        "join order.contract contract " +
                        "join contract.clause clause " +
                    "WHERE " +
                        "order.ID = '" + documentId + "' and " +
                        "clause.type = '" + clauseType + "'";

        } else if(documentType.equals(DocumentType.TRANSPORTEXECUTIONPLAN)) {
            query = "SELECT clause " +
                    "FROM TransportExecutionPlanRequestType tep_request " +
                        "join tep_request.contract.clause " +
                    "WHERE " +
                    "tep_request.ID = '" + documentId + "' and " +
                    "clause.type = '" + clauseType + "'";

        } else {
            return null;
        }

        ClauseType clause = (ClauseType) HibernateUtilityRef.getInstance(Configuration.UBL_PERSISTENCE_UNIT_NAME).loadIndividualItem(query);
        return clause;
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
}
