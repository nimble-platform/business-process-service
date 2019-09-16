package eu.nimble.service.bp.util.persistence.bp;

import eu.nimble.service.bp.model.hyperjaxb.ProcessInstanceDAO;
import eu.nimble.utility.persistence.GenericJPARepository;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by suat on 01-Jan-19.
 */
public class ProcessInstanceDAOUtility {
    private static final String QUERY_GET_BY_ID = "SELECT pi FROM ProcessInstanceDAO pi WHERE pi.processInstanceID = :processInstanceId";
    private static final String QUERY_DELETE_BY_IDS = "DELETE FROM ProcessInstanceDAO pi WHERE pi.processInstanceID in :processInstanceIds";
    private static final String QUERY_GET_PROCESS_INSTANCE_DAOS_IN_GROUP =
            "SELECT distinct pi FROM ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid,ProcessInstanceDAO pi WHERE pi.processInstanceID = pid.item and pig.ID = :pig ORDER BY pi.creationDate DESC";

    private static final String QUERY_GET_PROCESS_INSTANCE_DAOS_IN_COLLABORATION =
            "SELECT distinct pi FROM ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid,ProcessInstanceDAO pi WHERE pi.processInstanceID = pid.item and pig.ID IN" +
                    "(" +
                    "select distinct pig.ID from ProcessInstanceGroupDAO pig join pig.processInstanceIDsItems pid where pid.item in :pids" +
                    ")" +
                    "ORDER BY pi.creationDate DESC";

    public static ProcessInstanceDAO getById(String processInstanceId) {
        return getById(processInstanceId,new JPARepositoryFactory().forBpRepository(true));
    }

    public static ProcessInstanceDAO getById(String processInstanceId, GenericJPARepository repository) {
        return repository.getSingleEntity(QUERY_GET_BY_ID, new String[]{"processInstanceId"}, new Object[]{processInstanceId});
    }

    public static void deleteByIds(List<String> processInstanceIds){
        new JPARepositoryFactory().forBpRepository().executeUpdate(QUERY_DELETE_BY_IDS, new String[]{"processInstanceIds"}, new Object[]{processInstanceIds});
    }

    public static List<ProcessInstanceDAO> getProcessInstancesIncludedInTheGroup(String processInstanceGroupID) {
        return new JPARepositoryFactory().forBpRepository(true).getEntities(QUERY_GET_PROCESS_INSTANCE_DAOS_IN_GROUP, new String[]{"pig"}, new Object[]{processInstanceGroupID});
    }

    public static List<ProcessInstanceDAO> getAllProcessInstancesInCollaborationHistory(String processInstanceID,GenericJPARepository repository) {
        return repository.getEntities(QUERY_GET_PROCESS_INSTANCE_DAOS_IN_COLLABORATION, new String[]{"pids"}, new Object[]{Arrays.asList(processInstanceID)});
    }
    public static List<ProcessInstanceDAO> getAllProcessInstancesInCollaborationHistory(String processInstanceID) {
        return getAllProcessInstancesInCollaborationHistory(processInstanceID,new JPARepositoryFactory().forBpRepository(true));
    }
}
