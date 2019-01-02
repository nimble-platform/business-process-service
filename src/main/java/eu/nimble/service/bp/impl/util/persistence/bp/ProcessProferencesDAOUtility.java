package eu.nimble.service.bp.impl.util.persistence.bp;

import eu.nimble.service.bp.hyperjaxb.model.ProcessPreferencesDAO;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by suat on 02-Jan-19.
 */
public class ProcessProferencesDAOUtility {
    private static final String QUERY_GET_BY_PARTNER_ID = "SELECT conf FROM ProcessPreferencesDAO conf WHERE conf.partnerID = :partnerId";

    public static List<ProcessPreferencesDAO> getProcessPreferences(@Param("partnerId") String partnerId) {
        return new JPARepositoryFactory().forBpRepository().getEntities(QUERY_GET_BY_PARTNER_ID, new String[]{"partnerId"}, new Object[]{partnerId});
    }
}
