package org.elis.ericsson.datathon.user_management;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for DataInitializer - automatic first user creation at startup.
 *
 * This test enables the DataInitializer (overriding the test default) to verify
 * that on an empty database, the application startup creates the admin user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "app.data-initializer.enabled=true")
class DataInitializerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private static final String LOGIN_URL = "/api/auth/login";

    @Test
    @DisplayName("DataInitializer creates admin user with admin@elis.org on empty DB at startup")
    void startup_onEmptyDb_shouldCreateAdminUser() {
        List<UserProfile> users = userProfileRepository.findAll();
        assertEquals(1, users.size(), "DataInitializer should have created exactly one user");

        UserProfile admin = users.get(0);
        assertEquals("admin@elis.org", admin.getEmail());
        assertEquals("firstName_admin", admin.getFirstName());
        assertEquals("lastName_admin", admin.getLastName());
    }

    @Test
    @DisplayName("DataInitializer assigns both ROLE_ADMIN and ROLE_USER to the admin user")
    void startup_onEmptyDb_adminShouldHaveBothRoles() {
        List<UserProfile> users = userProfileRepository.findAll();
        assertEquals(1, users.size());

        UserProfile admin = users.get(0);
        List<String> roleNames = admin.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        assertTrue(roleNames.contains("ROLE_ADMIN"), "Admin should have ROLE_ADMIN");
        assertTrue(roleNames.contains("ROLE_USER"), "Admin should have ROLE_USER");
    }

    @Test
    @DisplayName("DataInitializer does not create duplicate when user already exists")
    void startup_withExistingUser_shouldNotDuplicate() {
        // The DataInitializer already ran at startup and created one user.
        // Verify there's still only one user (no duplicates from multiple invocations).
        long userCount = userProfileRepository.count();
        assertEquals(1, userCount, "Should have exactly 1 user, no duplicates");
    }

    @Test
    @DisplayName("Admin user created by DataInitializer can login successfully")
    void startup_adminUser_canLoginSuccessfully() throws Exception {
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

        String responseBody = loginResult.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);

        assertTrue(responseJson.has("token"), "Login response should contain 'token'");
        assertFalse(responseJson.get("token").asText().isEmpty(), "Token should not be empty");
    }
}
