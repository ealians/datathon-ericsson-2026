package org.elis.ericsson.datathon.user_management.bugfix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.security.CustomAuthenticationManager;
import org.elis.ericsson.datathon.user_management.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bug Condition Exploration Test for createFirstUser endpoint.
 *
 * <p><b>Validates: Requirements AC-1, AC-2, AC-3, AC-5</b></p>
 *
 * <p>This test verifies that the bug conditions identified in UC-S-003 are resolved:</p>
 * <ul>
 *   <li>C(X): UserProfile serialized via Jackson without @JsonIgnore on password → response contains password field</li>
 *   <li>C(X): UserProfile serialized via Jackson without @JsonIgnore on eggUpInfo → StackOverflowError or infinite recursion</li>
 *   <li>C(X): AuthServiceImpl or CustomAuthenticationManager uses new BCryptPasswordEncoder() instead of injected bean</li>
 * </ul>
 *
 * <p>On fixed code: all tests PASS (confirming the bugs are resolved).</p>
 * <p>On unfixed code: tests would FAIL (confirming the bugs exist).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CreateFirstUserBugConditionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthServiceImpl authServiceImpl;

    @Autowired
    private CustomAuthenticationManager customAuthenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoderBean;

    /**
     * Integration test: POST /api/auth/createFirstUser on empty DB.
     * Verifies HTTP 200, no password in response, no serialization error.
     * Uses @DirtiesContext to ensure fresh DB state (empty DB required).
     */
    @Nested
    @DisplayName("Integration: POST /api/auth/createFirstUser - Serialization Bug Conditions")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    class IntegrationTests {

        @Test
        @DirtiesContext
        @DisplayName("AC-1, AC-2, AC-3: createFirstUser returns HTTP 200 with clean JSON (no password, no eggUpInfo, no serialization error)")
        void createFirstUser_onEmptyDb_returnsCleanJsonWithoutSensitiveFields() throws Exception {
            // POST to createFirstUser on empty DB
            MvcResult result = mockMvc.perform(post("/api/auth/createFirstUser"))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();

            // AC-1: Verify HTTP 200 and valid JSON response
            assertThat(responseBody).isNotEmpty();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            assertThat(jsonNode).isNotNull();
            assertThat(jsonNode.has("email")).isTrue();
            assertThat(jsonNode.get("email").asText()).isEqualTo("admin@elis.org");

            // AC-2: Response JSON does NOT contain 'password' field
            // Bug condition: without @JsonIgnore on password, the encoded password
            // would be visible in the JSON response
            assertThat(responseBody).doesNotContain("\"password\"");

            // AC-3: Response JSON does NOT contain 'eggUpInfo' field
            // Bug condition: without @JsonIgnore on eggUpInfo, Jackson would attempt
            // circular serialization (UserProfile ↔ EggUpInfo) → StackOverflowError
            assertThat(responseBody).doesNotContain("\"eggUpInfo\"");
        }

        @Test
        @DisplayName("AC-2+AC-3: UserProfile Jackson serialization excludes password and eggUpInfo fields")
        void userProfile_jacksonSerialization_excludesSensitiveFields() throws Exception {
            // Directly test Jackson serialization of UserProfile
            UserProfile user = new UserProfile();
            user.setId(1L);
            user.setEmail("test@example.com");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setPassword("$2a$10$encodedPasswordHash");
            // eggUpInfo left null but field still present in class

            String json = objectMapper.writeValueAsString(user);

            // Bug condition: without @JsonIgnore, password would appear in JSON
            assertThat(json).doesNotContain("\"password\"");
            assertThat(json).doesNotContain("encodedPasswordHash");

            // Bug condition: without @JsonIgnore, eggUpInfo field would appear
            assertThat(json).doesNotContain("\"eggUpInfo\"");
        }
    }

    /**
     * Unit tests: verify AuthServiceImpl and CustomAuthenticationManager
     * use injected PasswordEncoder (not local instantiation).
     */
    @Nested
    @DisplayName("Unit: DI PasswordEncoder Bug Condition (AC-5)")
    class DependencyInjectionTests {

        @Test
        @DisplayName("AC-5: AuthServiceImpl uses injected PasswordEncoder bean (not new BCryptPasswordEncoder())")
        void authServiceImpl_usesInjectedPasswordEncoder() throws Exception {
            // Use reflection to verify the passwordEncoder field is the same bean instance
            Field passwordEncoderField = AuthServiceImpl.class.getDeclaredField("passwordEncoder");
            passwordEncoderField.setAccessible(true);
            Object actualEncoder = passwordEncoderField.get(authServiceImpl);

            // If AuthServiceImpl used `new BCryptPasswordEncoder()`, this would be
            // a different instance than the Spring-managed bean
            assertThat(actualEncoder)
                    .as("AuthServiceImpl.passwordEncoder must be the same Spring bean instance (not new BCryptPasswordEncoder())")
                    .isSameAs(passwordEncoderBean);
        }

        @Test
        @DisplayName("AC-5: CustomAuthenticationManager uses injected PasswordEncoder bean (not new BCryptPasswordEncoder())")
        void customAuthenticationManager_usesInjectedPasswordEncoder() throws Exception {
            // Use reflection to verify the passwordEncoder field is the same bean instance
            Field passwordEncoderField = CustomAuthenticationManager.class.getDeclaredField("passwordEncoder");
            passwordEncoderField.setAccessible(true);
            Object actualEncoder = passwordEncoderField.get(customAuthenticationManager);

            // If CustomAuthenticationManager used `new BCryptPasswordEncoder()`, this would be
            // a different instance than the Spring-managed bean
            assertThat(actualEncoder)
                    .as("CustomAuthenticationManager.passwordEncoder must be the same Spring bean instance (not new BCryptPasswordEncoder())")
                    .isSameAs(passwordEncoderBean);
        }
    }
}
