package eu.nimble.service.bp.config;

import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Created by suat on 29-Jul-19.
 */
@Configuration
@Profile("!test")
@EnableFeignClients(basePackages = {"eu.nimble.common.rest"})
public class FeignConfig {
}