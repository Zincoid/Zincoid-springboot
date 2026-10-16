package com.zincoid.me.exception;

public class UnauthorizedException extends ApplicationException {

    public UnauthorizedException(Error error) {
        super(error);
    }
}
