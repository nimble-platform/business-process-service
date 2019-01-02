package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.CollaborationGroupDAO;
import eu.nimble.utility.persistence.JPARepositoryFactory;

/**
 * Created by suat on 01-Jan-19.
 */
public class CollaborationGroupDAOUtility {
    private static final String QUERY_GET_ASSOCIATED_GROUP =
            "select cg from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups pig where pig.partyID = :partyId and cg.hjid in " +
            "(select acg.item from CollaborationGroupDAO cg2 join cg2.associatedCollaborationGroupsItems acg where cg2.hjid = :associatedGroupId)";
    private static final String QUERY_GET_GROUP_OF_PROCESS_INSTANCE_GROUP =
            "select cg from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups apig where apig.ID = :groupId";

    public static CollaborationGroupDAO getAssociatedCollaborationGroup(String partyId, Long associatedGroupId) {
        return new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_ASSOCIATED_GROUP, new String[]{"partyId", "associatedGroupId"}, new Object[]{partyId, associatedGroupId});
    }

    public static CollaborationGroupDAO getCollaborationGroupOfProcessInstanceGroup(String groupId) {
        return new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_GROUP_OF_PROCESS_INSTANCE_GROUP, new String[]{"groupId"}, new Object[]{groupId});
    }
}
