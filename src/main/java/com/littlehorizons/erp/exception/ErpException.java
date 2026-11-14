package com.littlehorizons.erp.exception;

/**
 * Base class of every custom exception in the ERP.
 * It exists so the view layer can catch one single type for any expected
 * application error. It is unchecked so it works inside lambdas and streams.
 */
public class ErpException extends RuntimeException {

    public ErpException(String message) {
        super(message);
    }

    public ErpException(String message, Throwable cause) {
        super(message, cause);
    }
}
