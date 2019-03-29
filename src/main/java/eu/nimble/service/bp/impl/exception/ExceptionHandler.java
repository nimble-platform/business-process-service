package eu.nimble.service.bp.impl.exception;

import eu.nimble.utility.exception.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.ws.rs.BadRequestException;

/**
 * Created by suat on 19-Mar-19.
 */
@ControllerAdvice
public class ExceptionHandler {
    @org.springframework.web.bind.annotation.ExceptionHandler(AuthenticationException.class)
    public @ResponseBody
    ResponseEntity handleBadRequests(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(BadRequestException.class)
    public @ResponseBody
    ResponseEntity handleBadRequests(BadRequestException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(RuntimeException.class)
    public @ResponseBody
    ResponseEntity handleUnexpectedExceptions(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
