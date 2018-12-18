package eu.nimble.service.bp.impl.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.CollaborationGroupDAO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollaborationGroupDAORepository extends JpaRepository<CollaborationGroupDAO, Long> {

    @Query(value = "select cg from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups pig where pig.partyID = :partyId and cg.hjid in " +
            "(select acg.item from CollaborationGroupDAO cg2 join cg2.associatedCollaborationGroupsItems acg where cg2.hjid = :associatedGroupId)")
    CollaborationGroupDAO getAssociatedCollaborationGroup(@Param("partyId") String partyId, @Param("associatedGroupId") Long associatedGroupId);

    @Query(value = "select cg from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups apig where apig.ID = :groupID")
    CollaborationGroupDAO getCollaborationGroupOfProcessInstanceGroup(@Param("groupID") String groupID);
}
