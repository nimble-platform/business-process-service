package eu.nimble.service.bp.impl.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import javax.ws.rs.QueryParam;
import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
public interface ProcessDocumentMetadataDAORepository extends JpaRepository<ProcessDocumentMetadataDAO, Long> {
    List<ProcessDocumentMetadataDAO> findByDocumentID(String documentId);

    List<ProcessDocumentMetadataDAO> findByProcessInstanceIDOrderBySubmissionDateAsc(String processInstanceId);

    @Query(value="SELECT DISTINCT metadataDAO.processInstanceID FROM ProcessDocumentMetadataDAO metadataDAO WHERE metadataDAO.responderID = :responderId")
    List<String> getProcessInstanceIds(@QueryParam("responderId") String responderId);
}
