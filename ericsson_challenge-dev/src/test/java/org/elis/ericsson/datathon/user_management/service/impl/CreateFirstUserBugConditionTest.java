package org.elis.ericsson.datathon.user_management.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.RoleRepository;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.elis.ericsson.datathon.user_management.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug Condition Exploration Test for CreateFirstUser.
 *
 * These tests encode the EXPECTED (correct) behavior for the createFirstUser endpoint
 * when called on an empty database. They are expected to FAIL on the unfixed code,
 * thereby confirming the bugs exist.
 *
 * Validates: Requirements 1.1, 1.2, 1.3
 *
 * Bug Condition: userProfileRepository.count() == 0
 *   - Defect 1 (1.1): Missing @Transactional causes users_roles pivot table to not be populated
 *   - Defect 2 (1.2): Local BCryptPasswordEncoder produces hashes incompatible with Spring-managed bean
 *   - Defect 3 (1.3): Returning raw JPA entity causes circular serialization (500 error)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class CreateFirstUserBugConditionTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder springManagedPasswordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Ensure database is empty (bug condition: userProfileRepository.count() == 0)
        userProfileRepository.deleteAll();
        roleRepository.deleteAll();
        assertThat(userProfileRepository.count()).isEqualTo(0);
    }

    /**
     * Test 1 - Transactional Integrity
     *
     * Validates: Requirements 1.1
     *
     * Calls createFirstUser on empty DB, then verifies the created user
     * has both ROLE_ADMIN and ROLE_USER associations in the users_roles join table.
     *
     * EXPECTED TO FAIL on unfixed code: Missing @Transactional causes the
     * users_roles pivot table to not be populated correctly.
     */
    @Test
    @DisplayName("Bug Condition - Transactional Integrity: user should have both ROLE_ADMIN and ROLE_USER")
    void transactionalIntegrity_whenDatabaseIsEmpty_userIsCreatedWithBothRoles() throws Exception {
        // Given: empty database (bug condition is true)
        assertThat(userProfileRepository.count()).isEqualTo(0);

        // When: createFirstUser is called
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        authService.createFirstUser(mockRequest);

        // Then: user is persisted with both roles in the users_roles join table
        Optional<UserProfile> savedUser = userProfileRepository.findByEmail("admin@elis.org");
        assertThat(savedUser).isPresent();

        Collection<Role> roles = savedUser.get().getRoles();
        assertThat(roles).isNotNull();
        assertThat(roles).hasSize(2);

        List<String> roleNames = roles.stream()
                .map(Role::getName)
                .toList();
        assertThat(roleNames).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    /**
     * Test 2 - Password Encoding Consistency
     *
     * Validates: Requirements 1.2
     *
     * Calls createFirstUser on empty DB, retrieves the saved user, and asserts
     * that the Spring-managed PasswordEncoder can verify the stored password.
     *
     * EXPECTED TO FAIL on unfixed code: The locally-instantiated BCryptPasswordEncoder
     * produces a hash that may not match the Spring-managed encoder's verification.
     */
    @Test
    @DisplayName("Bug Condition - Password Encoding: password should match Spring-managed PasswordEncoder")
    void passwordEncodingConsistency_whenUserCreated_passwordMatchesSpringEncoder() throws Exception {
        // Given: empty database (bug condition is true)
        assertThat(userProfileRepository.count()).isEqualTo(0);

        // When: createFirstUser is called
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        authService.createFirstUser(mockRequest);

        // Then: the stored password matches using the Spring-managed PasswordEncoder bean
        Optional<UserProfile> savedUser = userProfileRepository.findByEmail("admin@elis.org");
        assertThat(savedUser).isPresent();
        assertThat(springManagedPasswordEncoder.matches("password", savedUser.get().getPassword()))
                .as("Password encoded by createFirstUser should be verifiable by Spring-managed PasswordEncoder bean")
                .isTrue();
    }

    /**
     * Test 3 - Serialization Safety
     *
     * Validates: Requirements 1.3
     *
     * Uses MockMvc to call POST /api/auth/createFirstUser and asserts:
     * - HTTP 200 status (not 500)
     * - Response body is valid JSON
     * - Response contains expected keys: email, firstName, lastName, roles
     *
     * EXPECTED TO FAIL on unfixed code: Returning raw JPA entity triggers
     * Jackson circular reference serialization error (UserProfile -> EggUpInfo -> UserProfile),
     * resulting in HTTP 500.
     */
    @Test
    @DisplayName("Bug Condition - Serialization Safety: response should be valid JSON with expected fields")
    void serializationSafety_whenUserCreated_responseIsCleanJson() throws Exception {
        // Given: empty database (bug condition is true)
        assertThat(userProfileRepository.count()).isEqualTo(0);

        // When: POST /api/auth/createFirstUser is called via MockMvc
        MvcResult result = mockMvc.perform(post("/api/auth/createFirstUser"))
                .andExpect(status().isOk())
                .andReturn();

        // Then: response body is valid JSON containing expected fields
        String json = result.getResponse().getContentAsString();
        assertThat(json).isNotEmpty();

        Map<String, Object> body = objectMapper.readValue(json, new TypeReference<>() {});
        assertThat(body).containsKey("email");
        assertThat(body).containsKey("firstName");
        assertThat(body).containsKey("lastName");
        assertThat(body).containsKey("roles");
    }
}
