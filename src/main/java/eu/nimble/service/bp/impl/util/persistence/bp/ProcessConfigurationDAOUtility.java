package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessConfigurationDAO;
import eu.nimble.utility.persistence.JPARepositoryFactory;

import java.util.List;

/**
 * Created by suat on 02-Jan-19.
 */
public class ProcessConfigurationDAOUtility {
    private static final String QUERY_GET_BY_PARTNER_ID = "SELECT conf FROM ProcessConfigurationDAO conf WHERE conf.partnerID = :partnerId";
    private static final String QUERY_GET_BY_PARTNER_ID_AND_PROCESS_ID = "SELECT conf FROM ProcessConfigurationDAO conf WHERE conf.partnerID = :partnerId AND conf.processID = :processId";

    public static List<ProcessConfigurationDAO> getProcessConfigurations(String partnerId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_PARTNER_ID, new String[]{"partnerId"}, new Object[]{partnerId});
    }

    public static List<ProcessConfigurationDAO> getProcessConfigurations(String partnerId, String processId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_PARTNER_ID_AND_PROCESS_ID, new String[]{"partnerId", "processId"}, new Object[]{partnerId, processId});
    }
}
