package com.zincoid.me.exception.handler;

import com.zincoid.me.exception.BusinessException;
import com.zincoid.me.exception.MaintenanceException;
import com.zincoid.me.exception.UnauthorizedException;
import com.zincoid.me.model.ApiResp;
import com.zincoid.me.model.Error;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Unhandled Exceptions ────────────────────────────

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResp<Void> handleUnknown(Exception e) {
        log.error("Unexpected error", e);
        return ApiResp.error(500, Error.INTERNAL_SERVER_ERROR);
    }

    // ── Application errors ──────────────────────────────

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResp<Void> handleBusiness(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ApiResp.error(e.getCode(), e.getError());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResp<Void> handleUnauthorized(UnauthorizedException e) {
        log.warn("Unauthorized exception: {}", e.getMessage());
        return ApiResp.error(401, e.getError());
    }

    @ExceptionHandler(MaintenanceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResp<Void> handleMaintenance(MaintenanceException e) {
        log.info("Request blocked during maintenance");
        return ApiResp.error(503, e.getError());
    }

    // ── Request validation ──────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResp<Void> handleNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> "Invalid " + error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("Validation failed: {}", message);
        return ApiResp.error(400, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResp<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: {}", e.getName());
        return ApiResp.error(400, Error.INVALID_PARAMETER);
    }

    // ── Client disconnect ───────────────────────────────

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException e) {
        log.debug("Client aborted connection: {}", e.getMessage());
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
        log.debug("Async request not usable (client disconnected): {}", e.getMessage());
    }
}
