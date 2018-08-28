package eu.nimble.service.bp.impl.util.controller;

import eu.nimble.service.bp.impl.ContractController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Created by suat on 16-May-18.
 */
public class HttpResponseUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseUtil.class);

    public static ResponseEntity createResponseEntityAndLog(String msg, HttpStatus httpStatus) {
        return createResponseEntityAndLog(msg, null, httpStatus, LogLevel.WARN);
    }

    public static ResponseEntity createResponseEntityAndLog(String msg, Exception e, HttpStatus httpStatus) {
        if (httpStatus == HttpStatus.BAD_REQUEST) {
            return createResponseEntityAndLog(msg, e, httpStatus, LogLevel.WARN);
        } else {
            return createResponseEntityAndLog(msg, e, httpStatus, LogLevel.ERROR);
        }
    }


    public static ResponseEntity createResponseEntityAndLog(String msg, Exception e, HttpStatus httpStatus, LogLevel logLevel) {
        if(logLevel == null || logLevel == LogLevel.INFO) {
            if(e != null) {
                logger.info(msg, e);
            } else {
                logger.info(msg);
            }
        } else if (logLevel == LogLevel.WARN) {
            if (e != null) {
                logger.warn(msg, e);
            } else {
                logger.warn(msg);
            }
        } else if (logLevel == LogLevel.ERROR) {
            if (e != null) {
                logger.error(msg, e);
            } else {
                logger.error(msg);
            }
        }
        return ResponseEntity.status(httpStatus).body(msg);
    }
}
