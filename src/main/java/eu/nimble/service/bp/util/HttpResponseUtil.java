package eu.nimble.service.bp.util;

import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.utility.exception.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

/**
 * Created by suat on 15-Jan-19.
 */
public class HttpResponseUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseUtil.class);

    /**
     * Returns a non-empty {@link ResponseEntity} if there is no associated user for the provided token. Otherwise
     * {@code null} is returned.
     *
     * @param token
     * @return
     */
    public static ResponseEntity checkToken(String token) {
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(token);
            if (!isValid) {
                String msg = String.format("No user exists for the given token : %s", token);
                return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog(msg, null, HttpStatus.UNAUTHORIZED, LogLevel.INFO);
            }
        } catch (IOException e) {
            String msg = String.format("Failed to check user authorization for token: %s", token);
            return eu.nimble.utility.HttpResponseUtil.createResponseEntityAndLog(msg, e, HttpStatus.INTERNAL_SERVER_ERROR, LogLevel.ERROR);
        }
        return null;
    }

    public static void validateToken(String token) throws AuthenticationException {
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(token);
            if (!isValid) {
                String msg = String.format("No user exists for the given token : %s", token);
                throw new AuthenticationException(msg);
            }
        } catch (IOException e) {
            throw new AuthenticationException(String.format("Failed to check user authorization for token: %s", token), e);
        }
    }
}