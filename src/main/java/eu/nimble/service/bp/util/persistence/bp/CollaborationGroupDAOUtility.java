package eu.nimble.service.bp.util.persistence.bp;

import eu.nimble.service.bp.model.hyperjaxb.CollaborationGroupDAO;
import eu.nimble.service.bp.model.hyperjaxb.CollaborationStatus;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceStatus;
import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyNameType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by suat on 01-Jan-19.
 */
public class CollaborationGroupDAOUtility {
    private static final String QUERY_GET_ASSOCIATED_GROUP =
            "select cg from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups pig where pig.partyID = :partyId and cg.hjid in " +
            "(select acg.item from CollaborationGroupDAO cg2 join cg2.associatedCollaborationGroupsItems acg where cg2.hjid = :associatedGroupId)";
    private static final String QUERY_GET_GROUP_OF_PROCESS_INSTANCE_GROUP =
            "select cg from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups apig where apig.ID = :groupId";
    private static final String QUERY_GET_GROUP_OF_PROCESS_INSTANCE_GROUPS =
            "select cg.hjid from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups apig where apig.ID in :groupIds";
    private static final String QUERY_GET_PROCESS_INSTANCES_OF_COLLABORATION_GROUP =
            "SELECT DISTINCT pi.status FROM " +
                    "ProcessInstanceDAO pi, " +
                    "CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups pig join pig.processInstanceIDsItems pids" +
                    " WHERE pids.item = pi.processInstanceID AND cg.hjid = :collaborationGroupId";
    private static final String QUERY_GET_HJID_BY_PROCESS_INSTANCE_ID_AND_PARTY_ID =
            "SELECT cg.hjid FROM CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups apig join apig.processInstanceIDsItems pids " +
                    "WHERE apig.partyID = :partyID AND pids.item = :processInstanceId";
    private static final String QUERY_GET_BY_PROCESS_INSTANCE_ID_AND_PARTY_ID =
            "SELECT cg FROM CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups apig join apig.processInstanceIDsItems pids " +
                    "WHERE apig.partyID = :partyID AND pids.item = :processInstanceId";
    private static final String QUERY_GET_PROCESS_INSTANCES_OF_COLLABORATION_GROUPS =
            "SELECT cg.hjid, pi.status FROM " +
                    "ProcessInstanceDAO pi, " +
                    "CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups pig join pig.processInstanceIDsItems pids" +
                    " WHERE pids.item = pi.processInstanceID AND cg.hjid IN (%s)"; // to be completed in the query

    private static final Logger logger = LoggerFactory.getLogger(CollaborationGroupDAOUtility.class);

    public static CollaborationGroupDAO getCollaborationGroupDAO(Long hjid){
        return new JPARepositoryFactory().forBpRepository(true).getSingleEntityByHjid(CollaborationGroupDAO.class,hjid);
    }

    public static CollaborationGroupDAO getCollaborationGroupByProcessInstanceIdAndPartyId(String processInstanceId, String partyId){
        return new JPARepositoryFactory().forBpRepository(true).getSingleEntity(QUERY_GET_BY_PROCESS_INSTANCE_ID_AND_PARTY_ID, new String[]{"partyID", "processInstanceId"}, new Object[]{partyId, processInstanceId});
    }

