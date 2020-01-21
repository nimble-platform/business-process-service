package eu.nimble.service.bp.config.interceptor;

import eu.nimble.service.bp.util.ExecutionContext;
import eu.nimble.utility.exception.AuthenticationException;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * This interceptor injects the bearer token into the {@link ExecutionContext} for each Rest call
 *
 * Created by suat on 24-Jan-19.
 */
@Configuration
public class RestServiceInterceptor extends HandlerInterceptorAdapter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ExecutionContext executionContext;

    private final String swaggerPath = "swagger-resources";
    private final String apiDocsPath = "api-docs";
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        // do not validate the token for swagger operations
        if(bearerToken != null && !(request.getServletPath().contains(swaggerPath) || request.getServletPath().contains(apiDocsPath))){
            // validate token
            try {
                eu.nimble.service.bp.util.HttpResponseUtil.validateToken(bearerToken);
            } catch (AuthenticationException e) {
                logger.error("RestServiceInterceptor.preHandle failed ",e);
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_NO_USER_FOR_TOKEN.toString(), Arrays.asList(bearerToken),e);
            }
        }

        // set token to the execution context
        executionContext.setBearerToken(bearerToken);

        return true;
    }
}
