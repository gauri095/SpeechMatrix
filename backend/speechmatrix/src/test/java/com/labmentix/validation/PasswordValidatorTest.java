package com.labmentix.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import com.sttapp.dto.AuthDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("@ValidPassword constraint tests")
class PasswordValidatorTest {

    static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest(name = "INVALID: \"{0}\"")
    @ValueSource(strings = {
        "short1A",          // only 7 chars
        "alllowercase1",    // no uppercase
        "ALLUPPERCASE1",    // no lowercase
        "NoDigitsHere",     // no digit
        "ab",               // way too short
    })
    @DisplayName("Weak passwords are rejected")
    void weakPasswordsRejected(String password) {
        AuthDto.RegisterRequest req = buildRequest(password);
        Set<ConstraintViolation<AuthDto.RegisterRequest>> violations =
            validator.validate(req);

        assertThat(violations).isNotEmpty();
        boolean hasPasswordViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertThat(hasPasswordViolation).isTrue();
    }

    @ParameterizedTest(name = "VALID: \"{0}\"")
    @ValueSource(strings = {
        "SecurePass1",
        "MyP@ssw0rd",
        "Java2024Dev",
        "Spring3oot!",
        "Abcdefg1",
    })
    @DisplayName("Strong passwords are accepted")
    void strongPasswordsAccepted(String password) {
        AuthDto.RegisterRequest req = buildRequest(password);
        Set<ConstraintViolation<AuthDto.RegisterRequest>> violations =
            validator.validate(req);

        boolean hasPasswordViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        assertThat(hasPasswordViolation).isFalse();
    }

    private AuthDto.RegisterRequest buildRequest(String password) {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Test User");
        req.setEmail("test@sttapp.com");
        req.setPassword(password);
        return req;
    }
}
