package com.zincoid.me.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidJsonObjectValidator.class)
public @interface ValidJsonObject {

    String message() default "Must be a valid JSON object";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
