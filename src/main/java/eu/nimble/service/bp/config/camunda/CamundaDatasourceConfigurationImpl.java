package eu.nimble.service.bp.config.camunda;

import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaDatasourceConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.camunda.bpm.spring.boot.starter.property.DatabaseProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Created by suat on 19-Nov-18.
 */

public class CamundaDatasourceConfigurationImpl extends AbstractCamundaConfiguration implements CamundaDatasourceConfiguration {
    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("transactionManager")
    private PlatformTransactionManager platformTransactionManager;

    @Override
    public void preInit(SpringProcessEngineConfiguration configuration) {
        final DatabaseProperty database = camundaBpmProperties.getDatabase();

        configuration.setTransactionManager(platformTransactionManager);
        configuration.setDataSource(dataSource);
        configuration.setDatabaseType(database.getType());
        configuration.setDatabaseSchemaUpdate(database.getSchemaUpdate());

        if (!StringUtils.isEmpty(database.getTablePrefix())) {
            configuration.setDatabaseTablePrefix(database.getTablePrefix());
        }
    }
}
