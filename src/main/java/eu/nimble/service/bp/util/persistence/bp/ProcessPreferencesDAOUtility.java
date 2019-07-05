package eu.nimble.service.bp.util.persistence.bp;

import eu.nimble.service.bp.model.hyperjaxb.ProcessPreferencesDAO;
import eu.nimble.utility.persistence.JPARepositoryFactory;
import org.springframework.data.repository.query.Param;

/**
 * Created by suat on 02-Jan-19.
 */
public class ProcessPreferencesDAOUtility {
    private static final String QUERY_GET_BY_PARTNER_ID = "SELECT conf FROM ProcessPreferencesDAO conf WHERE conf.partnerID = :partnerId";

    public static ProcessPreferencesDAO getProcessPreferences(@Param("partnerId") String partnerId,boolean lazyEnabled) {
        return new JPARepositoryFactory().forBpRepository(lazyEnabled).getSingleEntity(QUERY_GET_BY_PARTNER_ID, new String[]{"partnerId"}, new Object[]{partnerId});
    }

    public static ProcessPreferencesDAO getProcessPreferences(@Param("partnerId") String partnerId) {
        return getProcessPreferences(partnerId,true);
    }
}
