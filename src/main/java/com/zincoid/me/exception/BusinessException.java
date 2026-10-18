package com.zincoid.me.exception;

import com.zincoid.me.model.enums.dict.Error;

public class BusinessException extends ApplicationException {

    public BusinessException(Error error) {
        super(error);
    }
}
