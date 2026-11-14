package com.littlehorizons.erp.exception;

/**
 * Thrown when reading or writing a file fails (logs, invoices, config).
 * It wraps IOException so upper layers do not depend on java.io.
 */
public class FileOperationException extends ErpException {

    public FileOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
