package com.partywave.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a request contains invalid parameters or violates business rules.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;
    private final Object fieldValue;

    public InvalidRequestException(String message) {
        super(message);
        this.fieldName = null;
        this.fieldValue = null;
    }

    public InvalidRequestException(String message, String fieldName, Object fieldValue) {
        super(message);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}
