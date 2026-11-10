package com.yolda.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = YearValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidYear {
    String message() default "İl 1950 ilə cari il arasında olmalıdır";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}