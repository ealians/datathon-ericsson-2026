package org.elis.ericsson.datathon.user_management;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Test for UC-S-003: createFirstUser endpoint.
 *
 * This test encodes the EXPECTED (correct) behavior of the endpoint.
 * It is designed to FAIL on the current unfixed code, proving the bug exists.
 *
 * Bug conditions tested:
 * 1. createFirstUser ignores client-provided credentials and uses hardcoded values
 * 2. createFirstUser accepts empty/invalid input without returning HTTP 400
 * 3. createFirstUser exposes password in the response body
 *
 * **Validates: Requirements 1.1, 1.2, 1.3**
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateFirstUserBugConditionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CREATE_FIRST_USER_URL = "/api/auth/createFirstUser";

    /**
     * Property 1.1: Client-provided credentials MUST be used.
     *
     * Invokes POST /api/auth/createFirstUser with custom DTO and asserts the created user
     * has the email provided by the client (not hardcoded "admin@elis.org").
     *
     * EXPECTED TO FAIL on unfixed code: the service ignores the DTO and always creates
     * a user with email "admin@elis.org".
     *
     * **Validates: Requirements 1.1**
     */
    @Test
    @DisplayName("Bug Condition 1.1: createFirstUser should use client-provided email, not hardcoded admin@elis.org")
    void createFirstUser_withCustomCredentials_shouldUseClientProvidedEmail() throws Exception {
        String requestBody = """
                {
                    "email": "custom@test.it",
                    "password": "MyP@ss123",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

        MvcResult result = mockMvc.perform(post(CREATE_FIRST_USER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);

        // The created user MUST have the email provided by the client
        String actualEmail = responseJson.get("email").asText();
        assertEquals("custom@test.it", actualEmail,
                "Bug confirmed: createFirstUser ignores client email and uses hardcoded 'admin@elis.org'. " +
                "Actual email was: " + actualEmail);
    }

    /**
     * Property 1.2: Invalid/empty input MUST produce HTTP 400.
     *
     * Invokes POST /api/auth/createFirstUser with empty body {} and asserts response
     * status is 400 (Bad Request) due to Jakarta Validation failure.
     *
     * EXPECTED TO FAIL on unfixed code: the service accepts any input (including empty)
     * because it doesn't use a DTO with validation annotations.
     *
     * **Validates: Requirements 1.2**
     */
    @Test
    @DisplayName("Bug Condition 1.2: createFirstUser with empty body should return HTTP 400")
    void createFirstUser_withEmptyBody_shouldReturn400() throws Exception {
        String emptyBody = "{}";

        mockMvc.perform(post(CREATE_FIRST_USER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyBody))
                .andExpect(status().isBadRequest());
    }

    /**
     * Property 1.3: Response MUST NOT expose password field.
     *
     * Invokes POST /api/auth/createFirstUser with valid data and asserts the response
     * JSON does NOT contain a "password" field.
     *
     * EXPECTED TO FAIL on unfixed code: the service returns the full UserProfile entity
     * including the BCrypt-encoded password.
     *
     * **Validates: Requirements 1.3**
     */
    @Test
    @DisplayName("Bug Condition 1.3: createFirstUser response should NOT contain password field")
    void createFirstUser_withValidData_shouldNotExposePassword() throws Exception {
        String requestBody = """
                {
                    "email": "custom@test.it",
                    "password": "MyP@ss123",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

        MvcResult result = mockMvc.perform(post(CREATE_FIRST_USER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);

        // The response MUST NOT contain the password field
        assertFalse(responseJson.has("password"),
                "Bug confirmed: createFirstUser response exposes the 'password' field. " +
                "Response contained: " + responseBody);
    }
}
