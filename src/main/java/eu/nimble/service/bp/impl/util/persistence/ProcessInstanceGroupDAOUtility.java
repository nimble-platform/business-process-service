package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.common.rest.identity.IdentityClient;
import eu.nimble.common.rest.identity.IdentityClientTyped;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.impl.DocumentController;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.order.OrderType;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by suat on 26-Mar-18.
 */
@Component
public class ProcessInstanceGroupDAOUtility {
    @Autowired
    private IdentityClientTyped identityClient;

    public static List<ProcessInstanceGroupDAO> getProcessInstanceGroupDAOs(
            String partyId,
            String collaborationRole,
            Boolean archived,
            List<String> tradingPartnerIds,
            List<String> relatedProductIds,
            List<String> relatedProductCategories,
            String startTime,
            String endTime,
            int limit,
            int offset) {

        String query = getGroupRetrievalQuery(GroupQueryType.GROUP, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, startTime, endTime);
        List<Object> groups = (List<Object>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query, offset, limit);
        List<ProcessInstanceGroupDAO> results = new ArrayList<>();
        for(Object groupResult : groups) {
            Object[] resultItems = (Object[]) groupResult;
            ProcessInstanceGroupDAO group = (ProcessInstanceGroupDAO) resultItems[0];
            group.setLastActivityTime((String) resultItems[1]);
            group.setFirstActivityTime((String) resultItems[2]);
            results.add(group);
        }
        return results;
    }

    public static int getProcessInstanceGroupSize(String partyId,
                                                  String collaborationRole,
                                                  boolean archived,
                                                  List<String> tradingPartnerIds,
                                                  List<String> relatedProductIds,
                                                  List<String> relatedProductCategories,
                                                  String startTime,
                                                  String endTime) {
        String query = getGroupRetrievalQuery(GroupQueryType.SIZE, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, startTime, endTime);
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
            String startTime,
            String endTime,
            String bearerToken) {

        String query = getGroupRetrievalQuery(GroupQueryType.FILTER, partyId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, startTime, endTime);

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

            List<PartyType> parties = identityClient.getParties(bearerToken, filter.getTradingPartnerIDs());

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
            String startTime,
            String endTime) {

        String query = "";
        if(queryType == GroupQueryType.FILTER) {
            query += "select distinct new list(relProd.item, relCat.item, doc.initiatorID, doc.responderID)";
        } else if(queryType == GroupQueryType.SIZE) {
            query += "select count(distinct pig)";
        } else if(queryType == GroupQueryType.GROUP) {
            query += "select pig, max(doc.submissionDate) as lastActivityTime, min(doc.submissionDate) as firstActivityTime";
        }

        query += " from " +
                "ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid, " +
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
            query += " group by pig.hjid";
            query += " order by firstActivityTime desc";
        }
        return query;
    }

    public static ProcessInstanceGroupDAO createProcessInstanceGroupDAO(String partyId, String processInstanceId, String collaborationRole, String relatedProducts) {
        return createProcessInstanceGroupDAO(partyId, processInstanceId, collaborationRole, relatedProducts, null);
    }

    public static ProcessInstanceGroupDAO createProcessInstanceGroupDAO(String partyId, String processInstanceId, String collaborationRole, String relatedProducts, String associatedGroup) {
        String uuid = UUID.randomUUID().toString();
        ProcessInstanceGroupDAO group = new ProcessInstanceGroupDAO();
        group.setArchived(false);
        group.setID(uuid);
        group.setName(relatedProducts);
        group.setPartyID(partyId);
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
        String query = "select pig from ProcessInstanceGroupDAO pig where ( pig.ID ='" + groupID+ "') ";
        ProcessInstanceGroupDAO group = (ProcessInstanceGroupDAO) HibernateUtilityRef.getInstance("bp-data-model").loadIndividualItem(query);
        HibernateUtilityRef.getInstance("bp-data-model").delete(ProcessInstanceGroupDAO.class, group.getHjid());
    }

    public static void archiveAllGroupsForParty(String partyId) {
        String query = "update ProcessInstanceGroupDAO as pig set pig.archived = true WHERE pig.partyID = '" + partyId + "'";
        HibernateUtilityRef.getInstance("bp-data-model").executeUpdate(query);
    }

    public static void deleteArchivedGroupsForParty(String partyId) {
        String query = "select pig.hjid from ProcessInstanceGroupDAO pig WHERE pig.archived = true and pig.partyID = '" + partyId + "'";
        List<Long> longs = (List<Long>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query);
        for(Long hjid : longs){
            HibernateUtilityRef.getInstance("bp-data-model").delete(ProcessInstanceGroupDAO.class,hjid);
        }
    }

    public static String getOrderIdInGroup(String processInstanceId) {
        // get order
        String query = "SELECT DISTINCT doc.documentID FROM ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid," +
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

        String orderId = HibernateUtility.getInstance("bp-data-model").load(query, processInstanceId);
        return orderId;
    }

    private enum GroupQueryType {
        GROUP, FILTER, SIZE
    }
}
