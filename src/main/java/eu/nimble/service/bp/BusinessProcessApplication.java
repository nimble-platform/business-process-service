package eu.nimble.service.bp;

import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import eu.nimble.service.bp.impl.util.persistence.HibernateUtilityRef;
import eu.nimble.utility.HibernateUtility;
import org.apache.ibatis.session.SqlSession;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngines;
import org.camunda.bpm.engine.impl.ProcessEngineImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@SpringBootApplication
@EnableHystrix
@Configuration
@EnableCircuitBreaker
@EnableAutoConfiguration
@EnableEurekaClient
@EnableFeignClients(basePackages = {"eu.nimble.common.rest"})
@RestController
@EnableSwagger2
@EnableProcessApplication
@ComponentScan(basePackages = "eu")
public class BusinessProcessApplication {

  public static void main(final String... args) throws Exception {
    SpringApplication.run(BusinessProcessApplication.class, args);
  }

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Bean
  public HystrixCommandAspect hystrixAspect() {
    return new HystrixCommandAspect();
  }

  @EventListener({ContextRefreshedEvent.class})
  void contextRefreshedEvent() {
    ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
    ProcessEngineConfigurationImpl processEngineConfiguration = ((ProcessEngineImpl)processEngine).getProcessEngineConfiguration();
    SqlSession session = processEngineConfiguration.getDbSqlSessionFactory().getSqlSessionFactory().openSession();
    Connection connection = session.getConnection();

    try {
      Statement jdbcStatement = connection.createStatement();
      jdbcStatement.execute("ALTER TABLE ACT_HI_VARINST ALTER COLUMN TEXT_ TYPE text");
      jdbcStatement.execute("ALTER TABLE ACT_RU_VARIABLE ALTER COLUMN TEXT_ TYPE text");
      jdbcStatement.execute("ALTER TABLE ACT_HI_DETAIL ALTER COLUMN TEXT_ TYPE text");

      jdbcStatement.close();
      logger.info("Updated the column type TEXT_ column of ACT_HI_VARINST, ACT_RU_VARIABLE, ACT_HI_DETAIL tables to TEXT");
    } catch (SQLException e) {
      logger.error("Failed to alter table for changing the column type to CLOB", e);
    } finally {
      try {
        session.close();
        connection.close();
      } catch (Exception e) {
        logger.warn("Failed to close session/connection", e);
      }
    }

    HibernateUtility.getInstance(eu.nimble.utility.Configuration.UBL_PERSISTENCE_UNIT_NAME);
    HibernateUtilityRef.getInstance("bp-data-model");
  }
}
