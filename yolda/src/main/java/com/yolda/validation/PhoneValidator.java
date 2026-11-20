package com.yolda.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null || phone.isEmpty()) {
            return true;
        }
        return phone.matches("^\\+994\\d{9}$");
    }
}