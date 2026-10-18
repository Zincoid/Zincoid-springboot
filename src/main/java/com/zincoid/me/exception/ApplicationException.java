package com.zincoid.me.exception;

import com.zincoid.me.model.enums.dict.Error;
import lombok.Getter;

@Getter
public abstract class ApplicationException extends RuntimeException {

    private final Error error;

    protected ApplicationException(Error error) {
        super(error.getMessage());
        this.error = error;
    }
}
