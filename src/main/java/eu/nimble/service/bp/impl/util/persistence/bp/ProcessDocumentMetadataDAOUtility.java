package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

/**
 * Created by suat on 01-Jan-19.
 */
public class ProcessDocumentMetadataDAOUtility {
    private static final String QUERY_GET_BY_DOCUMENT_ID = "SELECT pdm FROM ProcessDocumentMetadataDAO pdm WHERE pdm.documentID = :documentId";
    private static final String QUERY_GET_BY_PROCESS_INSTANCE_ID = "SELECT pdm FROM ProcessDocumentMetadataDAO pdm WHERE pdm.processInstanceID = :processInstanceId ORDER BY pdm.submissionDate ASC";
    private static final String QUERY_GET_BY_RESPONDER_ID = "SELECT DISTINCT metadataDAO.processInstanceID FROM ProcessDocumentMetadataDAO metadataDAO WHERE metadataDAO.responderID = :responderId";

    public static List<ProcessDocumentMetadataDAO> findByDocumentID(String documentId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_DOCUMENT_ID, new String[]{"documentId"}, new Object[]{documentId});
    }

    public static List<ProcessDocumentMetadataDAO> findByProcessInstanceIDOrderBySubmissionDateAsc(String processInstanceId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_PROCESS_INSTANCE_ID, new String[]{"processInstanceId"}, new Object[]{processInstanceId});
    }

    public static List<String> getProcessInstanceIds(String responderId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_RESPONDER_ID, new String[]{"responderId"}, new Object[]{responderId});
    }
}
