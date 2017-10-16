package eu.nimble.service.bp.config;

import eu.nimble.utility.config.BluemixDatabaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by suat on 10-Oct-17.
 */
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "persistence.orm")
@PropertySource("classpath:bootstrap.yml")
public class BusinessProcessPersistenceConfig {

    private static BusinessProcessPersistenceConfig instance;

    private BusinessProcessPersistenceConfig() {
        instance = this;
    }

    private static boolean dbInitialized = false;
    public static BusinessProcessPersistenceConfig getInstance() {
        if(instance != null && dbInitialized == false) {
            instance.setupDbConnections();
            dbInitialized = true;
        }
        return instance;
    }

    @Autowired
    private Environment environment;

    private Map<String, String> business_process;

    @Bean
    @Primary
    public DataSource getDataSource() {
        if(Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.contentEquals("kubernetes"))) {
            String camundaDbCredentialsJson = environment.getProperty("nimble.db-credentials-json");
            BluemixDatabaseConfig config = new BluemixDatabaseConfig(camundaDbCredentialsJson);
            return DataSourceBuilder.create().url(config.getUrl()).username(config.getUsername()).password(config.getPassword()).driverClassName(config.getDriver()).build();
        } else {
            return DataSourceBuilder.create().url(environment.getProperty("spring.datasource.url")).build();
        }
    }

    public void setupDbConnections() {
        // update persistence properties if kubernetes profile is active
        if(Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.contentEquals("kubernetes"))) {
            String bpDbCredentialsJson = environment.getProperty("persistence.orm.business_process.bluemix.credentials_json");
            BluemixDatabaseConfig config = new BluemixDatabaseConfig(bpDbCredentialsJson);
//            config.copyToHibernatePersistenceParameters(business_process); ToDo: check if this is necessary.
        }
    }

    public Map<String, String> getBusiness_process() {
        return business_process;
    }

    public void setBusiness_process(Map<String, String> business_process) {
        this.business_process = business_process;
    }
}