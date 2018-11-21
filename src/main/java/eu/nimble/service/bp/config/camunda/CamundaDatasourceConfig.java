package eu.nimble.service.bp.config.camunda;

import eu.nimble.utility.config.BluemixDatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * Created by suat on 19-Nov-18.
 */
@Component
public class CamundaDatasourceConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Environment environment;

    @Bean
    @Primary
    public DataSource getDataSource() {
        DataSource ds;

        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.contentEquals("kubernetes"))) {
            String camundaDbCredentialsJson = environment.getProperty("nimble.db-credentials-json");
            BluemixDatabaseConfig config = new BluemixDatabaseConfig(camundaDbCredentialsJson);
            ds = DataSourceBuilder.create()
                    .url(config.getUrl())
                    .username(config.getUsername())
                    .password(config.getPassword())
                    .driverClassName(config.getDriver())
                    .build();
        } else {
            logger.info("Creating datasource: url={}, user={}",
                    environment.getProperty("spring.datasource.url"),
                    environment.getProperty("spring.datasource.username"));

            ds = DataSourceBuilder.create()
                    .url(environment.getProperty("spring.datasource.url"))
                    .username(environment.getProperty("spring.datasource.username"))
                    .password(environment.getProperty("spring.datasource.password"))
                    .driverClassName(environment.getProperty("spring.datasource.driverClassName"))
                    .build();
        }
        // Assume we make use of Apache Tomcat connection pooling (default in Spring Boot)
        org.apache.tomcat.jdbc.pool.DataSource tds = (org.apache.tomcat.jdbc.pool.DataSource) ds;
        tds.setInitialSize(Integer.valueOf(environment.getProperty("spring.datasource.tomcat.initial-size")));
        tds.setTestWhileIdle(Boolean.valueOf(environment.getProperty("spring.datasource.tomcat.test-while-idle").toUpperCase()));
        tds.setTimeBetweenEvictionRunsMillis(Integer.valueOf(environment.getProperty("spring.datasource.tomcat.time-between-eviction-runs-millis")));
        tds.setMinEvictableIdleTimeMillis(Integer.valueOf(environment.getProperty("spring.datasource.tomcat.min-evictable-idle-time-millis")));
        return tds;
    }

    @Bean(name = "camundaEmfBean")
    @Primary
    public LocalContainerEntityManagerFactoryBean camundaEntityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactoryBean.setPackagesToScan("org.camunda");
        return entityManagerFactoryBean;
    }

    @Bean(name = "camundaTm")
    public JpaTransactionManager transactionManager(@Qualifier("camundaEmfBean") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }
}
