package com.littlehorizons.erp.exception;

/**
 * Thrown when config.properties is missing, unreadable or incomplete.
 */
public class ConfigurationException extends ErpException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
