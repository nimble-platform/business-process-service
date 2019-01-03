package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.DocumentType;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentStatus;
import eu.nimble.service.bp.hyperjaxb.model.RoleType;
import eu.nimble.service.bp.impl.model.statistics.BusinessProcessCount;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 16-Oct-18.
 */
public class ProcessDocumentMetadataDAOUtility {
    private static final String QUERY_GET_BY_DOCUMENT_ID = "SELECT pdm FROM ProcessDocumentMetadataDAO pdm WHERE pdm.documentID = :documentId";
    private static final String QUERY_GET_BY_PROCESS_INSTANCE_ID = "SELECT pdm FROM ProcessDocumentMetadataDAO pdm WHERE pdm.processInstanceID = :processInstanceId ORDER BY pdm.submissionDate ASC";
    private static final String QUERY_GET_BY_RESPONDER_ID = "SELECT DISTINCT metadataDAO.processInstanceID FROM ProcessDocumentMetadataDAO metadataDAO WHERE metadataDAO.responderID = :responderId";

    /**
     * The conditions for the queries below are initialized during the query instantiation
     */
    private static final String QUERY_GET_TRANSACTION_COUNT = "SELECT count(*) FROM ProcessDocumentMetadataDAO documentMetadata %s";
    private static final String QUERY_GET_DOCUMENT_IDS = "SELECT documentMetadata.documentID FROM ProcessDocumentMetadataDAO documentMetadata %s";
    private static final String QUERY_GET_GROUPED_TRANSACTIONS = "SELECT documentMetadata.initiatorID, documentMetadata.type, documentMetadata.status, count(*) FROM ProcessDocumentMetadataDAO documentMetadata %s";
    private static final String QUERY_GET = "SELECT document FROM ProcessDocumentMetadataDAO document WHERE (%s)";

    private static final Logger logger = LoggerFactory.getLogger(ProcessDocumentMetadataDAOUtility.class);

