package com.zincoid.me.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zincoid.me.model.enums.dict.Error;
import lombok.Data;

@Data
public class ApiResponse<T> {

    private int code;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;
    private String message;
    private T data;

    private ApiResponse(int code, String error, String message, T data) {
        this.code = code;
        this.error = error;
        this.message = message;
        this.data = data;
    }

    /** Success response: with data. **/
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, null, "success", data);
    }

    /** Success response: no data. **/
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, null, "success", null);
    }

    /** Error response: uses the provided error, with fallback message. */
    public static <T> ApiResponse<T> error(Error error) {
        return new ApiResponse<>(error.getCode(), error.name(), error.getMessage(), null);
    }

    /** Error response: message-only error (no stable code, e.g. DTO validation detail). */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, null, message, null);
    }
}
