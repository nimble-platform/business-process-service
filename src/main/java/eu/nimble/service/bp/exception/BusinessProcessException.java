package eu.nimble.service.bp.exception;

/**
 * A generic exception to wrap internal exceptions with a context-aware message
 * Created by suat on 20-Mar-19.
 */
public class BusinessProcessException extends RuntimeException {
    public BusinessProcessException(String message) {
        super(message);
    }

    public BusinessProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
