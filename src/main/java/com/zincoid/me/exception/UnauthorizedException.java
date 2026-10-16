package com.zincoid.me.exception;

import com.zincoid.me.model.Error;

public class UnauthorizedException extends ApplicationException {

    public UnauthorizedException(Error error) {
        super(error);
    }
}
