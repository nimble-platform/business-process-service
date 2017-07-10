package eu.nimble.service.bp;

import com.netflix.hystrix.contrib.javanica.aop.aspectj.HystrixCommandAspect;
import org.camunda.bpm.engine.HistoryService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

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

  @Value("${nimble.cors_enabled}")
  private String corsEnabled;

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurerAdapter() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        if( corsEnabled.equals("true"))
          registry.addMapping("/*").allowedOrigins("*");
      }
    };
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
