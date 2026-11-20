package com.yolda.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PhoneValidatorTest {

    @Test
    void shouldAcceptValidPhone() {

        PhoneValidator validator = new PhoneValidator();

        boolean result = validator.isValid("+994501234567", null);

        assertTrue(result);
    }

    @Test
    void shouldRejectInvalidPhone() {

        PhoneValidator validator = new PhoneValidator();

        boolean result = validator.isValid("0501234567", null);

        assertFalse(result);
    }

    @Test
    void shouldAcceptNullPhone() {

        PhoneValidator validator = new PhoneValidator();

        boolean result = validator.isValid(null, null);

        assertTrue(result);
    }

    @Test
    void shouldAcceptEmptyPhone() {

        PhoneValidator validator = new PhoneValidator();

        boolean result = validator.isValid("", null);

        assertTrue(result);
    }
}