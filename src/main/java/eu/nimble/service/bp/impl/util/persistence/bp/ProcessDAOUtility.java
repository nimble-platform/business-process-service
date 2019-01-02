package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessDAO;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

/**
 * Created by suat on 01-Jan-19.
 */
public class ProcessDAOUtility {
    private static final String QUERY_GET_BY_PROCESS_ID = "SELECT p FROM ProcessDAO p WHERE p.processID = :processId";

    public static List<ProcessDAO> findByProcessID(String processId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_PROCESS_ID, new String[]{"processId"}, new Object[]{processId});
    }
}
