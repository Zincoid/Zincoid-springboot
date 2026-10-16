package com.zincoid.me.exception;

public class MaintenanceException extends ApplicationException {

    public MaintenanceException() {
        super(Error.SERVER_UNDER_MAINTENANCE);
    }
}
