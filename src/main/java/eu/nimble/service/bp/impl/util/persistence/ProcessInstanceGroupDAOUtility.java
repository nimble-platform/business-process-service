package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceGroupDAO;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;

import java.security.acl.Group;
import java.util.List;

/**
 * Created by suat on 26-Mar-18.
 */
public class ProcessInstanceGroupDAOUtility {
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
        List<ProcessInstanceGroupDAO> groups = (List<ProcessInstanceGroupDAO>) HibernateUtilityRef.getInstance("bp-data-model").loadAll(query, offset, limit);
        return groups;
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

    public static ProcessInstanceGroupFilter getFilterDetails(
            String partyId,
            String collaborationRole,
            Boolean archived,
            List<String> tradingPartnerIds,
            List<String> relatedProductIds,
            List<String> relatedProductCategories,
            String startTime,
            String endTime) {

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

            // populate partners' names
            //TODO gather these from the actual REST services
            for (String tradingPartnerId : filter.getTradingPartnerIDs()) {
                String partnerName = null;
                if (tradingPartnerId.contentEquals("482")) {
                    partnerName = "SRDC";
                } else if (tradingPartnerId.contentEquals("554")) {
                    partnerName = "Alici";
                } else if (tradingPartnerId.contentEquals("1621")) {
                    partnerName = "Transport";
                }


                if (partnerName != null && !filter.getTradingPartnerNames().contains(partnerName)) {
                    filter.getTradingPartnerNames().add(partnerName);
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
            query += "select count(*)";
        } else if(queryType == GroupQueryType.GROUP) {
            query += "select pig";
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
        return query;
    }

    private enum GroupQueryType {
        GROUP, FILTER, SIZE
    }
}
