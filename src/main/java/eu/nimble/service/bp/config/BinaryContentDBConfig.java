package eu.nimble.service.bp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties
@PropertySource("classpath:bootstrap.yml")
@EnableJpaRepositories(
        entityManagerFactoryRef = "binarycontentdbEmfBean",
        transactionManagerRef = "binarycontentdbTransactionManager",
        basePackages = {"eu.nimble.utility.persistence.binary"}
)
@ComponentScan(basePackages = {"eu.nimble.utility.config"})
public class BinaryContentDBConfig {

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Bean(name = "binarycontentdbDataSource")
    public DataSource binaryContentDbdataSource() {
        return dataSourceFactory.createDatasource("binarycontentdb");
    }
}