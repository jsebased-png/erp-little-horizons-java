package com.littlehorizons.erp.exception;

/**
 * Thrown when a SQL or JDBC operation fails.
 * It wraps SQLException so upper layers never depend on java.sql.
 */
public class DatabaseException extends ErpException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
