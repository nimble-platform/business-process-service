package eu.nimble.service.bp.impl.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
public interface ProcessDAORepository extends JpaRepository<ProcessDAO, Long> {
    List<ProcessDAO> findByProcessID(String processId);
}
