package eu.nimble.service.bp.util.persistence.bp;

import eu.nimble.service.bp.model.hyperjaxb.CollaborationGroupDAO;
import eu.nimble.service.bp.model.hyperjaxb.GroupStatus;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by suat on 26-Mar-18.
 */
public class ProcessInstanceGroupDAOUtility {

    private static final String QUERY_GET_PROCESS_INSTANCE_GROUPS =
            "SELECT pig, max(doc.submissionDate) AS lastActivityTime, min(doc.submissionDate) AS firstActivityTime FROM" +
                    " ProcessInstanceGroupDAO pig JOIN pig.processInstanceIDsItems pid," +
                    " ProcessInstanceDAO pi," +
                    " ProcessDocumentMetadataDAO doc" +
                    " WHERE" +
                    " ( pig.ID = :groupId) AND" +
                    " pid.item = pi.processInstanceID AND" +
                    " doc.processInstanceID = pi.processInstanceID" +
                    " GROUP BY pig.hjid";

    private static final String QUERY_GET_ORDER_ID_IN_GROUP =
            "SELECT DISTINCT doc.documentID FROM ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid," +
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
                    ")";

    private static final String QUERY_GET_BY_ASSOCIATED_GROUP_ID =
            "select pig from ProcessInstanceGroupDAO pig where pig.partyID = :partyId and pig.ID in " +
                    "(select agrp.item from ProcessInstanceGroupDAO pig2 join pig2.associatedGroupsItems agrp where pig2.ID = :associatedGroupId)";

    private static final String QUERY_GET_PRECEDING_PROCESS_INSTANCE_ID =
            "SELECT pig.precedingProcess.processInstanceID FROM ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid," +
                    "ProcessInstanceDAO pi " +
                    "WHERE " +
                    "pid.item = pi.processInstanceID AND " +
                    "pi.processInstanceID = :processInstanceId AND pig.precedingProcess IS NOT NULL";

    private static final String QUERY_GET_BY_PARTY_ID = "SELECT pig.ID FROM ProcessInstanceGroupDAO pig WHERE pig.partyID in :partyIds";

    private static final String QUERY_GET_BY_HJID = "SELECT c FROM ProcessInstanceGroupDAO c WHERE c.hjid = :hjid";
    public static List<ProcessInstanceGroupDAO> getProcessInstanceGroupDAO(Long hjid) {
        return new JPARepositoryFactory().forBpRepository(true).getSingleEntity(QUERY_GET_BY_HJID, new String[]{"hjid"}, new Object[]{hjid});
    }

