package eu.nimble.service.bp.util;

import eu.nimble.service.bp.util.spring.SpringBridge;
import eu.nimble.utility.exception.AuthenticationException;
import eu.nimble.utility.exception.NimbleException;
import eu.nimble.utility.exception.NimbleExceptionMessageCode;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by suat on 15-Jan-19.
 */
public class HttpResponseUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseUtil.class);

    /**
     * Throws a {@link NimbleException} if there is no associated user for the provided token.
     *
     * @param token
     * @return
     */
    public static void checkToken(String token) throws NimbleException{
        try {
            // check token
            boolean isValid = SpringBridge.getInstance().getiIdentityClientTyped().getUserInfo(token);
            if (!isValid) {
                throw new NimbleException(NimbleExceptionMessageCode.UNAUTHORIZED_NO_USER_FOR_TOKEN.toString(), Arrays.asList(token));
            }
        } catch (IOException e) {
            throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_FAILED_TO_CHECK_TOKEN.toString(), Arrays.asList(token),e);
        }
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

    public static String extractBodyFromFeignClientResponse(Response feignResponse) throws IOException {
        try {
            String responseBody = IOUtils.toString(feignResponse.body().asInputStream());
            if(feignResponse.status() == HttpStatus.OK.value()) {
                return responseBody;

            } else {
                String msg = String.format("Unexpected response status: %d.\n%s", feignResponse.status(), responseBody);
                logger.error(msg);
                throw new IOException(msg);
            }

        } catch (IOException e) {
            String msg = String.format("Failed to extract response body from feign response");
            logger.error(msg, e);
            throw e;
        }
    }
}