package com.zincoid.me.exception;

import com.zincoid.me.model.Error;

public class BusinessException extends ApplicationException {

    public BusinessException(Error error) {
        super(error);
    }
}
