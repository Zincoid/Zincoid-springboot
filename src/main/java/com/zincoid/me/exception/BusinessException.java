package com.zincoid.me.exception;

public class BusinessException extends ApplicationException {

    public BusinessException(Error error) {
        super(error);
    }
}
