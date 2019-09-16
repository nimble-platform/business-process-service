package eu.nimble.service.bp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@EnableConfigurationProperties
@PropertySource("classpath:bootstrap.yml")
@ComponentScan(basePackages = {"eu.nimble.service.bp.config"})
public class BusinessProcessDBConfig {

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Bean(name = "bpdbDataSource")
    public DataSource getDataSource() {
        return dataSourceFactory.createDatasource("bpdb");
    }

    @Bean(name = "bpdbEmfBean")
    @Primary
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

    @Bean(name = "bpdbLazyDisabledEmfBean")
    public LocalContainerEntityManagerFactoryBean bpdbLazyDisabledEntityManagerFactoryBean(
            EntityManagerFactoryBuilder builder,
            @Qualifier("bpdbHibernateConfigs") Map hibernateConfigs,
            @Qualifier("bpdbDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emfBean = builder
                .dataSource(dataSource)
                .persistenceUnit("bp-data-model")
                .build();

        Properties hibernateProperties = new Properties();
        hibernateProperties.putAll(hibernateConfigs);
        // enable hibernate.enable_lazy_load_no_trans property
        hibernateProperties.put("hibernate.enable_lazy_load_no_trans",true);
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emfBean.setJpaVendorAdapter(vendorAdapter);
        emfBean.setJpaProperties(hibernateProperties);
        return emfBean;
    }

    @Bean(name = "bpdbEntityManagerFactory")
    public EntityManagerFactory bpdbEntityManagerFactory(@Qualifier("bpdbEmfBean") LocalContainerEntityManagerFactoryBean emfBean) {
        return emfBean.getObject();
    }

    @Bean(name = "bpdbLazyDisabledEntityManagerFactory")
    public EntityManagerFactory bpdbLazyEnabledEntityManagerFactory(@Qualifier("bpdbLazyDisabledEmfBean") LocalContainerEntityManagerFactoryBean emfBean) {
        return emfBean.getObject();
    }
}