    public static ProcessDocumentMetadataDAO findByDocumentID(String documentId) {
        return new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_BY_DOCUMENT_ID, new String[]{"documentId"}, new Object[]{documentId});
    }

    public static List<ProcessDocumentMetadataDAO> findByProcessInstanceID(String processInstanceId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_PROCESS_INSTANCE_ID, new String[]{"processInstanceId"}, new Object[]{processInstanceId});
    }

    public static List<String> getProcessInstanceIds(String responderId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_RESPONDER_ID, new String[]{"responderId"}, new Object[]{responderId});
    }

    public static ProcessDocumentMetadataDAO getDocumentOfTheOtherParty(String processInstanceId, String thisPartyId) {
        List<ProcessDocumentMetadataDAO> documentMetadataDAOs = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(processInstanceId);
        // if this party is the initiator party, return the second document metadata
        if(documentMetadataDAOs.get(0).getInitiatorID().contentEquals(thisPartyId)) {
            if(documentMetadataDAOs.size() > 1) {
                return documentMetadataDAOs.get(1);
            } else {
                return null;
            }
        } else {
            return documentMetadataDAOs.get(0);
        }
    }

    public static String getTradingPartnerId(String processInstanceId, String thisPartyId) {
        ProcessDocumentMetadataDAO firstDocumentMetadataDAO = ProcessDocumentMetadataDAOUtility.findByProcessInstanceID(processInstanceId).get(0);
        return getTradingPartnerId(firstDocumentMetadataDAO, thisPartyId);
    }

    public static String getTradingPartnerId(ProcessDocumentMetadataDAO documentMetadata, String thisPartyId) {
        if(documentMetadata.getInitiatorID().contentEquals(thisPartyId)) {
            return documentMetadata.getResponderID();
        } else {
            return documentMetadata.getInitiatorID();
        }
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadata(String partnerID, String type) {
        return getProcessDocumentMetadata(partnerID, type, null, null);
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadata(String partnerID, String type, String source) {
        return getProcessDocumentMetadata(partnerID, type, null, source);
    }

    public static List<ProcessDocumentMetadataDAO> getProcessDocumentMetadata(String partnerID, String type, String status, String source) {
        List<String> parameterNames = new ArrayList<>();
        List<String> parameterValues = new ArrayList<>();
        String conditions = "";

        if (source != null && partnerID != null) {
            String attribute = source.equals("SENT") ? "initiatorID" : "responderID";
            conditions += " document." + attribute + " = :partnerId ";
            parameterNames.add("partnerId");
            parameterValues.add(partnerID);

        } else if (source == null && partnerID != null) {
            conditions += " (document.initiatorID = :partnerId or document.responderID = :partnerId) ";
            parameterNames.add("partnerId");
            parameterValues.add(partnerID);
        }

        if (type != null) {
            conditions += " and document.type = '" + DocumentType.valueOf(type).toString() + "' ";
        }

        if (status != null) {
            conditions += " and document.status = '" + ProcessDocumentStatus.valueOf(status).toString() + "'";
        }

        String query = String.format(QUERY_GET, conditions);
        List<ProcessDocumentMetadataDAO> resultSet = new JPARepositoryFactory().forBpRepository().getEntities(query, parameterNames.toArray(new String[parameterNames.size()]), parameterValues.toArray(new String[parameterValues.size()]));
        return resultSet;
    }

    public static List<String> getDocumentIds(Integer partyId, List<String> documentTypes, String role, String startDateStr, String endDateStr, String status) {
        DocumentMetadataQuery query = getDocumentMetadataQuery(partyId, documentTypes, role, startDateStr, endDateStr, status, DocumentMetadataQueryType.DOCUMENT_IDS);
        List<String> documentIds = new JPARepositoryFactory().forBpRepository().getEntities(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray());
        return documentIds;
    }

    public static int getTransactionCount(Integer partyId, List<String> documentTypes, String role, String startDateStr, String endDateStr, String status) {
        DocumentMetadataQuery query = getDocumentMetadataQuery(partyId, documentTypes, role, startDateStr, endDateStr, status, DocumentMetadataQueryType.TOTAL_TRANSACTION_COUNT);
        int count = ((Long) new JPARepositoryFactory().forBpRepository().getSingleEntity(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray())).intValue();
        return count;
    }

    public static BusinessProcessCount getGroupTransactionCounts(Integer partyId, String startDateStr, String endDateStr, String role, String bearerToken) {
        DocumentMetadataQuery query = getDocumentMetadataQuery(partyId, new ArrayList<>(), role, startDateStr, endDateStr, null, DocumentMetadataQueryType.GROUPED_TRANSACTION_COUNT);
        List<Object> results = new JPARepositoryFactory().forBpRepository().getEntities(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray());

        BusinessProcessCount counts = new BusinessProcessCount();
        for (Object result : results) {
            Object[] resultItems = (Object[]) result;
            PartyType partyType = null;
            try {
                partyType = SpringBridge.getInstance().getIdentityClientTyped().getParty(bearerToken, (String) resultItems[0]);
            } catch (IOException e) {
                String msg = String.format("Failed to get transaction counts for party: %s, role: %s", partyId, role);
                logger.error("msg");
                throw new RuntimeException(msg, e);
            }
            counts.addCount((String) resultItems[0], resultItems[1].toString(), resultItems[2].toString(), (Long) resultItems[3], partyType.getName());
        }
        return counts;
    }

    private static DocumentMetadataQuery getDocumentMetadataQuery(Integer partyId, List<String> documentTypes, String role, String startDateStr, String endDateStr, String status, DocumentMetadataQueryType queryType) {
        DocumentMetadataQuery query = new DocumentMetadataQuery();
        List<String> parameterNames = query.parameterNames;
        List<Object> parameterValues = query.parameterValues;

        String conditions = "";
        DateTimeFormatter sourceFormatter = DateTimeFormat.forPattern("dd-MM-yyyy");
        DateTimeFormatter bpFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

        boolean filterExists = false;

        if (partyId != null) {
            String attribute = role.equals(RoleType.BUYER.toString()) ? "initiatorID" : "responderID";
            conditions += " where documentMetadata." + attribute + " = :partyId ";
            filterExists = true;

            parameterNames.add("partyId");
            parameterValues.add(partyId.toString());
        }

        if (startDateStr != null || endDateStr != null) {
            if (!filterExists) {
                conditions += " where";
            } else {
                conditions += " and";
            }

            if (startDateStr != null && endDateStr != null) {
                DateTime startDate = sourceFormatter.parseDateTime(startDateStr);
                DateTime endDate = sourceFormatter.parseDateTime(endDateStr);
                endDate = endDate.plusDays(1).minusMillis(1);
                conditions += " documentMetadata.submissionDate between :startTime and :endTime";

                parameterNames.add("startTime");
                parameterValues.add(bpFormatter.print(startDate));
                parameterNames.add("endTime");
                parameterValues.add(bpFormatter.print(endDate));
            } else if (startDateStr != null) {
                DateTime startDate = sourceFormatter.parseDateTime(startDateStr);
                conditions += " documentMetadata.submissionDate >= :startTime";

                parameterNames.add("startTime");
                parameterValues.add(bpFormatter.print(startDate));
            } else {
                DateTime endDate = sourceFormatter.parseDateTime(endDateStr);
                endDate = endDate.plusDays(1).minusMillis(1);
                conditions += " documentMetadata.submissionDate <= :endTime";

                parameterNames.add("endTime");
                parameterValues.add(bpFormatter.print(endDate));
            }
            filterExists = true;
        }

        if (documentTypes.size() > 0) {
            if (!filterExists) {
                conditions += " where (";
            } else {
                conditions += " and(";
            }
            for (int i = 0; i < documentTypes.size() - 1; i++) {
                conditions += " documentMetadata.type = '" + DocumentType.valueOf(documentTypes.get(i)).toString() + "' or";
            }
            conditions += " documentMetadata.type = '" + DocumentType.valueOf(documentTypes.get(documentTypes.size() - 1)).toString() + "')";
            filterExists = true;
        }

        if (status != null) {
            if (!filterExists) {
                conditions += " where ";
            } else {
                conditions += " and ";
            }
            conditions += " documentMetadata.status = '" + ProcessDocumentStatus.valueOf(status).toString() + "'";
        }

        if (queryType.equals(DocumentMetadataQueryType.GROUPED_TRANSACTION_COUNT)) {
            conditions += " group by documentMetadata.initiatorID, documentMetadata.type, documentMetadata.status";
        }

        if (queryType.equals(DocumentMetadataQueryType.TOTAL_TRANSACTION_COUNT)) {
            query.query = String.format(QUERY_GET_TRANSACTION_COUNT, conditions);
        } else if (queryType.equals(DocumentMetadataQueryType.DOCUMENT_IDS)) {
            query.query = String.format(QUERY_GET_DOCUMENT_IDS, conditions);
        } else if (queryType.equals(DocumentMetadataQueryType.GROUPED_TRANSACTION_COUNT)) {
            query.query = String.format(QUERY_GET_GROUPED_TRANSACTIONS, conditions);
        }
        return query;
    }


    private enum DocumentMetadataQueryType {
        DOCUMENT_IDS, TOTAL_TRANSACTION_COUNT, GROUPED_TRANSACTION_COUNT
    }

    private static class DocumentMetadataQuery {
        private String query;
        private List<String> parameterNames = new ArrayList<>();
        private List<Object> parameterValues = new ArrayList<>();
    }
}
