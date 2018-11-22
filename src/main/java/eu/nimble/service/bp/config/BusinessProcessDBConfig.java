package eu.nimble.service.bp.config;

import eu.nimble.service.bp.impl.persistence.bp.BusinessProcessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties
@PropertySource("classpath:bootstrap.yml")
@EnableJpaRepositories(
        entityManagerFactoryRef = "bpdbEntityManagerFactory",
        transactionManagerRef = "bpdbTransactionManager",
        basePackages = {"eu.nimble.service.bp.impl.persistence.bp"}
)
@ComponentScan(basePackages = {"eu.nimble.service.bp.config"})
public class BusinessProcessDBConfig {

    @Autowired
    private DataSourceCreator dataSourceCreator;

    @Bean(name = "bpdbDataSource")
    @ConfigurationProperties(prefix = "persistence.orm.business_process.hibernate.connection")
    public DataSource getDataSource() {
        return dataSourceCreator.createDatasource();
    }

    @Bean(name = "bpdbEmfBean")
    public LocalContainerEntityManagerFactoryBean bpdbEntityManagerFactoryBean(
            EntityManagerFactoryBuilder builder,
            @Qualifier("bpdbHibernateConfigs") Map hibernateConfigs,
            @Qualifier("bpdbDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emfBean = builder
                .dataSource(dataSource)
                .persistenceUnit("bp-data-model")
                .build();

        Properties hibernateProperties = new Properties();
        hibernateProperties.putAll(hibernateConfigs);
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emfBean.setJpaVendorAdapter(vendorAdapter);
        emfBean.setJpaProperties(hibernateProperties);
        return emfBean;
    }

    @Bean(name = "bpdbEntityManagerFactory")
    public EntityManagerFactory bpdbEntityManagerFactory(@Qualifier("bpdbEmfBean") LocalContainerEntityManagerFactoryBean emfBean) {
        return emfBean.getObject();
    }

    @Bean(name = "bpdbTransactionManager")
    public PlatformTransactionManager bpdbTransactionManager(
            @Qualifier("bpdbEntityManagerFactory") EntityManagerFactory bpdbEntityManagerFactory) {
        return new JpaTransactionManager(bpdbEntityManagerFactory);
    }
}