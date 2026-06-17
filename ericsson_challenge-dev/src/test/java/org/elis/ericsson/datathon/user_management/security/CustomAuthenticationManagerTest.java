package org.elis.ericsson.datathon.user_management.security;

import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationManagerTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CustomAuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        authenticationManager = new CustomAuthenticationManager(userProfileRepository, passwordEncoder);
    }

    @Test
    void whenValidCredentials_thenReturnsAuthentication() {
        String email = "user@example.com";
        String rawPassword = "password123";
        String encodedPassword = "$2a$10$encodedHash";

        UserProfile user = new UserProfile();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setRoles(List.of(new Role("ROLE_USER")));

        when(userProfileRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        Authentication request = new UsernamePasswordAuthenticationToken(email, rawPassword);
        Authentication result = authenticationManager.authenticate(request);

        assertThat(result).isNotNull();
        assertThat(result.getPrincipal()).isEqualTo(email);
        assertThat(result.getAuthorities()).isNotEmpty();
    }

    @Test
    void whenEmailNotFound_thenThrowsBadCredentials() {
        String email = "unknown@example.com";
        when(userProfileRepository.findByEmail(email)).thenReturn(Optional.empty());

        Authentication request = new UsernamePasswordAuthenticationToken(email, "password");

        assertThatThrownBy(() -> authenticationManager.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email not found");
    }

    @Test
    void whenPasswordIncorrect_thenThrowsBadCredentials() {
        String email = "user@example.com";
        String rawPassword = "wrongPassword";
        String encodedPassword = "$2a$10$encodedHash";

        UserProfile user = new UserProfile();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setRoles(List.of(new Role("ROLE_USER")));

        when(userProfileRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        Authentication request = new UsernamePasswordAuthenticationToken(email, rawPassword);

        assertThatThrownBy(() -> authenticationManager.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Password is incorrect");
    }
}
