package com.littlehorizons.erp.exception;

/**
 * Thrown when a record searched by id or code does not exist.
 */
public class NotFoundException extends ErpException {

    public NotFoundException(String message) {
        super(message);
    }
}
