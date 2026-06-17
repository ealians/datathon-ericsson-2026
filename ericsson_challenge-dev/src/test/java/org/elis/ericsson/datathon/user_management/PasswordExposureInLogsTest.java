package org.elis.ericsson.datathon.user_management;

import org.elis.ericsson.datathon.user_management.model.dto.LoginDto;
import org.elis.ericsson.datathon.user_management.model.dto.request.SignUpRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-S-002: Password exposure in login/signup DTO toString() methods.
 *
 * These tests verify that sensitive credentials are NOT leaked when DTOs
 * are converted to string (e.g., via logging).
 *
 * CWE-532: Insertion of Sensitive Information into Log File
 *
 * Tests are designed to FAIL on unfixed code and PASS after the fix.
 */
class PasswordExposureInLogsTest {

    private static final String TEST_PASSWORD = "SuperSecret123!";

    @Test
    @DisplayName("UC-S-002 §1.3: LoginDto.toString() must NOT contain the actual password")
    void loginDto_toString_mustNotExposePassword() {
        LoginDto dto = new LoginDto();
        dto.setEmail("user@example.com");
        dto.setPassword(TEST_PASSWORD);

        String result = dto.toString();

        assertFalse(result.contains(TEST_PASSWORD),
                "LoginDto.toString() exposes password in plain text: " + result);
    }

    @Test
    @DisplayName("UC-S-002 §1.4: SignUpRequestDto.toString() must NOT contain the actual password")
    void signUpRequestDto_toString_mustNotExposePassword() {
        SignUpRequestDto dto = new SignUpRequestDto();
        dto.setEmail("user@example.com");
        dto.setPassword(TEST_PASSWORD);
        dto.setFirstName("John");
        dto.setLastName("Doe");

        String result = dto.toString();

        assertFalse(result.contains(TEST_PASSWORD),
                "SignUpRequestDto.toString() exposes password in plain text: " + result);
    }

    @Test
    @DisplayName("UC-S-002 §3.1: LoginDto.getPassword() must still return the real password")
    void loginDto_getPassword_stillReturnsRealValue() {
        LoginDto dto = new LoginDto();
        dto.setPassword(TEST_PASSWORD);

        assertEquals(TEST_PASSWORD, dto.getPassword(),
                "LoginDto.getPassword() must return the actual password for authentication");
    }

    @Test
    @DisplayName("UC-S-002 §3.2: SignUpRequestDto.getPassword() must still return the real password")
    void signUpRequestDto_getPassword_stillReturnsRealValue() {
        SignUpRequestDto dto = new SignUpRequestDto();
        dto.setPassword(TEST_PASSWORD);

        assertEquals(TEST_PASSWORD, dto.getPassword(),
                "SignUpRequestDto.getPassword() must return the actual password for encoding");
    }
}
