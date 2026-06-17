package org.elis.ericsson.datathon.user_management;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.RoleRepository;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preservation Property Tests for UC-S-003: createFirstUser endpoint.
 *
 * These tests capture the EXISTING correct behavior that MUST be preserved after the bugfix.
 * They are designed to PASS on UNFIXED code, confirming the baseline.
 *
 * Preserved behaviors tested:
 * 1. Duplicate prevention: createFirstUser rejects when users already exist
 * 2. Role auto-creation: ROLE_ADMIN and ROLE_USER are created if missing
 * 3. Role reuse: existing roles are reused without duplication
 * 4. Public access: endpoint is accessible without authentication
 * 5. Both roles assigned: the first user gets both ROLE_ADMIN and ROLE_USER
 * 6. Login endpoint: continues to work with admin credentials after creation
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateFirstUserPreservationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String CREATE_FIRST_USER_URL = "/api/auth/createFirstUser";
    private static final String LOGIN_URL = "/api/auth/login";

    private static final String VALID_REQUEST_BODY = """
            {
                "email": "admin@elis.org",
                "password": "password",
                "firstName": "firstName_admin",
                "lastName": "lastName_admin"
            }
            """;

    /**
     * Property 2 - Preservation: Duplicate Prevention
     *
     * When the database already has at least one user, calling createFirstUser
     * MUST be rejected with an error (throws Exception "First user already present!").
     *
     * **Validates: Requirements 3.1**
     */
    @Test
    @DisplayName("Preservation 3.1: createFirstUser rejects when DB already has users")
    void createFirstUser_whenUsersExist_shouldRejectRequest() throws Exception {
        // ARRANGE: Create an existing user in the database
        UserProfile existingUser = new UserProfile();
        existingUser.setEmail("existing@test.it");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");
        userProfileRepository.save(existingUser);

        assertTrue(userProfileRepository.count() > 0, "Precondition: DB should have at least one user");

        // ACT & ASSERT: The service throws an exception when users already exist,
        // which propagates as a ServletException through MockMvc
        Exception thrownException = assertThrows(Exception.class, () -> {
            mockMvc.perform(post(CREATE_FIRST_USER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REQUEST_BODY))
                    .andReturn();
        });

        assertTrue(thrownException.getMessage().contains("First user already present!"),
                "Preservation violated: expected 'First user already present!' but got: " + thrownException.getMessage());
    }

    /**
     * Property 2 - Preservation: Role Auto-Creation
     *
     * When ROLE_ADMIN and ROLE_USER do not exist in the database, calling createFirstUser
     * MUST create them automatically.
     *
     * **Validates: Requirements 3.2**
     */
    @Test
    @DisplayName("Preservation 3.2: createFirstUser creates ROLE_ADMIN and ROLE_USER if missing")
    void createFirstUser_whenRolesNotExist_shouldCreateBothRoles() throws Exception {
        // ARRANGE: Ensure no roles exist
        assertEquals(0, roleRepository.count(), "Precondition: no roles should exist");

        // ACT
        mockMvc.perform(post(CREATE_FIRST_USER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk());

        // ASSERT: Both roles now exist
        Optional<Role> adminRole = roleRepository.findByName("ROLE_ADMIN");
        Optional<Role> userRole = roleRepository.findByName("ROLE_USER");

        assertTrue(adminRole.isPresent(), "Preservation violated: ROLE_ADMIN was not created automatically");
        assertTrue(userRole.isPresent(), "Preservation violated: ROLE_USER was not created automatically");
    }

    /**
     * Property 2 - Preservation: Role Reuse (no duplication)
     *
     * When ROLE_ADMIN and ROLE_USER already exist in the database, calling createFirstUser
     * MUST reuse them without creating duplicates.
     *
     * **Validates: Requirements 3.3**
     */
    @Test
    @DisplayName("Preservation 3.3: createFirstUser reuses existing roles without duplication")
    void createFirstUser_whenRolesExist_shouldReuseWithoutDuplication() throws Exception {
        // ARRANGE: Pre-create the roles
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        roleRepository.save(userRole);

        long roleCountBefore = roleRepository.count();
        assertEquals(2, roleCountBefore, "Precondition: exactly 2 roles should exist");

        // ACT
        mockMvc.perform(post(CREATE_FIRST_USER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk());

        // ASSERT: Role count must still be exactly 2
        long roleCountAfter = roleRepository.count();
        assertEquals(2, roleCountAfter,
                "Preservation violated: roles were duplicated. Before: " + roleCountBefore + ", After: " + roleCountAfter);
    }

    /**
     * Property 2 - Preservation: Public Access
     *
     * The endpoint /api/auth/createFirstUser MUST be accessible without authentication.
     *
     * **Validates: Requirements 3.4**
     */
    @Test
    @DisplayName("Preservation 3.4: createFirstUser endpoint is accessible without authentication")
    void createFirstUser_withoutAuthentication_shouldBeAccessible() throws Exception {
        // ACT: Call the endpoint without any auth token
        MvcResult result = mockMvc.perform(post(CREATE_FIRST_USER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andReturn();

        // ASSERT: Should NOT get 401 or 403
        int statusCode = result.getResponse().getStatus();
        assertNotEquals(401, statusCode,
                "Preservation violated: endpoint returned 401 - it should be publicly accessible");
        assertNotEquals(403, statusCode,
                "Preservation violated: endpoint returned 403 - it should be publicly accessible");
    }

    /**
     * Property 2 - Preservation: Both Roles Assigned to First User
     *
     * After successful createFirstUser, the created user MUST have both
     * ROLE_ADMIN and ROLE_USER assigned.
     *
     * **Validates: Requirements 3.5**
     */
    @Test
    @DisplayName("Preservation 3.5: createFirstUser assigns both ROLE_ADMIN and ROLE_USER to first user")
    void createFirstUser_onSuccess_shouldAssignBothRoles() throws Exception {
        // ACT
        mockMvc.perform(post(CREATE_FIRST_USER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk());

        // ASSERT
        List<UserProfile> users = userProfileRepository.findAll();
        assertEquals(1, users.size(), "Expected exactly one user after createFirstUser");

        UserProfile createdUser = users.get(0);
        assertNotNull(createdUser.getRoles(), "Created user should have roles assigned");

        List<String> roleNames = createdUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        assertTrue(roleNames.contains("ROLE_ADMIN"),
                "Preservation violated: created user does not have ROLE_ADMIN. Roles: " + roleNames);
        assertTrue(roleNames.contains("ROLE_USER"),
                "Preservation violated: created user does not have ROLE_USER. Roles: " + roleNames);
    }

    /**
     * Property 2 - Preservation: Login Endpoint Works After User Creation
     *
     * After createFirstUser succeeds, the login endpoint MUST work with the
     * credentials used during creation and return an AuthResponseDTO.
     *
     * **Validates: Requirements 3.6**
     */
    @Test
    @DisplayName("Preservation 3.6: login endpoint works after createFirstUser with correct credentials")
    void loginEndpoint_afterCreateFirstUser_shouldReturnAuthResponse() throws Exception {
        // ARRANGE: Create first user with admin@elis.org / password
        mockMvc.perform(post(CREATE_FIRST_USER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk());

        // ACT: Login with the credentials used in creation
        String loginBody = """
                {
                    "email": "admin@elis.org",
                    "password": "password"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        // ASSERT: Response has the expected AuthResponseDTO structure
        String responseBody = loginResult.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);

        assertTrue(responseJson.has("id"),
                "Preservation violated: login response missing 'id' field");
        assertTrue(responseJson.has("email"),
                "Preservation violated: login response missing 'email' field");
        assertTrue(responseJson.has("token"),
                "Preservation violated: login response missing 'token' field");
        assertTrue(responseJson.has("refreshToken"),
                "Preservation violated: login response missing 'refreshToken' field");

        String token = responseJson.get("token").asText();
        assertFalse(token.isEmpty(), "Preservation violated: login token is empty");
    }
}
