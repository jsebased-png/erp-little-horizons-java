package com.littlehorizons.erp.exception;

/**
 * Thrown when a business rule is broken
 * (club is full, resource already loaned, subject not in student's grade).
 */
public class BusinessRuleException extends ErpException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
