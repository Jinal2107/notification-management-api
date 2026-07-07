package com.notification.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotRepeatedWordsValidator implements ConstraintValidator<NotRepeatedWords, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        // Reject if any word is repeated more than 3 consecutive times
        return !ValidationUtil.hasConsecutiveRepeatedWords(value, 3);
    }
}
