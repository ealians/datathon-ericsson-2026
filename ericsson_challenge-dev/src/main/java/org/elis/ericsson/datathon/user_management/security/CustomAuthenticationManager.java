package org.elis.ericsson.datathon.user_management.security;

import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomAuthenticationManager implements AuthenticationManager {

    private final UserProfileRepository userProfileRepository;

    final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CustomAuthenticationManager(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getPrincipal() + "";
        String password = authentication.getCredentials() + "";

        Optional<UserProfile> userByEmail = userProfileRepository.findByEmail(email);

        if (userByEmail.isEmpty()) {
            throw new BadCredentialsException("Email not found");
        }

        UserProfile user = userByEmail.orElseGet(userByEmail::get);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Password is incorrect");
        }
        return new UsernamePasswordAuthenticationToken(email, null, user.getAuthorities());
    }
}
