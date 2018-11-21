package eu.nimble.service.bp.impl.persistence.catalogue;

import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
public interface CatalogueRepository extends JpaRepository<CatalogueType, Long>, GenericCatalogueRepository {
    @Query(value = "SELECT party FROM PartyType party WHERE party.ID = :id ORDER BY party.hjid ASC")
    List<PartyType> getPartyByID(@Param("id") String id);
}