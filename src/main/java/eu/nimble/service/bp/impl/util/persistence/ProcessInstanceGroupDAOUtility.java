package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.service.bp.hyperjaxb.model.*;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.HibernateUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.rmi.runtime.Log;

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

        String query = getGroupRetrievalQuery(GroupQueryType.GROUP, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories,status, startTime, endTime);
        List<Object> collaborationGroups = (List<Object>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query, offset, limit);
        List<CollaborationGroupDAO> results = new ArrayList<>();
        for(Object groupResult : collaborationGroups) {
            Object[] resultItems = (Object[]) groupResult;
            CollaborationGroupDAO collaborationGroupDAO = (CollaborationGroupDAO) resultItems[0];

            CollaborationGroupDAO collaborationGroupInResults = null;

            // check whether the collaborationGroup is the results or not
            for(CollaborationGroupDAO collaborationGroup : results){
                if(collaborationGroup.getHjid().equals(collaborationGroupDAO.getHjid())){
                    collaborationGroupInResults = collaborationGroup;
                }
            }
            if(collaborationGroupInResults == null){
                collaborationGroupInResults = collaborationGroupDAO;
                results.add(collaborationGroupInResults);
            }

            ProcessInstanceGroupDAO group = (ProcessInstanceGroupDAO) resultItems[1];
            // find the group in the collaborationGroup
            for(ProcessInstanceGroupDAO groupDAO:collaborationGroupInResults.getAssociatedProcessInstanceGroups()){
                if(groupDAO.getID().equals(group.getID())){
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
        String query = getGroupRetrievalQuery(GroupQueryType.SIZE, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories,status, startTime, endTime);
        int count = ((Long) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query)).intValue();
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

        String query = getGroupRetrievalQuery(GroupQueryType.FILTER, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status,startTime, endTime);

        ProcessInstanceGroupFilter filter = new ProcessInstanceGroupFilter();
        List<Object> resultSet = (List<Object>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
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
            if(parties != null) {
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
            if(!filter.getStatus().contains(ProcessInstanceGroupFilter.StatusEnum.valueOf(processInstanceStatus.value()))){
                filter.getStatus().add(ProcessInstanceGroupFilter.StatusEnum.valueOf(processInstanceStatus.value()));
            }
        }
        return filter;
    }

    private static String getGroupRetrievalQuery(
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

        String query = "";
        if(queryType == GroupQueryType.FILTER) {
            query += "select distinct new list(relProd.item, relCat.item, doc.initiatorID, doc.responderID, pi.status)";
        } else if(queryType == GroupQueryType.SIZE) {
            query += "select count(distinct cg)";
        } else if(queryType == GroupQueryType.GROUP) {
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
                query += " relCat.item = '" + relatedProductCategories.get(i) + "' or";
            }
            query += " relCat.item = '" + relatedProductCategories.get(i) + "')";
        }
        if (relatedProductIds != null && relatedProductIds.size() > 0) {
            query += " and (";
            int i = 0;
            for (; i < relatedProductIds.size() - 1; i++) {
                query += " relProd.item = '" + relatedProductIds.get(i) + "' or";
            }
            query += " relProd.item = '" + relatedProductIds.get(i) + "')";
        }
        if (tradingPartnerIds != null && tradingPartnerIds.size() > 0) {
            query += " and (";
            int i = 0;
            for (; i < tradingPartnerIds.size() - 1; i++) {
                query += " (doc.initiatorID = '" + tradingPartnerIds.get(i) + "' or doc.responderID = '" + tradingPartnerIds.get(i) + "') or";
            }
            query += " (doc.initiatorID = '" + tradingPartnerIds.get(i) + "' or doc.responderID = '" + tradingPartnerIds.get(i) + "'))";
        }
        if (status != null && status.size() > 0) {
            query += " and (";
            int i = 0;
            for (; i < status.size() - 1; i++) {
                query += " (pi.status = '" + status.get(i) + "') or";
            }
            query += " (pi.status = '" + status.get(i) + "'))";
        }
        if (archived != null) {
            query += " and pig.archived = " + archived;
        }
        if (partyId != null) {
            query += " and pig.partyID ='" + partyId + "'";
        }
        if (collaborationRole != null) {
            query += " and pig.collaborationRole = '" + collaborationRole + "'";
        }

        if(queryType == GroupQueryType.GROUP) {
            query += " group by pig.hjid,cg.hjid";
            query += " order by firstActivityTime desc";
        }

        query += ") > 0";

        return query;
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
        for(int i = 0; i<relatedProducts.size();i++){
            if(i == relatedProducts.size()-1){
                groupName += relatedProducts.get(i);
            }
            else {
                groupName += relatedProducts.get(i)+",";
            }
        }
        group.setName(groupName);
        group.setPartyID(partyId);
        group.setStatus(GroupStatus.INPROGRESS);
        group.setCollaborationRole(collaborationRole);
        List<String> processInstanceIds = new ArrayList<>();
        processInstanceIds.add(processInstanceId);
        group.setProcessInstanceIDs(processInstanceIds);
        if(associatedGroup != null) {
            List<String> associatedGroups = new ArrayList<>();
            associatedGroups.add(associatedGroup);
            group.setAssociatedGroups(associatedGroups);
        }
        HibernateUtilityRef.getInstance("bp-data-model").persist(group);
        return group;
    }

    public static CollaborationGroupDAO createCollaborationGroupDAO(){
        CollaborationGroupDAO collaborationGroupDAO = new CollaborationGroupDAO();
        collaborationGroupDAO.setStatus(CollaborationStatus.INPROGRESS);
        collaborationGroupDAO.setArchived(false);
        HibernateUtilityRef.getInstance("bp-data-model").persist(collaborationGroupDAO);
        return collaborationGroupDAO;
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String groupID) {
        String query = "select pig, max(doc.submissionDate) as lastActivityTime, min(doc.submissionDate) as firstActivityTime from" +
                " ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid," +
                " ProcessInstanceDAO pi," +
                " ProcessDocumentMetadataDAO doc" +
                " where" +
                " ( pig.ID ='" + groupID+ "') and" +
                " pid.item = pi.processInstanceID and" +
                " doc.processInstanceID = pi.processInstanceID" +
                " group by pig.hjid";
        Object[] resultItems = (Object[]) (HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query));
        ProcessInstanceGroupDAO pig = (ProcessInstanceGroupDAO) resultItems[0];
        pig.setLastActivityTime((String) resultItems[1]);
        pig.setFirstActivityTime((String) resultItems[2]);
        return pig;
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String partyId, String associatedGroupId) {
        String query = "select pig from ProcessInstanceGroupDAO pig where pig.partyID = '" + partyId+ "' and pig.ID in " +
                "(select agrp.item from ProcessInstanceGroupDAO pig2 join pig2.associatedGroupsItems agrp where pig2.ID = '" + associatedGroupId + "')";
        ProcessInstanceGroupDAO group = (ProcessInstanceGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query);
        return group;
    }

    public static CollaborationGroupDAO getCollaborationGroupDAO(String partyId,Long associatedGroupId){
        String query = "select cg from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups pig where pig.partyID = '"+partyId+"' and cg.hjid in " +
                "(select acg.item from CollaborationGroupDAO cg2 join cg2.associatedCollaborationGroupsItems acg where cg2.hjid = "+associatedGroupId+")";
        CollaborationGroupDAO group = (CollaborationGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query);
        return group;
    }

    public static ProcessInstanceDAO getProcessInstance(String processInstanceId) {
        String queryStr = "SELECT pi FROM ProcessInstanceDAO pi WHERE pi.processInstanceID = ?";
        ProcessInstanceDAO pi = HibernateUtility.getInstance("bp-data-model").load(queryStr, processInstanceId);
        return pi;
    }

    public static List<ProcessInstanceDAO> getProcessInstances(List<String> ids) {
        String idsString = "(";
        int size = ids.size();
        for(int i=0;i<size;i++){
            if(i != size-1){
                idsString = idsString + "'"+ids.get(i)+"',";
            }
            else {
                idsString = idsString + "'"+ids.get(i)+"'";
            }
        }
        idsString = idsString + ")";

        String query = "select processInst from ProcessInstanceDAO processInst where processInst.processInstanceID in "+idsString;
        List<ProcessInstanceDAO> processInstanceDAOS = (List<ProcessInstanceDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        return processInstanceDAOS;
    }

    public static void deleteProcessInstanceGroupDAOByID(String groupID) {
        // get collaboration group which the given group belongs to
        String query = "select cg from CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups apig where apig.ID = '"+groupID+"'";

        CollaborationGroupDAO collaborationGroupDAO = (CollaborationGroupDAO ) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query);
        // if the collaboration group only contains the given group,then delete the collaboration group so that there will not be any garbage on the database
        if(collaborationGroupDAO.getAssociatedProcessInstanceGroups().size() == 1){
            deleteCollaborationGroupDAOByID(collaborationGroupDAO.getHjid());
        }
        else {
            ProcessInstanceGroupDAO group = null;
            for(ProcessInstanceGroupDAO groupDAO : collaborationGroupDAO.getAssociatedProcessInstanceGroups()){
                if(groupDAO.getID().equals(groupID)){
                    group = groupDAO;
                    break;
                }
            }
            // delete references to this group
            for(String id:group.getAssociatedGroups()){
                ProcessInstanceGroupDAO groupDAO =  getProcessInstanceGroupDAO(id);
                groupDAO.getAssociatedGroups().remove(groupID);
                HibernateUtilityRef.getInstance("bp-data-model").update(groupDAO);
            }
            HibernateUtilityRef.getInstance("bp-data-model").delete(ProcessInstanceGroupDAO.class, group.getHjid());
        }
    }

    public static void deleteCollaborationGroupDAOByID(Long groupID) {
        CollaborationGroupDAO group = (CollaborationGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").load(CollaborationGroupDAO.class,groupID);
        // delete references to this group
        for(Long id:group.getAssociatedCollaborationGroups()){
            CollaborationGroupDAO groupDAO = (CollaborationGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").load(CollaborationGroupDAO.class,id);
            groupDAO.getAssociatedCollaborationGroups().remove(groupID);
            HibernateUtilityRef.getInstance("bp-data-model").update(groupDAO);
        }
        HibernateUtilityRef.getInstance("bp-data-model").delete(CollaborationGroupDAO.class,groupID);
    }

    public static void archiveAllGroupsForParty(String partyId) {
        String query = "update ProcessInstanceGroupDAO as pig set pig.archived = true WHERE pig.partyID = '" + partyId + "'";
        HibernateUtilityRef.getInstance("bp-data-model").executeUpdate(query);
    }

    public static CollaborationGroupDAO archiveCollaborationGroup(String id){
        CollaborationGroupDAO collaborationGroupDAO = (CollaborationGroupDAO) HibernateUtility.getInstance("bp-data-model").load(CollaborationGroupDAO.class,Long.parseLong(id));
        // archive the collaboration group
        collaborationGroupDAO.setArchived(true);
        // archive the groups inside the given collaboration group
        for(ProcessInstanceGroupDAO processInstanceGroupDAO : collaborationGroupDAO.getAssociatedProcessInstanceGroups()){
            processInstanceGroupDAO.setArchived(true);
        }
        collaborationGroupDAO = (CollaborationGroupDAO) HibernateUtility.getInstance("bp-data-model").update(collaborationGroupDAO);
        return collaborationGroupDAO;
    }

    public static CollaborationGroupDAO restoreCollaborationGroup(String id){
        CollaborationGroupDAO collaborationGroupDAO = (CollaborationGroupDAO) HibernateUtility.getInstance("bp-data-model").load(CollaborationGroupDAO.class,Long.parseLong(id));
        // archive the collaboration group
        collaborationGroupDAO.setArchived(false);
        // archive the groups inside the given collaboration group
        for(ProcessInstanceGroupDAO processInstanceGroupDAO : collaborationGroupDAO.getAssociatedProcessInstanceGroups()){
            processInstanceGroupDAO.setArchived(false);
        }
        collaborationGroupDAO = (CollaborationGroupDAO) HibernateUtility.getInstance("bp-data-model").update(collaborationGroupDAO);
        return collaborationGroupDAO;
    }

    public static void deleteArchivedGroupsForParty(String partyId) {
        String query = "select pig.ID from ProcessInstanceGroupDAO pig WHERE pig.archived = true and pig.partyID = '" + partyId + "'";
        List<String> groupIds = (List<String>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        for(String id : groupIds){
            deleteProcessInstanceGroupDAOByID(id);
        }
    }

    public static String getOrderIdInGroup(String processInstanceId) {
        // get the id of preceding process instance if there is any
        String query = "SELECT pig.precedingProcess.processInstanceID FROM ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid," +
                " ProcessInstanceDAO pi " +
                " WHERE" +
                " pid.item = pi.processInstanceID AND " +
                " pi.processInstanceID = ? AND pig.precedingProcess IS NOT NULL";
        String precedingProcessInstanceID = HibernateUtility.getInstance("bp-data-model").load(query, processInstanceId);
        String orderId;
        // if there is a preceding process instance, using that, get the order id
        if(precedingProcessInstanceID != null){
            orderId = DocumentDAOUtility.getRequestMetadata(precedingProcessInstanceID).getDocumentID();
        }
        else {
            // get order
            query = "SELECT DISTINCT doc.documentID FROM ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid," +
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
                    " pi2.processInstanceID = ?" +
                    ")";

            orderId = HibernateUtility.getInstance("bp-data-model").load(query, processInstanceId);
        }

        return orderId;
    }

    private enum GroupQueryType {
        GROUP, FILTER, SIZE
    }
}
