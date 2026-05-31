package com.labmentix.speechmatrix.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.List;

public class PasswordConstraintValidator
        implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext ctx) {
        if (password == null || password.isBlank()) {
            return false; // @NotBlank handles the blank message separately
        }

        List<String> violations = new ArrayList<>();

        if (password.length() < 8)
            violations.add("at least 8 characters");

        if (!password.matches(".*[A-Z].*"))
            violations.add("one uppercase letter");

        if (!password.matches(".*[a-z].*"))
            violations.add("one lowercase letter");

        if (!password.matches(".*\\d.*"))
            violations.add("one digit");

        if (violations.isEmpty()) return true;

        // Build a helpful message listing exactly what is missing
        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(
                "Password must contain: " + String.join(", ", violations))
           .addConstraintViolation();

        return false;
    }
}
