package com.lib.demo.exception;

public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message) {
        this("BIZ_ERROR", message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
