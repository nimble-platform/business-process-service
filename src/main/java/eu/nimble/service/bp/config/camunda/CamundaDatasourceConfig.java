package eu.nimble.service.bp.config.camunda;

import eu.nimble.service.bp.config.DataSourceCreator;
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
    private DataSourceCreator dataSourceCreator;

    @Bean
    @Primary
    public DataSource getDataSource() {
        return dataSourceCreator.createDatasource();
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
