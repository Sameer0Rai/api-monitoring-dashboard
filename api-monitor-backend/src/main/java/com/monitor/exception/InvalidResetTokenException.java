package com.monitor.exception;

public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("This reset link is invalid or has expired");
    }
}
