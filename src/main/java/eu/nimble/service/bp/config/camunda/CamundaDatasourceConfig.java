package eu.nimble.service.bp.config.camunda;

import eu.nimble.service.bp.config.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Created by suat on 19-Nov-18.
 */
@Component
public class CamundaDatasourceConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Bean
    @Primary
    public DataSource getDataSource() {
        return dataSourceFactory.createDatasource("camunda");
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
