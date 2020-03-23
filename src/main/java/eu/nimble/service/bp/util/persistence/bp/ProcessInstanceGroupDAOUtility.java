package eu.nimble.service.bp.util.persistence.bp;

import eu.nimble.service.bp.model.hyperjaxb.*;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroup;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupFilter;
import eu.nimble.service.bp.swagger.model.ProcessInstanceGroupResponse;
import eu.nimble.service.bp.util.persistence.catalogue.DocumentPersistenceUtility;
import eu.nimble.service.bp.util.persistence.catalogue.PartyPersistenceUtility;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyNameType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.utility.HibernateUtility;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.apache.xpath.operations.Bool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by suat on 26-Mar-18.
 */
public class ProcessInstanceGroupDAOUtility {

    private static final Logger logger = LoggerFactory.getLogger(ProcessInstanceGroupDAOUtility.class);

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
            "select pig from ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid where pig.partyID = :partyId and pig.federationID = :federationId and pid.item in :pids";

    private static final String QUERY_GET_ASSOCIATED_GROUPS =
            "select pig from ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid where pig.partyID <> :partyId and pig.federationID = :federationId and pid.item in :pids";

    private static final String QUERY_GET_PRECEDING_PROCESS_INSTANCE_GROUP =
            "SELECT pig.precedingProcessInstanceGroupMetadata FROM ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid," +
                    "ProcessInstanceDAO pi " +
                    "WHERE " +
                    "pid.item = pi.processInstanceID AND " +
                    "pi.processInstanceID = :processInstanceId AND pig.precedingProcessInstanceGroupMetadata IS NOT NULL";

    private static final String QUERY_GET_CONTAINING_THE_PROCESS =
            "SELECT pig FROM ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid WHERE pid.item = :processInstanceID";

    private static final String QUERY_GET_BY_PARTY_ID = "SELECT pig.ID FROM ProcessInstanceGroupDAO pig WHERE pig.partyID in :partyIds";
    private static final String QUERY_GET_METADATAS_FOR_PROCESS_INSTANGE_GROUP =
            "SELECT docMetadata " +
                    " FROM ProcessDocumentMetadataDAO docMetadata, ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid" +
                    " WHERE docMetadata.processInstanceID = pid.item" +
                    "   AND pig.ID = :pigId";

