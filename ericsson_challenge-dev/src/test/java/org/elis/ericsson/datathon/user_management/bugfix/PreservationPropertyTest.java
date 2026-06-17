package org.elis.ericsson.datathon.user_management.bugfix;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preservation Property Tests - UC-S-003
 *
 * These tests verify the EXISTING behavior that must be preserved after the bugfix.
 * They run on UNFIXED code and MUST PASS - establishing the baseline.
 *
 * Property 2: Preservation - Comportamento invariato su endpoint protetti e pubblici esistenti
 *
 * After the fix is applied, these tests will be re-run to confirm no regressions.
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4
 */
@SpringBootTest
@AutoConfigureMockMvc
class PreservationPropertyTest {

    @Autowired
    private MockMvc mockMvc;

    // =========================================================================
    // Property Test 1: Protected endpoints require authentication (redirect 302)
    // For all protected endpoint paths, access without token generates redirect to /login
    // =========================================================================

    /**
     * Generates protected POST endpoint paths that require authentication.
     * On unfixed code, accessing these without a token results in 302 redirect to /login.
     */
    static Stream<String> protectedPostEndpointPaths() {
        return Stream.of(
                "/api/profiles/add",
                "/profiles/edit/1"
        );
    }

    /**
     * Property: For all protected POST endpoints, access without token generates 302 redirect to /login.
     *
     * This validates that authentication is required for protected endpoints and that
     * the LoginUrlAuthenticationEntryPoint correctly redirects unauthenticated requests.
     * This behavior must remain unchanged after the bugfix.
     *
     * **Validates: Requirements 3.1, 3.2**
     */
    @ParameterizedTest(name = "Protected POST endpoint [{0}] without token -> 302 redirect to /login")
    @MethodSource("protectedPostEndpointPaths")
    @DisplayName("Protected POST endpoints without token must redirect to /login")
    void protectedPostEndpoints_withoutToken_shouldRedirectToLogin(String path) throws Exception {
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")));
    }

    /**
     * Property: For all protected GET endpoints, access without token generates 302 redirect to /login.
     *
     * **Validates: Requirements 3.1, 3.2**
     */
    @ParameterizedTest(name = "Protected GET endpoint [{0}] without token -> 302 redirect to /login")
    @ValueSource(strings = {"/profiles", "/profiles/add-profile"})
    @DisplayName("Protected GET endpoints without token must redirect to /login")
    void protectedGetEndpoints_withoutToken_shouldRedirectToLogin(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")));
    }

    // =========================================================================
    // Property Test 2: Public pages and auth endpoints are accessible without token
    // The login page and createFirstUser are accessible without authentication
    // =========================================================================

    /**
     * Property: The /login page must be accessible without authentication (returns 200 OK).
     *
     * This is a Thymeleaf page that is explicitly listed in permitAll() and must remain
     * accessible to unauthenticated users.
     *
     * **Validates: Requirements 3.3**
     */
    @ParameterizedTest(name = "Public page [{0}] without token -> 200 OK")
    @ValueSource(strings = {"/login"})
    @DisplayName("Login page must be accessible without token (200 OK)")
    void loginPage_withoutToken_shouldBeAccessible(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    /**
     * Property: POST /api/auth/createFirstUser without token reaches the controller
     * (not blocked by security). On unfixed code with empty DB, this returns 200.
     * The endpoint is accessible via the /api/auth/** wildcard permitAll().
     *
     * **Validates: Requirements 3.3**
     */
    @ParameterizedTest(name = "Public auth endpoint [{0}] without token -> reachable (not 401/403)")
    @ValueSource(strings = {"/api/auth/createFirstUser"})
    @DisplayName("createFirstUser endpoint must be accessible without token")
    void createFirstUser_withoutToken_shouldBeAccessible(String path) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int statusCode = result.getResponse().getStatus();
        // The endpoint should be accessible (not blocked by security with 401 or 403)
        // It returns 200 on empty DB (creates first user) or other application-level status
        assertThat(statusCode)
                .as("Public endpoint %s should not be blocked by security (401/403)", path)
                .isNotIn(401, 403);
    }

    // =========================================================================
    // Property Test 3: Static resources are accessible without token
    // For all static resource paths, access without token does not redirect to /login
    // =========================================================================

    /**
     * Generates static resource paths that should be accessible without token.
     * These are covered by the permitAll() rules for /webjars/**, /css/**, /js/**.
     */
    static Stream<String> staticResourcePaths() {
        return Stream.of(
                "/webjars/bootstrap/5.3.3/css/bootstrap.min.css",
                "/webjars/bootstrap/5.3.3/js/bootstrap.bundle.min.js"
        );
    }

    /**
     * Property: For all static resource paths (webjars, css, js), access without token
     * does NOT result in a 302 redirect to /login. Static resources must remain publicly accessible.
     *
     * They return 200 (resource served) or 404 (resource not found), but never 302 (auth redirect).
     *
     * **Validates: Requirements 3.4**
     */
    @ParameterizedTest(name = "Static resource [{0}] without token -> NOT 302 redirect")
    @MethodSource("staticResourcePaths")
    @DisplayName("Static resources must be accessible without token (no auth redirect)")
    void staticResources_withoutToken_shouldNotRedirect(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path))
                .andReturn();

        int statusCode = result.getResponse().getStatus();
        // Static resources should return 200 (served) or possibly 404 (not found in test),
        // but NEVER 302 redirect to /login (they are not auth-protected)
        assertThat(statusCode)
                .as("Static resource %s should NOT redirect to login (302), got %d", path, statusCode)
                .isNotEqualTo(302);
    }

    // =========================================================================
    // Property Test 4: JwtAuthenticationFilter runs on ALL requests (unfixed behavior)
    // On unfixed code, there is NO shouldNotFilter() override, so the filter executes
    // on every request including public endpoints. This test verifies that the
    // filter chain still works correctly despite filtering all requests.
    // =========================================================================

    /**
     * Property: The security filter chain correctly differentiates between
     * authenticated and unauthenticated access based on SecurityConfig rules,
     * not on JwtAuthenticationFilter's shouldNotFilter (which doesn't exist yet).
     *
     * Protected endpoints with no token -> 302 (authentication required)
     * Public endpoints with no token -> reachable (permitAll applies after filter)
     *
     * This ensures the fix (adding shouldNotFilter) does not change the observable behavior.
     *
     * **Validates: Requirements 3.1, 3.4**
     */
    @ParameterizedTest(name = "Security chain differentiates access: protected [{0}] -> 302")
    @ValueSource(strings = {"/api/profiles/add"})
    @DisplayName("Security chain correctly requires auth for ADMIN-only endpoints")
    void securityChain_protectedAdminEndpoints_requireAuth(String path) throws Exception {
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(302))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")));
    }
}
