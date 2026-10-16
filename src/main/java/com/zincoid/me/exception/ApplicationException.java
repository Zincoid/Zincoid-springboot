package com.zincoid.me.exception;

import lombok.Getter;

@Getter
public abstract class ApplicationException extends RuntimeException {

    private final Error error;

    protected ApplicationException(Error error) {
        super(error.getMessage());
        this.error = error;
    }

    public int getCode() {
        return error.getCode();
    }
}
