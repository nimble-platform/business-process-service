package eu.nimble.service.bp;

import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import org.apache.ibatis.session.SqlSession;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.impl.ProcessEngineImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
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
@EnableFeignClients
@RestController
@EnableSwagger2
@EnableProcessApplication
@ComponentScan(basePackages = "eu.nimble.service.bp")
public class BusinessProcessApplication {

  boolean contextClosed;

  public static void main(final String... args) throws Exception {
    SpringApplication.run(BusinessProcessApplication.class, args);
  }

  @Bean
  public HystrixCommandAspect hystrixAspect() {
    return new HystrixCommandAspect();
  }

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private HistoryService historyService;

  @Autowired
  private ConfigurableApplicationContext context;

  @EventListener({ContextRefreshedEvent.class})
  void contextRefreshedEvent() {
    ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
    ProcessEngineConfigurationImpl processEngineConfiguration = ((ProcessEngineImpl)processEngine).getProcessEngineConfiguration();
    SqlSession session = processEngineConfiguration.getDbSqlSessionFactory().getSqlSessionFactory().openSession();
    Connection connection = session.getConnection();

    try {
      Statement jdbcStatement = connection.createStatement();
      jdbcStatement.execute("ALTER TABLE ACT_HI_VARINST ALTER TEXT_ CLOB");
      jdbcStatement.execute("ALTER TABLE ACT_RU_VARIABLE ALTER TEXT_ CLOB");
      jdbcStatement.execute("ALTER TABLE ACT_HI_DETAIL ALTER TEXT_ CLOB");
      jdbcStatement.close();
      logger.info("Updated the column type TEXT_ column of ACT_HI_VARINST, ACT_RU_VARIABLE, ACT_HI_DETAIL tables to CLOB");
    } catch (SQLException e) {
      logger.error("Failed to alter table for changing the column type to CLOB", e);
    }
  }

//  @Autowired
//  private Showcase showcase;

//  @Value("${eu.nimble.service.bp.BusinessProcessApplication.exitWhenFinished:false}")
//  private boolean exitWhenFinished;
//
//  @EventListener
//  public void contextClosed(ContextClosedEvent event) {
//    logger.info("context closed!");
//    contextClosed = true;
//  }

//  @Scheduled(fixedDelay = 1500L)
//  public void exitApplicationWhenProcessIsFinished() {
//    String processInstanceId = showcase.getProcessInstanceId();
//
//    if (processInstanceId == null) {
//      logger.info("processInstance not yet started!");
//      return;
//    }
//
//    if (isProcessInstanceFinished()) {
//      logger.info("processinstance ended!");
//
//      if (exitWhenFinished) {
//        SpringApplication.exit(context, () -> 0);
//      }
//      return;
//    }
//    logger.info("processInstance not yet ended!");
//  }
//
//  public boolean isProcessInstanceFinished() {
//    final HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
//        .processInstanceId(showcase.getProcessInstanceId()).singleResult();
//
//    return historicProcessInstance != null && historicProcessInstance.getEndTime() != null;
//
//  }

}
