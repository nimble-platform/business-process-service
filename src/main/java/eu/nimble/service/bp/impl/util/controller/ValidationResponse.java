package eu.nimble.service.bp.impl.util.controller;

import org.springframework.http.ResponseEntity;

/**
 * Created by suat on 07-Jun-18.
 */
public class ValidationResponse {
    private Object validatedObject;
    private ResponseEntity invalidResponse;

    public Object getValidatedObject() {
        return validatedObject;
    }

    public void setValidatedObject(Object validatedObject) {
        this.validatedObject = validatedObject;
    }

    public ResponseEntity getInvalidResponse() {
        return invalidResponse;
    }

    public void setInvalidResponse(ResponseEntity invalidResponse) {
        this.invalidResponse = invalidResponse;
    }
}

