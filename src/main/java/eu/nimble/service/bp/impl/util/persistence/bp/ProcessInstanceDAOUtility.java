package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suat on 01-Jan-19.
 */
public class ProcessInstanceDAOUtility {
    private static final String QUERY_GET_BY_ID = "SELECT pi FROM ProcessInstanceDAO pi WHERE pi.processInstanceID = :processInstanceId";

    public static ProcessInstanceDAO getById(String processInstanceId) {
        return new JPARepositoryFactory().forBpRepository().getSingleEntity(QUERY_GET_BY_ID, new String[]{"processInstanceId"}, new Object[]{processInstanceId});
    }

    public static List<String> getAllProcessInstanceIdsInCollaborationHistory(String processInstanceID) {
        List<String> processInstanceIDs = new ArrayList<>();
        ProcessInstanceDAO processInstanceDAO = ProcessInstanceDAOUtility.getById(processInstanceID);
        processInstanceIDs.add(0, processInstanceID);
        while (processInstanceDAO.getPrecedingProcess() != null) {
            processInstanceDAO = ProcessInstanceDAOUtility.getById(processInstanceDAO.getPrecedingProcess().getProcessInstanceID());
            processInstanceIDs.add(0, processInstanceDAO.getProcessInstanceID());
        }
        return processInstanceIDs;
    }
}
