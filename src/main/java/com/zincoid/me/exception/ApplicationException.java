package com.zincoid.me.exception;

import com.zincoid.me.model.Error;
import lombok.Getter;

@Getter
public abstract class ApplicationException extends RuntimeException {

    private final com.zincoid.me.model.Error error;

    protected ApplicationException(Error error) {
        super(error.getMessage());
        this.error = error;
    }

    public int getCode() {
        return error.getCode();
    }
}
