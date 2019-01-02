package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessInstanceDAO;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

/**
 * Created by suat on 01-Jan-19.
 */
public class ProcessInstanceDAOUtility {
    private static final String QUERY_GET_BY_ID = "SELECT pi FROM ProcessInstanceDAO pi WHERE pi.processInstanceID = :processInstanceId";

    public static List<ProcessInstanceDAO> findByProcessInstanceId(String processInstanceId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_ID, new String[]{"processInstanceId"}, new Object[]{processInstanceId});
    }
}