    public static List<String> getProcessInstanceGroupIdsForParty(List<String> partyIds){
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_PARTY_ID, new String[]{"partyIds"}, new Object[]{partyIds});
    }

    public static Object getProcessInstanceGroups(String groupId, GenericJPARepository repository) {
        return repository.getSingleEntity(QUERY_GET_PROCESS_INSTANCE_GROUPS, new String[]{"groupId"}, new Object[]{groupId});
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String partyId, String federationId, List<String> processInstanceIds, GenericJPARepository genericJPARepository) {
        return genericJPARepository.getSingleEntity(QUERY_GET_BY_ASSOCIATED_GROUP_ID, new String[]{"partyId", "pids","federationId"}, new Object[]{partyId, processInstanceIds,federationId});
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String partyId,String federationId, List<String> processInstanceIds) {
        return getProcessInstanceGroupDAO(partyId, federationId, processInstanceIds, new JPARepositoryFactory().forBpRepository(true));
    }

    /**
     * This method is used to retrieve process instance groups which contain the given process instance id.
     */
    public static List<ProcessInstanceGroupDAO> getProcessInstanceGroupDAOs(String processInstanceId){
        return getProcessInstanceGroupDAOs(processInstanceId, new JPARepositoryFactory().forBpRepository(true));
    }

    public static List<ProcessInstanceGroupDAO> getProcessInstanceGroupDAOs(String processInstanceId, GenericJPARepository repository){
        return repository.getEntities(QUERY_GET_CONTAINING_THE_PROCESS, new String[]{"processInstanceID"}, new Object[]{processInstanceId});
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String partyId,String federationId, List<String> processInstanceIds,boolean lazyDisabled) {
        return new JPARepositoryFactory().forBpRepository(lazyDisabled).getSingleEntity(QUERY_GET_BY_ASSOCIATED_GROUP_ID, new String[]{"partyId", "pids","federationId"}, new Object[]{partyId, processInstanceIds,federationId});
    }

    public static FederatedCollaborationGroupMetadataDAO getPrecedingProcessInstanceGroup(String processInstanceId) {
        return new JPARepositoryFactory().forBpRepository(true).getSingleEntity(QUERY_GET_PRECEDING_PROCESS_INSTANCE_GROUP, new String[]{"processInstanceId"}, new Object[]{processInstanceId});
    }

    public static List<ProcessInstanceGroupDAO> getAssociatedProcessInstanceGroupDAOs(String partyId, String federationId, List<String> processInstanceIds){
        return new JPARepositoryFactory().forBpRepository(true).getEntities(QUERY_GET_ASSOCIATED_GROUPS, new String[]{"partyId", "pids","federationId"}, new Object[]{partyId, processInstanceIds,federationId});
    }

    public static ProcessInstanceGroupDAO createProcessInstanceGroupDAO(String partyId, String federationId, String processInstanceId, String collaborationRole, List<String> relatedProducts,GenericJPARepository repo) {
        return createProcessInstanceGroupDAO(partyId, federationId, processInstanceId, collaborationRole, relatedProducts, null, repo);
    }

    public static ProcessInstanceGroupDAO createProcessInstanceGroupDAO(String partyId, String federationId, String processInstanceId, String collaborationRole, List<String> relatedProducts, String dataChannelId, GenericJPARepository repo) {
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
        group.setFederationID(federationId);
        group.setStatus(GroupStatus.INPROGRESS);
        group.setDataChannelId(dataChannelId);
        group.setCollaborationRole(collaborationRole);
        List<String> processInstanceIds = new ArrayList<>();
        processInstanceIds.add(processInstanceId);
        group.setProcessInstanceIDs(processInstanceIds);
        repo.persistEntity(group);
        return group;
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String groupID, GenericJPARepository repository) {
        Object[] resultItems = (Object[]) getProcessInstanceGroups(groupID,repository);
        // return null if there is no process instance group with the given id
        if(resultItems == null){
            return null;
        }
        ProcessInstanceGroupDAO pig = (ProcessInstanceGroupDAO) resultItems[0];
        pig.setLastActivityTime((String) resultItems[1]);
        pig.setFirstActivityTime((String) resultItems[2]);
        return pig;
    }

    public static ProcessInstanceGroupDAO getProcessInstanceGroupDAO(String groupID) {
        return getProcessInstanceGroupDAO(groupID,new JPARepositoryFactory().forBpRepository(true));
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
            for (ProcessInstanceGroupDAO groupDAO : collaborationGroupDAO.getAssociatedProcessInstanceGroups()) {
                if (groupDAO.getID().equals(groupID)) {
                    GenericJPARepository repo = new JPARepositoryFactory().forBpRepository();
                    repo.deleteEntityByHjid(ProcessInstanceGroupDAO.class, groupDAO.getHjid());
                    return;
                }
            }
        }
    }

    /**
     * @param processInstanceId Identifier of a transport-related process instance
     */
    public static String getSourceOrderResponseIdForTransportRelatedProcess(String processInstanceId) {
        // 1) First find the OrderResponse id by checking the ProcessInstanceGroups associated this ProcessInstance id
        // 2) Find the corresponding Order id via the ProcessInstance associated to the OrderResponse

        // find order response

        // get ProcessInstanceGroups including the specified process instance id
        List<ProcessInstanceGroupDAO> pigs = ProcessInstanceGroupDAOUtility.getProcessInstanceGroupDAOs(processInstanceId);
        if(pigs == null || pigs.size() == 0) {
            return null;
        }

        // collect the ProcessDocumentMetadata for the ProcessInstances included in the identified ProcessInstanceGroups
        List<ProcessDocumentMetadataDAO> docMetadatas = new ArrayList<>();
        for(ProcessInstanceGroupDAO pig : pigs) {
            List<ProcessDocumentMetadataDAO> docMetadata = getDocumentMetadataInProcessInstanceGroup(pig.getID());
            if(docMetadata != null) {
                docMetadatas.addAll(docMetadata);
            }
        }

        // traverse document ProcessDocumentMetadata. If they are of type IIR or RFQ, we check the associated documents in the actual document
        String orderResponseId = null;
        for(ProcessDocumentMetadataDAO docMetadata : docMetadatas) {
            List<Object> docRefs = null;
            if(docMetadata.getType().toString().contentEquals(DocumentType.ITEMINFORMATIONREQUEST.toString())) {
                docRefs = DocumentPersistenceUtility.getAdditionalDocumentTypesFromIir(docMetadata.getDocumentID());
            } else if(docMetadata.getType().toString().contentEquals(DocumentType.REQUESTFORQUOTATION.toString())) {
                docRefs = DocumentPersistenceUtility.getAdditionalDocumentTypesFromRfq(docMetadata.getDocumentID());
            }

            if(docRefs != null) {
                for(Object docRefInfo : docRefs) {
                    Object[] docRefInfoArray = (Object[]) docRefInfo;
                    String docType = (String) docRefInfoArray[1];
                    if(docType.contentEquals("previousOrder")) {
                        orderResponseId = (String) docRefInfoArray[0];
                    }
                }
            }
        }

        return orderResponseId;
    }

    public static List<ProcessDocumentMetadataDAO> getDocumentMetadataInProcessInstanceGroup(String pigId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_METADATAS_FOR_PROCESS_INSTANGE_GROUP, new String[]{"pigId"}, new Object[]{pigId});
    }

    public static ProcessInstanceGroupResponse getProcessInstanceGroupResponses(
            String partyId,
            String federationId,
            String collaborationRole,
            Boolean archived,
            List<String> tradingPartnerIds,
            List<String> relatedProductIds,
            List<String> relatedProductCategories,
            List<String> status,
            int limit,
            int offset,
            Boolean isProject) {

        CollaborationGroupDAOUtility.QueryData query = getGroupRetrievalQuery(ProcessInstanceGroupQueryType.GROUP, partyId, federationId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, isProject);
        List<Object> processInstanceGroups = new JPARepositoryFactory().forBpRepository(true).getEntities(query.getQuery(), query.getParameterNames().toArray(new String[query.getParameterNames().size()]), query.getParameterValues().toArray(), limit, offset);

        List<ProcessInstanceGroup> pigList = new ArrayList<>();
        List<String> cgIdList = new ArrayList<>();
        for (Object groupResult : processInstanceGroups) {
            Object[] resultItems = (Object[]) groupResult;
            ProcessInstanceGroupDAO processInstanceGroupDAO = (ProcessInstanceGroupDAO) resultItems[0];
            processInstanceGroupDAO.setLastActivityTime((String) resultItems[1]);
            processInstanceGroupDAO.setFirstActivityTime((String) resultItems[2]);

            pigList.add(HibernateSwaggerObjectMapper.convertProcessInstanceGroupDAO((ProcessInstanceGroupDAO) resultItems[0]));
            cgIdList.add(((Long) resultItems[3]).toString());
        }

        ProcessInstanceGroupResponse response = new ProcessInstanceGroupResponse();
        response.setGroups(pigList);
        response.setCollaborationGroupIds(cgIdList);
        return response;
    }

    public static ProcessInstanceGroupFilter getFilterDetails(
            String partyId,
            String federationId,
            String collaborationRole,
            Boolean archived,
            List<String> tradingPartnerIds,
            List<String> relatedProductIds,
            List<String> relatedProductCategories,
            List<String> status,
            String startTime,
            String endTime,
            Boolean isProject,
            String bearerToken) {

        CollaborationGroupDAOUtility.QueryData query = getGroupRetrievalQuery(
                ProcessInstanceGroupDAOUtility.ProcessInstanceGroupQueryType.FILTER, partyId, federationId,
                collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, isProject);

        ProcessInstanceGroupFilter filter = new ProcessInstanceGroupFilter();
        List<Object> resultSet = new JPARepositoryFactory().forBpRepository().getEntities(
                query.getQuery(), query.getParameterNames().toArray(new String[query.getParameterNames().size()]), query.getParameterValues().toArray());
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
            String federationIdColumn = (String) returnedColumns.get(3);
            if (resultColumn.contentEquals(partyId)) {
                resultColumn = (String) returnedColumns.get(4);
                federationIdColumn = (String) returnedColumns.get(5);
            }
            if (!filter.getTradingPartnerIDs().contains(resultColumn)) {
                filter.getTradingPartnerIDs().add(resultColumn);
                filter.getTradingPartnerFederationIds().add(federationIdColumn);
            }

            // status
            GroupStatus groupStatus = (GroupStatus) returnedColumns.get(6);
            if (!filter.getStatus().contains(ProcessInstanceGroupFilter.StatusEnum.valueOf(groupStatus.value()))) {
                filter.getStatus().add(ProcessInstanceGroupFilter.StatusEnum.valueOf(groupStatus.value()));
            }
        }

        List<PartyType> parties = null;
        try {
            if(filter.getTradingPartnerIDs().size() > 0){
                parties = PartyPersistenceUtility.getParties(bearerToken, new ArrayList<>(filter.getTradingPartnerIDs()),new ArrayList<>(filter.getTradingPartnerFederationIds()));
            }
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

        return filter;
    }

    public static int getProcessInstanceGroupCount(
            String partyId, String federationId, String collaborationRole, boolean archived, List<String> tradingPartnerIds,
            List<String> relatedProductIds, List<String> relatedProductCategories, List<String> status, Boolean isProject) {

        CollaborationGroupDAOUtility.QueryData query = getGroupRetrievalQuery(ProcessInstanceGroupQueryType.COUNT, partyId, federationId, collaborationRole, archived, tradingPartnerIds, relatedProductIds, relatedProductCategories, status, isProject);
        int count = ((Long) new JPARepositoryFactory().forBpRepository().getSingleEntity(query.getQuery(), query.getParameterNames().toArray(new String[query.getParameterValues().size()]), query.getParameterValues().toArray())).intValue();

        return count;
    }

    private static CollaborationGroupDAOUtility.QueryData getGroupRetrievalQuery(
            ProcessInstanceGroupDAOUtility.ProcessInstanceGroupQueryType queryType, String partyId, String federationId,
            String collaborationRole, Boolean archived, List<String> tradingPartnerIds, List<String> relatedProductIds,
            List<String> relatedProductCategories, List<String> status, Boolean isProject) {

        String query = "";
        if (queryType == ProcessInstanceGroupDAOUtility.ProcessInstanceGroupQueryType.FILTER) {
            query += "select distinct new list(relProd.item, relCat.item, doc.initiatorID, doc.initiatorFederationID, doc.responderID, doc.responderFederationID, pig.status)";
        } else if (queryType == ProcessInstanceGroupDAOUtility.ProcessInstanceGroupQueryType.COUNT) {
            query += "select count(distinct pig)";
        } else if (queryType == ProcessInstanceGroupDAOUtility.ProcessInstanceGroupQueryType.GROUP) {
            query += "select pig, max(doc.submissionDate) as lastActivityTime, min(doc.submissionDate) as firstActivityTime, cg.hjid";
        }

        query += " from " +
                "CollaborationGroupDAO cg join cg.associatedProcessInstanceGroups pig join pig.processInstanceIDsItems pid, " +
                "ProcessInstanceDAO pi, " +
                "ProcessDocumentMetadataDAO doc left join doc.relatedProductCategoriesItems relCat left join doc.relatedProductsItems relProd" +
                " where " +
                "pid.item = pi.processInstanceID and doc.processInstanceID = pi.processInstanceID";

        CollaborationGroupDAOUtility.QueryData queryData = getGroupCriteria(
                partyId, federationId,
                collaborationRole,
                archived,
                tradingPartnerIds,
                relatedProductIds,
                relatedProductCategories,
                status, isProject
        );

        // append the order condition
        if (queryType == ProcessInstanceGroupQueryType.GROUP) {
            String orderGroupCriteria = " group by pig.hjid, cg.hjid";
            orderGroupCriteria += " order by lastActivityTime desc";
            queryData.setQuery(queryData.getQuery() + orderGroupCriteria);
        }

        // concatenate the initial part with the group criteria
        queryData.setQuery(query + queryData.getQuery());
        return queryData;
    }

    public static CollaborationGroupDAOUtility.QueryData getGroupCriteria(
            String partyId, String federationId, String collaborationRole, Boolean archived, List<String> tradingPartnerIds,
            List<String> relatedProductIds, List<String> relatedProductCategories, List<String> status, Boolean isProject
    ) {
        CollaborationGroupDAOUtility.QueryData queryData = new CollaborationGroupDAOUtility.QueryData();
        List<String> parameterNames = queryData.getParameterNames();
        List<Object> parameterValues = queryData.getParameterValues();

        String query = "";
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
                query += " (pig.status = '" + GroupStatus.valueOf(status.get(i)).toString() + "') or";
            }
            query += " (pig.status = '" + GroupStatus.valueOf(status.get(i)).toString() + "'))";
        }
        if (archived != null) {
            query += " and pig.archived = :archived";

            parameterNames.add("archived");
            parameterValues.add(archived);
        }
        if (partyId != null && federationId !=null) {
            query += " and pig.partyID = :partyId and pig.federationID = :federationId";

            parameterNames.add("partyId");
            parameterNames.add("federationId");
            parameterValues.add(partyId);
            parameterValues.add(federationId);
        }
        if (collaborationRole != null) {
            query += " and pig.collaborationRole = :role";

            parameterNames.add("role");
            parameterValues.add(collaborationRole);
        }
        if (isProject) {
            query += " and cg.isProject = true";
        }

        queryData.setQuery(query);
        return queryData;
    }

    public enum ProcessInstanceGroupQueryType {
        GROUP, FILTER, COUNT
    }
}
