package eu.nimble.service.bp.config.interceptor;

import eu.nimble.service.bp.impl.util.ExecutionContext;
import eu.nimble.utility.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This interceptor injects the bearer token into the {@link ExecutionContext} for each Rest call
 *
 * Created by suat on 24-Jan-19.
 */
@Configuration
public class RestServiceInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    private ExecutionContext executionContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws AuthenticationException {

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        // validate token
        eu.nimble.service.bp.impl.util.HttpResponseUtil.validateToken(bearerToken);

        // set token to the execution context
        executionContext.setBearerToken(bearerToken);

        return true;
    }
}
