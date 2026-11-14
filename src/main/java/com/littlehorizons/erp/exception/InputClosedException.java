package com.littlehorizons.erp.exception;

/**
 * Thrown when the keyboard input ends (Ctrl+D / Ctrl+Z or a closed pipe).
 * It extends RuntimeException, NOT ErpException, on purpose: menus catch ErpException to show a message and keep
 * running, but when there is no more input the only safe thing to do is to leave the program.
 */
public class InputClosedException extends RuntimeException {

    public InputClosedException() {
        super("Input stream closed");
    }
}
