package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.impl.util.persistence.catalogue.DocumentDAOUtility;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by suat on 26-Mar-18.
 */
@Component
public class ProcessInstanceGroupDAOUtility {
    private final Logger logger = LoggerFactory.getLogger(ProcessInstanceGroupDAOUtility.class);

    @Autowired
    private IdentityClientTyped identityClient;

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

    private static final String QUERY_GET_BY_HJID = "SELECT c FROM ProcessInstanceGroupDAO c WHERE c.hjid = :hjid";
    public static List<ProcessInstanceGroupDAO> getProcessInstanceGroupDAO(Long hjid) {
        return new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_BY_HJID, new String[]{"hjid"}, new Object[]{hjid});
    }

    public static Object getProcessInstanceGroups(String groupId) {
        return new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_PROCESS_INSTANCE_GROUPS, new String[]{"groupId"}, new Object[]{groupId});
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroup(String partyId, String associatedGroupId) {
        return new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_BY_ASSOCIATED_GROUP_ID, new String[]{"partyId", "associatedGroupId"}, new Object[]{partyId, associatedGroupId});
    }

    public static String getPrecedingProcessInstanceId(String processInstanceId) {
        return new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_PRECEDING_PROCESS_INSTANCE_ID, new String[]{"processInstanceId"}, new Object[]{processInstanceId});
    }

    public static List<CollaborationGroupDAO> getCollaborationGroupDAOs(
            String partyId,
            String collaborationRole,
            Boolean archived,
            List<String> tradingPartnerIds,
            List<String> relatedProductIds,
            List<String> relatedProductCategories,
            List<String> status,
            String startTime,
            String endTime,
            int limit,
            int offset) {

        QueryData query = getGroupRetrievalQuery(GroupQueryType.GROUP, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, startTime, endTime);
        List<Object> collaborationGroups = new JPARepositoryFactory().forBpRepository().getEntities(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray(),limit,offset);
        List<CollaborationGroupDAO> results = new ArrayList<>();
        for (Object groupResult : collaborationGroups) {
            Object[] resultItems = (Object[]) groupResult;
            CollaborationGroupDAO collaborationGroupDAO = (CollaborationGroupDAO) resultItems[0];

            CollaborationGroupDAO collaborationGroupInResults = null;

            // check whether the collaborationGroup is the results or not
            for (CollaborationGroupDAO collaborationGroup : results) {
                if (collaborationGroup.getHjid().equals(collaborationGroupDAO.getHjid())) {
                    collaborationGroupInResults = collaborationGroup;
                }
            }
            if (collaborationGroupInResults == null) {
                collaborationGroupInResults = collaborationGroupDAO;
                results.add(collaborationGroupInResults);
            }

            ProcessInstanceGroupDAO group = (ProcessInstanceGroupDAO) resultItems[1];
            // find the group in the collaborationGroup
            for (ProcessInstanceGroupDAO groupDAO : collaborationGroupInResults.getAssociatedProcessInstanceGroups()) {
                if (groupDAO.getID().equals(group.getID())) {
                    groupDAO.setLastActivityTime((String) resultItems[2]);
                    groupDAO.setFirstActivityTime((String) resultItems[3]);
                }
            }
        }
        return results;
    }

    public static int getCollaborationGroupSize(String partyId,
                                                String collaborationRole,
                                                boolean archived,
                                                List<String> tradingPartnerIds,
                                                List<String> relatedProductIds,
                                                List<String> relatedProductCategories,
                                                List<String> status,
                                                String startTime,
                                                String endTime) {
        QueryData query = getGroupRetrievalQuery(GroupQueryType.SIZE, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, startTime, endTime);
        int count = ((Long) new JPARepositoryFactory().forBpRepository().getSingleEntity(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray())).intValue();
        return count;
    }

    public ProcessInstanceGroupFilter getFilterDetails(
            String partyId,
            String collaborationRole,
            Boolean archived,
            List<String> tradingPartnerIds,
            List<String> relatedProductIds,
            List<String> relatedProductCategories,
            List<String> status,
            String startTime,
            String endTime,
            String bearerToken) {

        QueryData query = getGroupRetrievalQuery(GroupQueryType.FILTER, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, startTime, endTime);

        ProcessInstanceGroupFilter filter = new ProcessInstanceGroupFilter();
        List<Object> resultSet = new JPARepositoryFactory().forBpRepository().getEntities(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray());
        for (Object result : resultSet) {
            List<Object> returnedColumns = (List<Object>) result;

            //product
            String resultColumn = (String) returnedColumns.get(0);
            if (!filter.getRelatedProducts().contains(resultColumn)) {
                filter.getRelatedProducts().add(resultColumn);
            }

            // product category
            resultColumn = (String) returnedColumns.get(1);
            if (resultColumn != null && !filter.getRelatedProductCategories().contains(resultColumn)) {
                filter.getRelatedProductCategories().add(resultColumn);
            }

            // partner ids
            // Don't know if the current party is initiator or responder. So, should find the trading partner's id
            resultColumn = (String) returnedColumns.get(2);
            if (resultColumn.contentEquals(partyId)) {
                resultColumn = (String) returnedColumns.get(3);
            }
            if (!filter.getTradingPartnerIDs().contains(resultColumn)) {
                filter.getTradingPartnerIDs().add(resultColumn);
            }

            List<PartyType> parties = null;
            try {
                parties = identityClient.getParties(bearerToken, filter.getTradingPartnerIDs());
            } catch (IOException e) {
                String msg = String.format("Failed to get parties while getting categories for party: %s, collaboration role: %s, archived: %B", partyId, collaborationRole, archived);
                logger.error(msg);
                throw new RuntimeException(msg, e);
            }

            // populate partners' names
            if (parties != null) {
                for (String tradingPartnerId : filter.getTradingPartnerIDs()) {
                    for (PartyType party : parties) {
                        if (party.getID().equals(tradingPartnerId)) {
                            if (!filter.getTradingPartnerNames().contains(party.getName())) {
                                filter.getTradingPartnerNames().add(party.getName());
                            }
                            break;
                        }
                    }
                }
            }

            // status
            ProcessInstanceStatus processInstanceStatus = (ProcessInstanceStatus) returnedColumns.get(4);
            if (!filter.getStatus().contains(ProcessInstanceGroupFilter.StatusEnum.valueOf(processInstanceStatus.value()))) {
                filter.getStatus().add(ProcessInstanceGroupFilter.StatusEnum.valueOf(processInstanceStatus.value()));
            }
        }
        return filter;
    }

    private static QueryData getGroupRetrievalQuery(
            GroupQueryType queryType,
            String partyId,
            String collaborationRole,
            Boolean archived,
            List<String> tradingPartnerIds,
            List<String> relatedProductIds,
            List<String> relatedProductCategories,
            List<String> status,
            String startTime,
            String endTime) {

        QueryData queryData = new QueryData();
        List<String> parameterNames = queryData.parameterNames;
        List<Object> parameterValues = queryData.parameterValues;

        String query = "";
        if (queryType == GroupQueryType.FILTER) {
            query += "select distinct new list(relProd.item, relCat.item, doc.initiatorID, doc.responderID, pi.status)";
        } else if (queryType == GroupQueryType.SIZE) {
            query += "select count(distinct cg)";
        } else if (queryType == GroupQueryType.GROUP) {
            query += "select cg,pig, max(doc.submissionDate) as lastActivityTime, min(doc.submissionDate) as firstActivityTime";
        }

        query += " from " +
                "CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups pig join pig.processInstanceIDsItems pid, " +
                "ProcessInstanceDAO pi, " +
                "ProcessDocumentMetadataDAO doc left join doc.relatedProductCategoriesItems relCat left join doc.relatedProductsItems relProd" +
                " where " +
                "pid.item = pi.processInstanceID and doc.processInstanceID = pi.processInstanceID";

        if (relatedProductCategories != null && relatedProductCategories.size() > 0) {
            query += " and (";
            int i = 0;
            for (; i < relatedProductCategories.size() - 1; i++) {
                query += " relCat.item = :category" + i + " or";

                parameterNames.add("category" + i);
                parameterValues.add(relatedProductCategories.get(i));
            }
            query += " relCat.item = :category" + i + ")";

            parameterNames.add("category" + i);
            parameterValues.add(relatedProductCategories.get(i));
        }
        if (relatedProductIds != null && relatedProductIds.size() > 0) {
            query += " and (";
            int i = 0;
            for (; i < relatedProductIds.size() - 1; i++) {
                query += " relProd.item = :product" + i + " or";

                parameterNames.add("product" + i);
                parameterValues.add(relatedProductIds.get(i));
            }
            query += " relProd.item = :product" + i + ")";

            parameterNames.add("product" + i);
            parameterValues.add(relatedProductIds.get(i));
        }
        if (tradingPartnerIds != null && tradingPartnerIds.size() > 0) {
            query += " and (";
            int i = 0;
            for (; i < tradingPartnerIds.size() - 1; i++) {
                query += " (doc.initiatorID = :partner" + i + " or doc.responderID = :partner" + i + ") or";

                parameterNames.add("partner" + i);
                parameterValues.add(tradingPartnerIds.get(i));
            }
            query += " (doc.initiatorID = :partner" + i + " or doc.responderID = :partner" + i + "))";
            parameterNames.add("partner" + i);
            parameterValues.add(tradingPartnerIds.get(i));
        }
        if (status != null && status.size() > 0) {
            query += " and (";
            int i = 0;
            for (; i < status.size() - 1; i++) {
                query += " (pi.status = '" + ProcessInstanceStatus.valueOf(status.get(i)).toString() + "') or";
            }
            query += " (pi.status = '" + ProcessInstanceStatus.valueOf(status.get(i)).toString() + "'))";
        }
        if (archived != null) {
            query += " and pig.archived = :archived";

            parameterNames.add("archived");
            parameterValues.add(archived);
        }
        if (partyId != null) {
            query += " and pig.partyID = :partyId";

            parameterNames.add("partyId");
            parameterValues.add(partyId);
        }
        if (collaborationRole != null) {
            query += " and pig.collaborationRole = :role";

            parameterNames.add("role");
            parameterValues.add(collaborationRole);
        }

        if (queryType == GroupQueryType.GROUP) {
            query += " group by pig.hjid,cg.hjid";
            query += " order by firstActivityTime desc";
        }

        query += ") > 0";
        queryData.query = query;
        return queryData;
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

    public static CollaborationGroupDAO createCollaborationGroupDAO() {
        CollaborationGroupDAO collaborationGroupDAO = new CollaborationGroupDAO();
        collaborationGroupDAO.setStatus(CollaborationStatus.INPROGRESS);
        collaborationGroupDAO.setArchived(false);
        new JPARepositoryFactory().forBpRepository().persistEntity(collaborationGroupDAO);
        return collaborationGroupDAO;
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String groupID) {
        Object[] resultItems = (Object[]) getProcessInstanceGroups(groupID);
        ProcessInstanceGroupDAO pig = (ProcessInstanceGroupDAO) resultItems[0];
        pig.setLastActivityTime((String) resultItems[1]);
        pig.setFirstActivityTime((String) resultItems[2]);
        return pig;
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String partyId, String associatedGroupId) {
        ProcessInstanceGroupDAO group = ProcessInstanceGroupDAOUtility.getProcessInstanceGroup(partyId, associatedGroupId);
        return group;
    }

    public static CollaborationGroupDAO getCollaborationGroupDAO(String partyId, Long associatedGroupId) {
        CollaborationGroupDAO group = CollaborationGroupDAOUtility.getAssociatedCollaborationGroup(partyId, associatedGroupId);
        return group;
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
            deleteCollaborationGroupDAOByID(collaborationGroupDAO.getHjid());
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

    public static void deleteCollaborationGroupDAOByID(Long groupID) {
        GenericJPARepository repo = new JPARepositoryFactory().forBpRepository();
        CollaborationGroupDAO group = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, groupID);
        // delete references to this group
        for (Long id : group.getAssociatedCollaborationGroups()) {
            CollaborationGroupDAO groupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, id);
            groupDAO.getAssociatedCollaborationGroups().remove(groupID);
            repo.updateEntity(groupDAO);
        }
        repo.deleteEntityByHjid(CollaborationGroupDAO.class, groupID);
    }

    public static CollaborationGroupDAO archiveCollaborationGroup(String id) {
        GenericJPARepository repo = new JPARepositoryFactory().forBpRepository();
        CollaborationGroupDAO collaborationGroupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(id));
        // archive the collaboration group
        collaborationGroupDAO.setArchived(true);
        // archive the groups inside the given collaboration group
        for (ProcessInstanceGroupDAO processInstanceGroupDAO : collaborationGroupDAO.getAssociatedProcessInstanceGroups()) {
            processInstanceGroupDAO.setArchived(true);
        }
        collaborationGroupDAO = repo.updateEntity(collaborationGroupDAO);
        return collaborationGroupDAO;
    }

    public static CollaborationGroupDAO restoreCollaborationGroup(String id) {
        GenericJPARepository repo = new JPARepositoryFactory().forBpRepository();
        CollaborationGroupDAO collaborationGroupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, Long.parseLong(id));
        // archive the collaboration group
        collaborationGroupDAO.setArchived(false);
        // archive the groups inside the given collaboration group
        for (ProcessInstanceGroupDAO processInstanceGroupDAO : collaborationGroupDAO.getAssociatedProcessInstanceGroups()) {
            processInstanceGroupDAO.setArchived(false);
        }
        collaborationGroupDAO = repo.updateEntity(collaborationGroupDAO);
        return collaborationGroupDAO;
    }

    public static String getOrderIdInGroup(String processInstanceId) {
        // get the id of preceding process instance if there is any
        String precedingProcessInstanceID = ProcessInstanceGroupDAOUtility.getPrecedingProcessInstanceId(processInstanceId);
        String orderId;
        // if there is a preceding process instance, using that, get the order id
        if (precedingProcessInstanceID != null) {
            orderId = DocumentDAOUtility.getRequestMetadata(precedingProcessInstanceID).getDocumentID();
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
