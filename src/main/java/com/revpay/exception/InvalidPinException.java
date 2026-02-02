package com.revpay.exception;

public class InvalidPinException extends RuntimeException {
    public InvalidPinException() {
        super();
    }

    public InvalidPinException(String message) {
        super(message);
    }
}
