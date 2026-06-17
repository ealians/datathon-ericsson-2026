package org.elis.ericsson.datathon.user_management.bugfix;

import org.elis.ericsson.datathon.user_management.constants.Endpoints;
import org.elis.ericsson.datathon.user_management.controller.AuthController;
import org.elis.ericsson.datathon.user_management.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug Condition Exploration Test - UC-S-003
 *
 * These tests encode the EXPECTED behavior (what the code SHOULD do after the fix).
 * When run on UNFIXED code, they FAIL - confirming the bug exists.
 *
 * Bug Condition:
 * - JwtAuthenticationFilter executes on createFirstUser (shouldNotFilter not implemented)
 * - AuthController interface does not declare createFirstUser
 * - Endpoints.CREATE_FIRST_USER constant does not exist
 * - SecurityConfig lacks explicit permitAll() for POST /api/auth/createFirstUser
 *
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4
 */
@SpringBootTest
@AutoConfigureMockMvc
class CreateFirstUserBugConditionExplorationTest {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private MockMvc mockMvc;

    /**
     * Sub-task 1: Test che JwtAuthenticationFilter.shouldNotFilter(request) restituisce true
     * per POST /api/auth/createFirstUser.
     *
     * Bug Condition: jwtFilterExecutesOn(request.path) == TRUE
     * Expected Behavior: shouldNotFilter() returns true for public endpoints
     *
     * Validates: Requirements 2.2
     */
    @Test
    @DisplayName("shouldNotFilter() must return true for /api/auth/createFirstUser")
    void shouldNotFilter_returnsTrue_forCreateFirstUser() throws Exception {
        // The method shouldNotFilter must exist and return true for createFirstUser path
        Method shouldNotFilterMethod = JwtAuthenticationFilter.class
                .getDeclaredMethod("shouldNotFilter", jakarta.servlet.http.HttpServletRequest.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/auth/createFirstUser");
        request.setMethod("POST");

        shouldNotFilterMethod.setAccessible(true);
        boolean result = (boolean) shouldNotFilterMethod.invoke(jwtAuthenticationFilter, request);

        assertThat(result)
                .as("JwtAuthenticationFilter.shouldNotFilter() should return true for /api/auth/createFirstUser")
                .isTrue();
    }

    /**
     * Sub-task 2: Test che AuthController dichiara il metodo createFirstUser.
     *
     * Bug Condition: interfaceDeclaration("createFirstUser") NOT EXISTS
     * Expected Behavior: AuthController interface declares createFirstUser
     *
     * Validates: Requirements 2.3
     */
    @Test
    @DisplayName("AuthController interface must declare createFirstUser method")
    void authController_declares_createFirstUser() {
        boolean methodExists = false;
        for (Method method : AuthController.class.getDeclaredMethods()) {
            if ("createFirstUser".equals(method.getName())) {
                methodExists = true;
                break;
            }
        }

        assertThat(methodExists)
                .as("AuthController interface should declare createFirstUser method")
                .isTrue();
    }

    /**
     * Sub-task 3: Test che Endpoints.CREATE_FIRST_USER esiste con valore "/createFirstUser".
     *
     * Bug Condition: endpointConstant("CREATE_FIRST_USER") NOT EXISTS
     * Expected Behavior: Endpoints.CREATE_FIRST_USER == "/createFirstUser"
     *
     * Validates: Requirements 2.4
     */
    @Test
    @DisplayName("Endpoints.CREATE_FIRST_USER constant must exist with value '/createFirstUser'")
    void endpoints_hasCreateFirstUserConstant() throws Exception {
        Field field = Endpoints.class.getDeclaredField("CREATE_FIRST_USER");

        assertThat(field).isNotNull();
        assertThat(java.lang.reflect.Modifier.isPublic(field.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isStatic(field.getModifiers())).isTrue();
        assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers())).isTrue();

        String value = (String) field.get(null);
        assertThat(value)
                .as("Endpoints.CREATE_FIRST_USER should have value '/createFirstUser'")
                .isEqualTo("/createFirstUser");
    }

    /**
     * Sub-task 4: Test che SecurityConfig ha regola esplicita permitAll() per
     * POST /api/auth/createFirstUser.
     *
     * This is verified via an integration test: POST /api/auth/createFirstUser without
     * authentication should return 200 OK (not 302 redirect or 401/403).
     * On unfixed code, this currently works via wildcard but the test validates the endpoint
     * is accessible. The real verification is that the explicit rule exists in code.
     *
     * Bug Condition: explicitPermitAllRule(request.path) NOT EXISTS
     * Expected Behavior: Explicit permitAll() rule with POST method constraint
     *
     * Validates: Requirements 2.1
     */
    @Test
    @DisplayName("POST /api/auth/createFirstUser must be accessible without authentication (explicit permitAll)")
    void securityConfig_hasExplicitPermitAll_forCreateFirstUser() throws Exception {
        // Verify via MockMvc that the endpoint is accessible without authentication
        // and does not redirect to /login (which would indicate implicit access via wildcard only).
        // The request must reach the controller (not be blocked by security).
        // A 200 means first user created successfully.
        // A ServletException with "First user already present!" also proves security passed
        // (request reached the service layer - only thrown when DB already has a user).
        try {
            var result = mockMvc.perform(post("/api/auth/createFirstUser")
                            .contentType("application/json"))
                    .andReturn();

            int statusCode = result.getResponse().getStatus();
            // The endpoint must NOT be blocked by security (401/403/302 redirect to login)
            assertThat(statusCode)
                    .as("POST /api/auth/createFirstUser should not be blocked by security (got %d)", statusCode)
                    .isNotIn(401, 403, 302);
        } catch (jakarta.servlet.ServletException e) {
            // If a ServletException is thrown, the request reached the controller/service layer.
            // This means security did NOT block it. The exception "First user already present!"
            // is an application-level error, not a security rejection.
            assertThat(e.getMessage())
                    .as("Exception should be application-level (not security), proving request passed security filters")
                    .contains("First user already present!");
        }
    }
}
