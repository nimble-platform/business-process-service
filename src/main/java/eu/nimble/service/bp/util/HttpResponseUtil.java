package eu.nimble.service.bp.util;

import feign.Response;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;

/**
 * Created by suat on 15-Jan-19.
 */
public class HttpResponseUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseUtil.class);

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