    public static List<String> getProcessInstanceGroupIdsForParty(List<String> partyIds){
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_PARTY_ID, new String[]{"partyIds"}, new Object[]{partyIds});
    }

    public static Object getProcessInstanceGroups(String groupId) {
        return new JPARepositoryFactory().forBpRepository(true).getSingleEntity(QUERY_GET_PROCESS_INSTANCE_GROUPS, new String[]{"groupId"}, new Object[]{groupId});
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String partyId, String associatedGroupId,boolean lazyDisabled) {
        return new JPARepositoryFactory().forBpRepository(lazyDisabled).getSingleEntity(QUERY_GET_BY_ASSOCIATED_GROUP_ID, new String[]{"partyId", "associatedGroupId"}, new Object[]{partyId, associatedGroupId});
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String partyId, String associatedGroupId) {
        return getProcessInstanceGroupDAO(partyId,associatedGroupId,true);
    }

    public static String getPrecedingProcessInstanceId(String processInstanceId) {
        return new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_PRECEDING_PROCESS_INSTANCE_ID, new String[]{"processInstanceId"}, new Object[]{processInstanceId});
    }

    public static ProcessInstanceGroupDAO createProcessInstanceGroupDAO(String partyId, String processInstanceId, String collaborationRole, List<String> relatedProducts) {
        return createProcessInstanceGroupDAO(partyId, processInstanceId, collaborationRole, relatedProducts, null);
    }

    public static ProcessInstanceGroupDAO createProcessInstanceGroupDAO(String partyId, String processInstanceId, String collaborationRole, List<String> relatedProducts, String associatedGroup) {
        String uuid = UUID.randomUUID().toString();
        ProcessInstanceGroupDAO group = new ProcessInstanceGroupDAO();
        group.setArchived(false);
        group.setID(uuid);
        String groupName = "";
        for (int i = 0; i < relatedProducts.size(); i++) {
            if (i == relatedProducts.size() - 1) {
                groupName += relatedProducts.get(i);
            } else {
                groupName += relatedProducts.get(i) + ",";
            }
        }
        group.setName(groupName);
        group.setPartyID(partyId);
        group.setStatus(GroupStatus.INPROGRESS);
        group.setCollaborationRole(collaborationRole);
        List<String> processInstanceIds = new ArrayList<>();
        processInstanceIds.add(processInstanceId);
        group.setProcessInstanceIDs(processInstanceIds);
        if (associatedGroup != null) {
            List<String> associatedGroups = new ArrayList<>();
            associatedGroups.add(associatedGroup);
            group.setAssociatedGroups(associatedGroups);
        }
        new JPARepositoryFactory().forBpRepository().persistEntity(group);
        return group;
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String groupID) {
        Object[] resultItems = (Object[]) getProcessInstanceGroups(groupID);
        ProcessInstanceGroupDAO pig = (ProcessInstanceGroupDAO) resultItems[0];
        pig.setLastActivityTime((String) resultItems[1]);
        pig.setFirstActivityTime((String) resultItems[2]);
        return pig;
    }

    public static ProcessInstanceDAO getProcessInstance(String processInstanceId) {
        String queryStr = "SELECT pi FROM ProcessInstanceDAO pi WHERE pi.processInstanceID = ?";
        ProcessInstanceDAO pi = HibernateUtility.getInstance("bp-data-model").load(queryStr, processInstanceId);
        return pi;
    }

    public static void deleteProcessInstanceGroupDAOByID(String groupID) {
        CollaborationGroupDAO collaborationGroupDAO = CollaborationGroupDAOUtility.getCollaborationGroupOfProcessInstanceGroup(groupID);
        // if the collaboration group only contains the given group,then delete the collaboration group so that there will not be any garbage on the database
        if (collaborationGroupDAO.getAssociatedProcessInstanceGroups().size() == 1) {
            CollaborationGroupDAOUtility.deleteCollaborationGroupDAOsByID(Collections.singletonList(collaborationGroupDAO.getHjid()));
        } else {
            ProcessInstanceGroupDAO group = null;
            for (ProcessInstanceGroupDAO groupDAO : collaborationGroupDAO.getAssociatedProcessInstanceGroups()) {
                if (groupDAO.getID().equals(groupID)) {
                    group = groupDAO;
                    break;
                }
            }
            // delete references to this group
            GenericJPARepository repo = new JPARepositoryFactory().forBpRepository();
            for (String id : group.getAssociatedGroups()) {
                ProcessInstanceGroupDAO groupDAO = getProcessInstanceGroupDAO(id);
                groupDAO.getAssociatedGroups().remove(groupID);
                repo.updateEntity(groupDAO);
            }
            repo.deleteEntityByHjid(ProcessInstanceGroupDAO.class, group.getHjid());
        }
    }

    public static String getOrderIdInGroup(String processInstanceId) {
        // get the id of preceding process instance if there is any
        String precedingProcessInstanceID = ProcessInstanceGroupDAOUtility.getPrecedingProcessInstanceId(processInstanceId);
        String orderId;
        // if there is a preceding process instance, using that, get the order id
        if (precedingProcessInstanceID != null) {
            orderId = ProcessDocumentMetadataDAOUtility.getRequestMetadata(precedingProcessInstanceID).getDocumentID();
        } else {
            orderId = new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_ORDER_ID_IN_GROUP, new String[]{"processInstanceId"}, new Object[]{processInstanceId});
        }
        return orderId;
    }

    private enum GroupQueryType {
        GROUP, FILTER, SIZE
    }

    private static class QueryData {
        private String query;
        private List<String> parameterNames = new ArrayList<>();
        private List<Object> parameterValues = new ArrayList<>();
    }
}
