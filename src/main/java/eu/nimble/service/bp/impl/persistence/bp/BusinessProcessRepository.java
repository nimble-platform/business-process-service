package eu.nimble.service.bp.impl.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
public interface BusinessProcessRepository extends JpaRepository<ProcessInstanceDAO, Long> {
    @Query(value = "select c from ProcessInstanceGroupDAO c where c.hjid = :uuid")
    List<ProcessInstanceGroupDAO> getCatalogue(@Param("uuid") Long uuid);
}