    public static Long getCollaborationGroupHjidByProcessInstanceIdAndPartyId(String processInstanceId, String partyId){
        return new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_HJID_BY_PROCESS_INSTANCE_ID_AND_PARTY_ID, new String[]{"partyID", "processInstanceId"}, new Object[]{partyId, processInstanceId});
    }

    public static CollaborationGroupDAO getCollaborationGroupDAO(String partyId, Long associatedGroupId) {
        CollaborationGroupDAO group = CollaborationGroupDAOUtility.getAssociatedCollaborationGroup(partyId, associatedGroupId);
        return group;
    }
    
    public static CollaborationGroupDAO getAssociatedCollaborationGroup(String partyId, Long associatedGroupId) {
        return new JPARepositoryFactory().forBpRepository(true).getSingleEntity(QUERY_GET_ASSOCIATED_GROUP, new String[]{"partyId", "associatedGroupId"}, new Object[]{partyId, associatedGroupId});
    }

    public static List<Long> getCollaborationGroupHjidOfProcessInstanceGroups(List<String> groupIds) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_GROUP_OF_PROCESS_INSTANCE_GROUPS, new String[]{"groupIds"}, new Object[]{groupIds});
    }

    public static CollaborationGroupDAO getCollaborationGroupOfProcessInstanceGroup(String groupId) {
        return new JPARepositoryFactory().forBpRepository(true).getSingleEntity(QUERY_GET_GROUP_OF_PROCESS_INSTANCE_GROUP, new String[]{"groupId"}, new Object[]{groupId});
    }

    public static CollaborationGroupDAO createCollaborationGroupDAO() {
        CollaborationGroupDAO collaborationGroupDAO = new CollaborationGroupDAO();
        collaborationGroupDAO.setStatus(CollaborationStatus.INPROGRESS);
        collaborationGroupDAO.setArchived(false);
        new JPARepositoryFactory().forBpRepository().persistEntity(collaborationGroupDAO);
        return collaborationGroupDAO;
    }

    public static void deleteCollaborationGroupDAOsByID(List<Long> hjids) {
        for (Long groupID: hjids){
            GenericJPARepository repo = new JPARepositoryFactory().forBpRepository(true);
            CollaborationGroupDAO collaborationGroup = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, groupID);
            if(collaborationGroup != null){
                // delete references to the collaboration group
                for (Long id : collaborationGroup.getAssociatedCollaborationGroups()) {
                    CollaborationGroupDAO groupDAO = repo.getSingleEntityByHjid(CollaborationGroupDAO.class, id);
                    groupDAO.getAssociatedCollaborationGroups().remove(groupID);
                    repo.updateEntity(groupDAO);
                }
                // delete references to the process instance groups inside this collaboration group
                for(ProcessInstanceGroupDAO processInstanceGroup:collaborationGroup.getAssociatedProcessInstanceGroups()){
                    for(String associatedProcessInstanceGroupId:processInstanceGroup.getAssociatedGroups()){
                        ProcessInstanceGroupDAO groupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(associatedProcessInstanceGroupId);
                        groupDAO.getAssociatedGroups().remove(processInstanceGroup.getID());
                        repo.updateEntity(groupDAO);
                    }
                }
                repo.deleteEntityByHjid(CollaborationGroupDAO.class, groupID);
            }
        }
    }

    public static List<ProcessInstanceStatus> getCollaborationProcessInstanceStatusesForCollaborationGroup(Long collaborationGroupHjid) {
        GenericJPARepository repo = new JPARepositoryFactory().forBpRepository(true);
        List<ProcessInstanceStatus> statuses = repo.getEntities(QUERY_GET_PROCESS_INSTANCES_OF_COLLABORATION_GROUP, new String[]{"collaborationGroupId"}, new Object[]{collaborationGroupHjid});
        return statuses;
    }

    public static Map<Long, Set<ProcessInstanceStatus>> getCollaborationProcessInstanceStatusesForCollaborationGroups(List<Long> collaborationGroupHjids) {
        if(collaborationGroupHjids.size() == 0) {
            return new HashMap<>();
        }

        StringBuilder idConstraintBuilder = new StringBuilder("");
        collaborationGroupHjids.stream().forEach(id -> idConstraintBuilder.append(id).append(","));
        String idConstraint = idConstraintBuilder.substring(0, idConstraintBuilder.length()-1);
        String query = String.format(QUERY_GET_PROCESS_INSTANCES_OF_COLLABORATION_GROUPS, idConstraint);

        GenericJPARepository repo = new JPARepositoryFactory().forBpRepository(true);
        List<Object[]> statuses = repo.getEntities(query);
        Map<Long, Set<ProcessInstanceStatus>> result = new HashMap<>();
        for(Object[] status : statuses) {
            Set<ProcessInstanceStatus> statusList = result.get(status[0]);
            if(statusList == null) {
                statusList = new HashSet<>();
                result.put((Long) status[0], statusList);
            }
            statusList.add((ProcessInstanceStatus) status[1]);
        }
        return result;
    }

    public static boolean isCollaborationGroupArchivable(Long collaborationGroupHjid) {
        // get the status of process instances
        List<ProcessInstanceStatus> statuses = getCollaborationProcessInstanceStatusesForCollaborationGroup(collaborationGroupHjid);
        return doesProcessInstanceStatusesMeetArchivingConditions(statuses);
    }

    public static boolean doesProcessInstanceStatusesMeetArchivingConditions(List<ProcessInstanceStatus> processInstanceStatuses) {
        // if the status is not COMPLETED or CANCELLED, it's accepted as active and the process is regarded as non-archivable
        return !processInstanceStatuses.stream().filter(status ->
                !(status.equals(ProcessInstanceStatus.COMPLETED) || status.equals(ProcessInstanceStatus.CANCELLED)))
                .findFirst()
                .isPresent();
    }

    public static CollaborationGroupDAO archiveCollaborationGroup(CollaborationGroupDAO collaborationGroupDAO) {
        GenericJPARepository repo = new JPARepositoryFactory().forBpRepository(true);
        // archive the collaboration group
        collaborationGroupDAO.setArchived(true);
        // archive the groups inside the given collaboration group
        for (ProcessInstanceGroupDAO processInstanceGroupDAO : collaborationGroupDAO.getAssociatedProcessInstanceGroups()) {
            processInstanceGroupDAO.setArchived(true);
        }
        collaborationGroupDAO = repo.updateEntity(collaborationGroupDAO);
        return collaborationGroupDAO;
    }

    public static CollaborationGroupDAO restoreCollaborationGroup(CollaborationGroupDAO collaborationGroupDAO) {
        GenericJPARepository repo = new JPARepositoryFactory().forBpRepository(true);
        // archive the collaboration group
        collaborationGroupDAO.setArchived(false);
        // archive the groups inside the given collaboration group
        for (ProcessInstanceGroupDAO processInstanceGroupDAO : collaborationGroupDAO.getAssociatedProcessInstanceGroups()) {
            processInstanceGroupDAO.setArchived(false);
        }
        collaborationGroupDAO = repo.updateEntity(collaborationGroupDAO);
        return collaborationGroupDAO;
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
            int offset,
            Boolean isProject) {

        QueryData query = null;

        if(isProject){
            query = getGroupRetrievalQuery(GroupQueryType.PROJECT, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, startTime, endTime);
        }else{
            query = getGroupRetrievalQuery(GroupQueryType.GROUP, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, startTime, endTime);
        }
        List<Object> collaborationGroups = new JPARepositoryFactory().forBpRepository(true).getEntities(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray(),limit,offset);
        List<CollaborationGroupDAO> results = new ArrayList<>();
        for (Object groupResult : collaborationGroups) {
            Object[] resultItems = (Object[]) groupResult;
            CollaborationGroupDAO collaborationGroupDAO = (CollaborationGroupDAO) resultItems[0];

            // since in the result set, there may be multiple process instance groups belonging to the same collaboration group
            // we need to be sure that there will be only one collaboration group in the final result
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

            for (ProcessInstanceGroupDAO oneWay:collaborationGroupDAO.getAssociatedProcessInstanceGroups()) {
                ProcessInstanceGroupDAO processInstanceGroupDAO = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAO(oneWay.getID());
                // find the group in the collaborationGroup
                for (ProcessInstanceGroupDAO groupDAO : collaborationGroupInResults.getAssociatedProcessInstanceGroups()) {
                    if (groupDAO.getID().equals(processInstanceGroupDAO.getID())) {
                        groupDAO.setLastActivityTime((String) processInstanceGroupDAO.getLastActivityTime());
                        groupDAO.setFirstActivityTime((String) processInstanceGroupDAO.getFirstActivityTime());
                    }
                }
            }

//            ProcessInstanceGroupDAO group = (ProcessInstanceGroupDAO) resultItems[1];

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
                                                String endTime,
                                                Boolean isProject) {

        QueryData query = null;
        if(isProject){
            query = getGroupRetrievalQuery(GroupQueryType.PROJECTSIZE, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, startTime, endTime);
        }else {
             query = getGroupRetrievalQuery(GroupQueryType.SIZE, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, startTime, endTime);
        }
        int count = ((Long) new JPARepositoryFactory().forBpRepository(true).getSingleEntity(query.query, query.parameterNames.toArray(new String[query.parameterNames.size()]), query.parameterValues.toArray())).intValue();

        return count;
    }

    public static ProcessInstanceGroupFilter getFilterDetails(
            String partyId,
            String collaborationRole,
            Boolean archived,
            List<String> tradingPartnerIds,
            List<String> relatedProductIds,
            List<String> relatedProductCategories,
            List<String> status,
            String startTime,
            String endTime,
            String bearerToken,
            Boolean isProject) {

        QueryData query = null;
        if(isProject){
            query = getGroupRetrievalQuery(GroupQueryType.PROJECTFILTER, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, startTime, endTime);

        }else{
            query = getGroupRetrievalQuery(GroupQueryType.FILTER, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, startTime, endTime);

        }
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
                parties = SpringBridge.getInstance().getiIdentityClientTyped().getParties(bearerToken, filter.getTradingPartnerIDs());
            } catch (IOException e) {
                String msg = String.format("Failed to get parties while getting categories for party: %s, collaboration role: %s, archived: %B", partyId, collaborationRole, archived);
                logger.error(msg);
                throw new RuntimeException(msg, e);
            }

            // populate partners' names
            if (parties != null) {
                for (String tradingPartnerId : filter.getTradingPartnerIDs()) {
                    for (PartyType party : parties) {
                        if (party.getPartyIdentification().get(0).getID().equals(tradingPartnerId)) {
                            // check whether trading partner names array of filter contains any names of the party
                            boolean partyExists = false;
                            for(PartyNameType partyName : party.getPartyName()){
                                if(filter.getTradingPartnerNames().contains(partyName.getName().getValue())){
                                    partyExists = true;
                                    break;
                                }
                            }

                            if(!partyExists){
                                filter.getTradingPartnerNames().add(party.getPartyName().get(0).getName().getValue());
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
        if (queryType == GroupQueryType.FILTER || queryType == GroupQueryType.PROJECTFILTER) {
            query += "select distinct new list(relProd.item, relCat.item, doc.initiatorID, doc.responderID, pi.status)";
        } else if (queryType == GroupQueryType.SIZE || queryType == GroupQueryType.PROJECTSIZE) {
            query += "select count(distinct cg)";
        } else if (queryType == GroupQueryType.GROUP || queryType == GroupQueryType.PROJECT) {
            query += "select cg, max(doc.submissionDate) as lastActivityTime, min(doc.submissionDate) as firstActivityTime";
        }

        query += " from " +
                "CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups pig join pig.processInstanceIDsItems pid, " +
                "ProcessInstanceDAO pi, " +
                "ProcessDocumentMetadataDAO doc left join doc.relatedProductCategoriesItems relCat left join doc.relatedProductsItems relProd" +
                " where " +
                "pid.item = pi.processInstanceID and doc.processInstanceID = pi.processInstanceID";

        if (queryType == GroupQueryType.PROJECTSIZE || queryType == GroupQueryType.PROJECT || queryType == GroupQueryType.PROJECTFILTER) {
            query += " and cg.isProject = :isProject";

            parameterNames.add("isProject");
            parameterValues.add(Boolean.TRUE);
        }

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

        if (queryType == GroupQueryType.GROUP || queryType == GroupQueryType.PROJECT) {
            query += " group by cg.hjid";
            query += " order by firstActivityTime desc";
        }

        query += ") > 0";

        queryData.query = query;
        return queryData;
    }
    
    private enum GroupQueryType {
        GROUP, FILTER, SIZE,PROJECTSIZE,PROJECT,PROJECTFILTER
    }

    private static class QueryData {
        private String query;
        private List<String> parameterNames = new ArrayList<>();
        private List<Object> parameterValues = new ArrayList<>();
    }
}
