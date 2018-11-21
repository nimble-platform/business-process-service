package eu.nimble.service.bp.config.camunda;

import org.camunda.bpm.spring.boot.starter.configuration.CamundaDatasourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by suat on 19-Nov-18.
 */
@Configuration
public class CamundaConfig {

    @Bean
    public static CamundaDatasourceConfiguration camundaDatasourceConfiguration() {
        return new CamundaDatasourceConfigurationImpl();
    }
}