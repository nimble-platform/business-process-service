package eu.nimble.service.bp.impl.persistence.catalogue;

import eu.nimble.service.model.ubl.catalogue.CatalogueType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CatalogueLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CompletedTaskType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
import eu.nimble.service.model.ubl.orderresponsesimple.OrderResponseSimpleType;
import eu.nimble.utility.persistence.GenericJPARepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
public interface CatalogueRepository extends JpaRepository<CatalogueType, Long>, GenericJPARepository {
    @Query(value = "SELECT party FROM PartyType party WHERE party.ID = :id ORDER BY party.hjid ASC")
    List<PartyType> getPartyByID(@Param("id") String id);

    @Query(value = "SELECT cl FROM CatalogueLineType cl WHERE cl.ID = :lineId AND cl.goodsItem.item.manufacturerParty.ID = :partyId")
    List<CatalogueLineType> getCatalogueLine(@Param("lineId") String lineId, @Param("partyId") String partyId);

    @Query(value = "SELECT order_.ID from OrderType order_ join order_.orderLine line where line.lineItem.item.manufacturerParty.ID = :partyId AND line.lineItem.item.manufacturersItemIdentification.ID = :itemId")
    List<String> getOrderIds(@Param("partyId") String partyId, @Param("itemId") String itemId);

    @Query(value = "SELECT qpt FROM QualifyingPartyType qpt WHERE qpt.party.ID = :partyId")
    QualifyingPartyType getQualifyingParty(@Param("partyId") String partyId);

    @Query(value = "SELECT orderResponse.ID FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = :documentId")
    String getOrderResponseId(@Param("documentId") String documentId);

    @Query(value = "SELECT orderResponse FROM OrderResponseSimpleType orderResponse WHERE orderResponse.orderReference.documentReference.ID = :documentId")
    OrderResponseSimpleType getOrderResponseSimple(@Param("documentId") String documentId);


    @Query(value =
            "SELECT completedTask FROM QualifyingPartyType qParty JOIN qParty.completedTask completedTask " +
                    "WHERE qParty.party.ID = :partyId AND completedTask.associatedProcessInstanceID = :processInstanceId")
    CompletedTaskType getCompletedTaskByPartyIdAndProcessInstanceId(@Param("partyId") String partyId, @Param("processInstanceId") String processInstanceId);
}