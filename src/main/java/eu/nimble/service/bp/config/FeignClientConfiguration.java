package eu.nimble.service.bp.config;

import feign.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfiguration {

    @Value("${nimble.feign-client.timeout}")
    private int feignClientTimeout;

    @Bean
    public Request.Options options(){
        return new Request.Options(feignClientTimeout,feignClientTimeout);
    }
}
