package eu.nimble.service.bp.util;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This a Http-request scoped bean which is instantiated for each REST call. It can be used to pass REST call-specific
 * information to inner-level code execution. For example, the authorization token might passed to inner-level code
 * which can be used to check user roles or execute subsequent REST-calls respecting to the authorization level of the user.
 *
 * Created by suat on 24-Jan-19.
 */
@Component
@RequestScope
public class ExecutionContext {
    private String bearerToken;

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getBearerToken() {
        return this.bearerToken;
    }
}
