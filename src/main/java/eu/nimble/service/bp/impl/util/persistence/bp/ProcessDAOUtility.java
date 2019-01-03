package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDAO;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

/**
 * Created by suat on 01-Jan-19.
 */
public class ProcessDAOUtility {
    private static final String QUERY_GET_BY_PROCESS_ID = "SELECT p FROM ProcessDAO p WHERE p.processID = :processId";

    public static ProcessDAO findByProcessID(String processId) {
        List<ProcessDAO> results = new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_PROCESS_ID, new String[]{"processId"}, new Object[]{processId});
        if(results.size() == 0) {
            return null;
        }
        return results.get(0);
    }

    public static List<ProcessDAO> getProcessDAOs() {
        List<ProcessDAO> resultSet = new JPARepositoryFactory().forBpRepository().getEntities(ProcessDAO.class);
        return resultSet;
    }
}
