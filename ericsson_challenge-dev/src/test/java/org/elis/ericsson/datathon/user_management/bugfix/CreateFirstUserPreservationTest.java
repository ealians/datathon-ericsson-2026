package org.elis.ericsson.datathon.user_management.bugfix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.ServletException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preservation Property Tests for createFirstUser bugfix.
 *
 * <p><b>Validates: Requirements AC-4, AC-7, AC-8, AC-9</b></p>
 *
 * <p>These tests verify that existing auth and user behavior is preserved
 * after the bugfix (DI injection + @JsonIgnore annotations).</p>
 *
 * <p>Observation-first methodology: tests capture baseline behavior on the
 * current (fixed) code and serve as a regression guard.</p>
 *
 * <p>Properties tested:</p>
 * <ul>
 *   <li>P1: For all valid login attempts after user creation → HTTP 200 with JWT</li>
 *   <li>P2: For all createFirstUser calls on non-empty DB → exception is thrown (idempotency guard)</li>
 *   <li>P3: For all serialized UserProfile responses → roles field is present and non-empty</li>
 *   <li>P4: BCrypt encoding/matching works end-to-end (password encoded at creation, verified at login)</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CreateFirstUserPreservationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Setup: creates the first user so subsequent tests can verify login and idempotency.
     * Also verifies AC-8 (roles) from the initial response.
     */
    @Test
    @Order(1)
    @DisplayName("Setup + AC-8: createFirstUser on empty DB succeeds with roles ROLE_ADMIN and ROLE_USER")
    void setup_createFirstUser_succeeds_and_containsRoles() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/createFirstUser"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).isNotEmpty();

        JsonNode json = objectMapper.readTree(responseBody);
        assertThat(json.get("email").asText()).isEqualTo("admin@elis.org");

        // AC-8: roles field must be present and contain ROLE_ADMIN and ROLE_USER
        assertThat(json.has("roles")).isTrue();
        JsonNode rolesNode = json.get("roles");
        assertThat(rolesNode.isArray()).isTrue();
        assertThat(rolesNode.size()).isGreaterThanOrEqualTo(2);

        boolean hasAdmin = false;
        boolean hasUser = false;
        for (JsonNode roleNode : rolesNode) {
            String roleName = roleNode.get("name").asText();
            if ("ROLE_ADMIN".equals(roleName)) hasAdmin = true;
            if ("ROLE_USER".equals(roleName)) hasUser = true;
        }

        assertThat(hasAdmin)
                .as("User created by createFirstUser must have ROLE_ADMIN")
                .isTrue();
        assertThat(hasUser)
                .as("User created by createFirstUser must have ROLE_USER")
                .isTrue();
    }

    /**
     * Property 1 (AC-9): For all valid login attempts after user creation,
     * login returns HTTP 200 with a JWT token.
     */
    @Test
    @Order(2)
    @DisplayName("AC-9: Valid login after createFirstUser returns HTTP 200 with JWT")
    void property_loginAfterCreateFirstUser_returnsJwt() throws Exception {
        String loginJson = """
                {
                    "email": "admin@elis.org",
                    "password": "password"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).isNotEmpty();

        JsonNode json = objectMapper.readTree(responseBody);

        // AuthResponseDTO must contain a non-null, non-empty token (JWT)
        assertThat(json.has("token")).isTrue();
        assertThat(json.get("token").asText()).isNotBlank();

        // AuthResponseDTO must contain user identification
        assertThat(json.has("email")).isTrue();
        assertThat(json.get("email").asText()).isEqualTo("admin@elis.org");
    }

    /**
     * Property 2 (AC-4): For all createFirstUser calls on non-empty DB,
     * an exception is thrown with "First user already present!" (idempotency guard preserved).
     *
     * <p>Since no GlobalExceptionHandler exists, the exception propagates as a
     * ServletException wrapping the original exception.</p>
     */
    @Test
    @Order(3)
    @DisplayName("AC-4: createFirstUser on non-empty DB throws 'First user already present!'")
    void property_createFirstUserOnNonEmptyDb_throwsException() {
        // The DB already has users from the setup step.
        // Without a GlobalExceptionHandler, the unhandled exception propagates as ServletException.
        assertThatThrownBy(() ->
                mockMvc.perform(post("/api/auth/createFirstUser"))
                        .andReturn()
        )
                .isInstanceOf(ServletException.class)
                .hasMessageContaining("First user already present!");
    }

    /**
     * Property 4 (AC-7, AC-9): BCrypt encoding/matching works end-to-end.
     * Password is encoded at creation time and verified at login time.
     *
     * <p>This property confirms the DI fix did not break the encode→match pipeline.</p>
     */
    @Test
    @Order(4)
    @DisplayName("AC-7, AC-9: BCrypt encode/match pipeline works end-to-end (DI preserved)")
    void property_bcryptEncodingMatching_worksEndToEnd() throws Exception {
        // Verify the PasswordEncoder bean itself works correctly
        String rawPassword = "password";
        String encoded = passwordEncoder.encode(rawPassword);

        // Encoded password must NOT equal raw password
        assertThat(encoded).isNotEqualTo(rawPassword);

        // Encoded password must match raw password via BCrypt verification
        assertThat(passwordEncoder.matches(rawPassword, encoded))
                .as("BCrypt matches() must verify the raw password against its encoded form")
                .isTrue();

        // Different raw password must NOT match
        assertThat(passwordEncoder.matches("wrongpassword", encoded))
                .as("BCrypt matches() must reject incorrect passwords")
                .isFalse();

        // End-to-end: login with the correct password must succeed (user created in test 1)
        String loginJson = """
                {
                    "email": "admin@elis.org",
                    "password": "password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk());

        // Login with wrong password must fail.
        // Without a GlobalExceptionHandler, InvalidCredentialsException propagates as ServletException.
        String wrongLoginJson = """
                {
                    "email": "admin@elis.org",
                    "password": "wrongpassword"
                }
                """;

        assertThatThrownBy(() ->
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(wrongLoginJson))
                        .andReturn()
        )
                .isInstanceOf(ServletException.class)
                .hasMessageContaining("InvalidCredentialsException");
    }
}
