package com.zincoid.me.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class ApiResp<T> {

    private int code;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;
    private String message;
    private T data;

    private ApiResp(int code, String error, String message, T data) {
        this.code = code;
        this.error = error;
        this.message = message;
        this.data = data;
    }

    /** Success response: with data. **/
    public static <T> ApiResp<T> success(T data) {
        return new ApiResp<>(200, null, "success", data);
    }

    /** Success response: no data. **/
    public static <T> ApiResp<T> success() {
        return new ApiResp<>(200, null, "success", null);
    }

    /** Error response: uses the provided code and error, with fallback message. */
    public static <T> ApiResp<T> error(int code, Error error) {
        return new ApiResp<>(code, error.name(), error.getMessage(), null);
    }

    /** Error response: message-only error (no stable code, e.g. DTO validation detail). */
    public static <T> ApiResp<T> error(int code, String message) {
        return new ApiResp<>(code, null, message, null);
    }
}
