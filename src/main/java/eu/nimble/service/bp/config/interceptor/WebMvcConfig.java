package eu.nimble.service.bp.config.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * With this configuration the {@link RestServiceInterceptor} is injected to populate {@link eu.nimble.utility.ExecutionContext}s
 * for each REST call.
 *
 * Created by suat on 24-Jan-19.
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private RestServiceInterceptor restServiceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(restServiceInterceptor);
    }
}
