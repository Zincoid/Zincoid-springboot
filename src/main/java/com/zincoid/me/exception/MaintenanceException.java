package com.zincoid.me.exception;

import com.zincoid.me.model.enums.dict.Error;

public class MaintenanceException extends ApplicationException {

    public MaintenanceException() {
        super(Error.SERVER_UNDER_MAINTENANCE);
    }
}
