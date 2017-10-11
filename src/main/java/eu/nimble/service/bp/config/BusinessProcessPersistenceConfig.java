package eu.nimble.service.bp.config;

import eu.nimble.utility.config.BluemixDatabaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

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

    public static BusinessProcessPersistenceConfig getInstance() {
        return instance;
    }

    @Autowired
    private Environment environment;

    @Value("persistence.orm.business_process.bluemix.connection.uri")
    private String bluemixBusiness_processDbUri;

    private Map<String, String> business_process;

    public String getBluemixBusiness_processDbUri() {
        return bluemixBusiness_processDbUri;
    }

    public void setBluemixBusiness_processDbUri(String bluemixBusiness_processDbUri) {
        this.bluemixBusiness_processDbUri = bluemixBusiness_processDbUri;
        // update persistence properties if kubernetes profile is active
        if(Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.contentEquals("kubernetes"))) {
            BluemixDatabaseConfig config = new BluemixDatabaseConfig(bluemixBusiness_processDbUri);
            config.copyToHibernatePersistenceParameters(config, business_process);
        }
    }

    public Map<String, String> getBusiness_process() {
        return business_process;
    }

    public void setBusiness_process(Map<String, String> business_process) {
        this.business_process = business_process;
    }
}