package com.littlehorizons.erp.exception;

/**
 * Thrown when user input or an object state is invalid
 * (empty text, grade out of 0.0-5.0, bad date, etc.).
 */
public class ValidationException extends ErpException {

    public ValidationException(String message) {
        super(message);
    }
}
