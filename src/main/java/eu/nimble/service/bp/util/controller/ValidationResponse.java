package eu.nimble.service.bp.util.controller;

import org.springframework.http.ResponseEntity;

/**
 * Created by suat on 07-Jun-18.
 */
public class ValidationResponse {
    private Object validatedObject;

    public Object getValidatedObject() {
        return validatedObject;
    }

    public void setValidatedObject(Object validatedObject) {
        this.validatedObject = validatedObject;
    }
}

