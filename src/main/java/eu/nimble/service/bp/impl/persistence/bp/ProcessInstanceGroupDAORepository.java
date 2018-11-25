package eu.nimble.service.bp.impl.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by suat on 20-Nov-18.
 */
@Transactional(transactionManager = "bpdbTransactionManager")
public interface ProcessInstanceGroupDAORepository extends JpaRepository<ProcessInstanceGroupDAO, Long> {

    @Query(value = "SELECT gr FROM ProcessInstanceGroupDAO gr WHERE gr.ID = :id")
    ProcessInstanceGroupDAO getGroupById (@Param("id") String id);

    @Query(value =
            "SELECT pig, max(doc.submissionDate) AS lastActivityTime, min(doc.submissionDate) AS firstActivityTime FROM" +
            " ProcessInstanceGroupDAO pig JOIN pig.processInstanceIDsItems pid," +
            " ProcessInstanceDAO pi," +
            " ProcessDocumentMetadataDAO doc" +
            " WHERE" +
            " ( pig.ID = :groupId) AND" +
            " pid.item = pi.processInstanceID AND" +
            " doc.processInstanceID = pi.processInstanceID" +
            " GROUP BY pig.hjid")
    Object getProcessInstanceGroups(@Param("groupId") String groupId);

    @Query(value = "SELECT DISTINCT doc.documentID FROM ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid," +
            " ProcessInstanceDAO pi, " +
            " ProcessDocumentMetadataDAO doc" +
            " WHERE " +
            " pid.item = pi.processInstanceID AND" +
            " doc.processInstanceID = pi.processInstanceID AND" +
            " doc.type = 'ORDER' AND pig.ID IN" +

            " (" +
            " SELECT pig2.ID FROM ProcessInstanceGroupDAO pig2 join pig2.processInstanceIDsItems pid2," +
            " ProcessInstanceDAO pi2 " +
            " WHERE" +
            " pid2.item = pi2.processInstanceID AND " +
            " pi2.processInstanceID = :processInstanceId" +
            ")")
    String getOrderIdInGroup(@Param("processInstanceId") String processInstanceId);

    @Query(value = "select pig from ProcessInstanceGroupDAO pig where pig.partyID = :partyId and pig.ID in " +
            "(select agrp.item from ProcessInstanceGroupDAO pig2 join pig2.associatedGroupsItems agrp where pig2.ID = :associatedGroupId)")
    ProcessInstanceGroupDAO getProcessInstanceGroup(@Param("partyId") String partyId, @Param("associatedGroupId") String associatedGroupId);

    @Modifying
    @Query(value = "UPDATE ProcessInstanceGroupDAO AS pig SET pig.archived = true WHERE pig.partyID = :partyId")
    void archiveAllGroupsOfParty(@Param("partyId") String partyId);

    @Query(value = "SELECT pig.hjid FROM ProcessInstanceGroupDAO pig WHERE pig.archived = true AND pig.partyID = :partyId")
    List<Long> getHjidsOfArchivedGroupsForParty(@Param("partyId") String partyId);
}
