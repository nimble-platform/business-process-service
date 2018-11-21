package eu.nimble.service.bp.impl.util.persistence;

import eu.nimble.service.bp.config.BusinessProcessPersistenceConfig;
import eu.nimble.service.bp.impl.util.spring.SpringBridge;
import eu.nimble.utility.Configuration;
import eu.nimble.utility.HibernateUtility;

import java.util.Map;

/**
 * A class to retrieve the hibernate related configurations from this application's context and use them to initialize
 * relevant {@link eu.nimble.utility.HibernateUtility} instances
 *
 * Created by suat on 11-Oct-17.
 */
public class HibernateUtilityRef {
    public static HibernateUtility getInstance(String persistenceUnitName) {
        HibernateUtility utility = null;
        if(persistenceUnitName.contentEquals("bp-data-model")) {
            Map<String, String> persistenceProperties = SpringBridge.getInstance().getBpConfig().getBusiness_process();
            utility = HibernateUtility.getInstance(persistenceUnitName, persistenceProperties);

        }
        else if(persistenceUnitName.contentEquals(Configuration.UBL_PERSISTENCE_UNIT_NAME) ||
                persistenceUnitName.contentEquals(Configuration.MODAML_PERSISTENCE_UNIT_NAME)) {
            utility = HibernateUtility.getInstance(persistenceUnitName);
        }

        return utility;
    }
}
