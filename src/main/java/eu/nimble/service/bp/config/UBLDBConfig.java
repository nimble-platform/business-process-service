package eu.nimble.service.bp.config;

import eu.nimble.service.bp.impl.persistence.catalogue.CatalogueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
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
        entityManagerFactoryRef = "ubldbEntityManagerFactory",
        transactionManagerRef = "ubldbTransactionManager",
        basePackages = {"eu.nimble.service.bp.impl.persistence.catalogue"}
)
@ComponentScan(basePackages = {"eu.nimble.utility.config"})
class UBLDBConfig {

    @Autowired
    private DataSourceCreator dataSourceCreator;

    @Bean(name = "ubldbDataSource")
    @ConfigurationProperties(prefix = "persistence.orm.ubl.hibernate.connection")
    public DataSource getDataSource() {
        return dataSourceCreator.createDatasource();
    }

    @Bean(name = "ubldbEmfBean")
    public LocalContainerEntityManagerFactoryBean ubldbEntityManagerFactoryBean(
            EntityManagerFactoryBuilder builder,
            @Qualifier("ubldbHibernateConfigs") Map hibernateConfigs,
            @Qualifier("ubldbDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emfBean = builder
                .dataSource(dataSource)
                .persistenceUnit(eu.nimble.utility.Configuration.UBL_PERSISTENCE_UNIT_NAME)
                .packages("eu.nimble.service.model.ubl")
                .build();

        Properties hibernateProperties = new Properties();
        hibernateProperties.putAll(hibernateConfigs);
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emfBean.setJpaVendorAdapter(vendorAdapter);
        emfBean.setJpaProperties(hibernateProperties);
        return emfBean;
    }

    @Bean(name = "ubldbEntityManagerFactory")
    public EntityManagerFactory ubldbEntityManagerFactory(@Qualifier("ubldbEmfBean") LocalContainerEntityManagerFactoryBean emfBean) {
        return emfBean.getObject();
    }

    @Bean(name = "ubldbTransactionManager")
    PlatformTransactionManager ubldbTransactionManager(
            @Qualifier("ubldbEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}