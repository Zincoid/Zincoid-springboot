package com.zincoid.me.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidJsonObjectValidator implements ConstraintValidator<ValidJsonObject, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        try {
            return MAPPER.readTree(value).isObject();
        } catch (Exception e) {
            return false;
        }
    }
}
