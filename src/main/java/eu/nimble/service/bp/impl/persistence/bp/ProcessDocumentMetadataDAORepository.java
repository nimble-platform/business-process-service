package eu.nimble.service.bp.impl.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDocumentMetadataDAO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
public interface ProcessDocumentMetadataDAORepository extends JpaRepository<ProcessDocumentMetadataDAO, Long> {
    List<ProcessDocumentMetadataDAO> findByDocumentID(String documentId);
}
