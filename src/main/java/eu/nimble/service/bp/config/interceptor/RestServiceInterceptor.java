package eu.nimble.service.bp.config.interceptor;

import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.exception.AuthenticationException;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.service.bp.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.validation.IValidationUtil;
import eu.nimble.utility.validation.NimbleRole;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * This interceptor injects the bearer token into the {@link ExecutionContext} for each Rest call
 *
 * Created by suat on 24-Jan-19.
 */
@Configuration
public class RestServiceInterceptor extends HandlerInterceptorAdapter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    IValidationUtil iValidationUtil;

    @Autowired
    private ExecutionContext executionContext;

    private final String swaggerPath = "swagger-resources";
    private final String apiDocsPath = "api-docs";
    private final String CLAIMS_FIELD_REALM_ACCESS = "realm_access";
    private final String CLAIMS_FIELD_ROLES = "roles";
    private final String CLAIMS_FIELD_EMAIL = "email";
    // headers
    private final String ORIGINAL_AUTHORIZATION_HEADER = "originalAuthorizationHeader";
    private final String CLIENT_FEDERATION_ID_HEADER = "clientFederationIdHeader";

    private final int MEGABYTE = 1024*1024;

    private static Set<String> excludedEndpoints = new HashSet<>();
    static {
        excludedEndpoints.add("/swagger-resources");
        excludedEndpoints.add("/api-docs");
        // error point is called by spring if the execution of the request is not successful
        excludedEndpoints.add("/error");
        // excluding these as they are required while getting product details when the user is not logged in
        excludedEndpoints.add("/ratingsSummary");
    }

    @Override
    public boolean preHandle (HttpServletRequest request, HttpServletResponse response, Object handler) {
        // log JVM memory stats
        logJVMMemoryStats(request.getRequestURI(),request.getMethod());

        // save the time as an Http attribute
        request.setAttribute("startTime", System.currentTimeMillis());

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        String originalBearerToken = request.getHeader(ORIGINAL_AUTHORIZATION_HEADER);

        Claims claims = null;
        try {
            claims = iValidationUtil.validateToken(bearerToken);
        } catch (Exception e) {
            // do not throw an exception if the endpoint is among the excluded ones from authentication
            if(!excludedEndpoints.stream().anyMatch(endpoint -> request.getServletPath().matches(endpoint))) {
                executionContext.setUserRoles(Collections.singletonList(NimbleRole.COMPANY_ADMIN.getName()));
                return true;
            } else {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_NO_USER_FOR_TOKEN.toString(), Arrays.asList(bearerToken),e);
            }
        }

        // set token to the execution context
        executionContext.setBearerToken(bearerToken);
        // set language id
        executionContext.setLanguageId(request.getLocale().getLanguage());

        // if originalBearerToken exists,i.e it's a federated call, to check user roles properly,
        // we use Claims of the originalBearerToken
        if(originalBearerToken != null){
            executionContext.setOriginalBearerToken(originalBearerToken);
            executionContext.setClientFederationId(request.getHeader(CLIENT_FEDERATION_ID_HEADER));
            try {
                claims = iValidationUtil.getClaims(originalBearerToken);
            } catch (AuthenticationException e) {
                logger.error("RestServiceInterceptor.preHandle failed ",e);
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_NO_USER_FOR_TOKEN.toString(), Arrays.asList(bearerToken),e);
            }
        }

        // set user email and available roles to the execution context
        if(claims != null){
            String email = (String) claims.get(CLAIMS_FIELD_EMAIL);
            LinkedHashMap realmAccess = (LinkedHashMap) claims.get(CLAIMS_FIELD_REALM_ACCESS);
            List<String> roles = (List<String>) realmAccess.get(CLAIMS_FIELD_ROLES);

            executionContext.setUserEmail(email);
            executionContext.setUserRoles(roles);

            // to append user email to the exception logs, we do not clear MDC since ExceptionHandler is invoked after afterCompletion method.
            MDC.put("userEmail",email);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // calculate and log the execution time for the request
        long endTime = System.currentTimeMillis();

        long startTime = (Long)request.getAttribute("startTime");

        long executionTime = endTime - startTime;
        if(executionContext.getRequestLog() != null){
            logger.info("Duration for '{}' is {} millisecond",executionContext.getRequestLog(),executionTime);
        }
    }

    private void logJVMMemoryStats(String uri, String method){
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / MEGABYTE;
        long freeMemory = runtime.freeMemory() / MEGABYTE;

        logger.info("Incoming request to {} {} : Total Memory: {} MB, Used Memory: {} MB, Free Memory: {} MB",method,uri,totalMemory,totalMemory - freeMemory,freeMemory);
    }
}
