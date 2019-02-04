package eu.nimble.service.bp.config;

import eu.nimble.utility.config.BluemixDatabaseConfig;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Created by suat on 22-Nov-18.
 */
@Component
public class DataSourceFactory {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Environment environment;

    public DataSource createDatasource(String dataSourceName) {
        javax.sql.DataSource ds;

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.contentEquals("kubernetes"))) {
            String camundaDbCredentialsJson = environment.getProperty("nimble." + dataSourceName + "-db-credentials-json");
            BluemixDatabaseConfig config = new BluemixDatabaseConfig(camundaDbCredentialsJson);
            ds = DataSourceBuilder.create()
                    .url(config.getUrl())
                    .username(config.getUsername())
                    .password(config.getPassword())
                    .driverClassName(config.getDriver())
                    .build();
        } else {
            logger.info("Creating datasource: url={}, user={}",
                    environment.getProperty("spring.datasource." + dataSourceName + ".url"),
                    environment.getProperty("spring.datasource." + dataSourceName + ".username"));

            ds = DataSourceBuilder.create()
                    .url(environment.getProperty("spring.datasource." + dataSourceName + ".url"))
                    .username(environment.getProperty("spring.datasource." + dataSourceName + ".username"))
                    .password(environment.getProperty("spring.datasource." + dataSourceName + ".password"))
                    .driverClassName(environment.getProperty("spring.datasource." + dataSourceName + ".driver-class"))
                    .build();
        }
        // Assume we make use of Apache Tomcat connection pooling (default in Spring Boot)
        org.apache.tomcat.jdbc.pool.DataSource tds = (org.apache.tomcat.jdbc.pool.DataSource) ds;
        tds.setTestOnBorrow(Boolean.valueOf(environment.getProperty("spring.datasource.test-on-borrow")));
        tds.setTestWhileIdle(Boolean.valueOf(environment.getProperty("spring.datasource.test-while-idle")));
        tds.setRemoveAbandoned(Boolean.valueOf(environment.getProperty("spring.datasource.remove-abandoned")));
        tds.setLogAbandoned(Boolean.valueOf(environment.getProperty("spring.datasource.log-abandoned")));
        tds.setInitialSize(Integer.valueOf(environment.getProperty("spring.datasource.initial-size")));
        tds.setMaxActive(Integer.valueOf(environment.getProperty("spring.datasource.max-active")));
        tds.setMaxIdle(Integer.valueOf(environment.getProperty("spring.datasource.max-idle")));
        tds.setMinIdle(Integer.valueOf(environment.getProperty("spring.datasource.min-idle")));
        tds.setMaxWait(Integer.valueOf(environment.getProperty("spring.datasource.max-wait")));
        tds.setTimeBetweenEvictionRunsMillis(Integer.valueOf(environment.getProperty("spring.datasource.time-between-eviction-runs-millis")));
        tds.setMinEvictableIdleTimeMillis(Integer.valueOf(environment.getProperty("spring.datasource.min-evictable-idle-time-millis")));
        tds.setValidationQuery(String.valueOf(environment.getProperty("spring.datasource.validation-query")));
        return tds;
    }
}
