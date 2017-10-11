package eu.nimble.service.bp.config;

import eu.nimble.utility.config.BluemixDatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.json.BasicJsonParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Configuration
@Profile("kubernetes")
@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public class KubernetesDataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesDataSourceConfig.class);

    @Value("${nimble.db-credentials-json}")
    private String dbCredentialsJson;

    @Bean
    @Primary
    public DataSource getDataSource() {
        BluemixDatabaseConfig config = new BluemixDatabaseConfig(dbCredentialsJson);
        return DataSourceBuilder.create().url(config.getUrl()).username(config.getUsername()).password(config.getPassword()).driverClassName(config.getDriver()).build();
    }
}