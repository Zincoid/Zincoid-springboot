package com.zincoid.me.exception;

public class MaintenanceException extends RuntimeException {

    public MaintenanceException() {
        super("Server is under maintenance");
    }
}
