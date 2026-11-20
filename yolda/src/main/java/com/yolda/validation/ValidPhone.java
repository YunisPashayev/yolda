package com.yolda.validation;
import jakarta.validation.Constraint;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.validation.Payload;


@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {
    String message() default "Telefon +994XXXXXXXXX formatında olmalıdır";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}