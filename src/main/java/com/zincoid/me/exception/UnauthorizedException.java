package com.zincoid.me.exception;

import com.zincoid.me.model.enums.dict.Error;

public class UnauthorizedException extends ApplicationException {

    public UnauthorizedException(Error error) {
        super(error);
    }
}
