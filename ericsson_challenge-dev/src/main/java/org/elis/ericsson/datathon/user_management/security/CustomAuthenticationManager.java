package org.elis.ericsson.datathon.user_management.security;

import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomAuthenticationManager implements AuthenticationManager {

    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationManager(UserProfileRepository userProfileRepository,
                                       PasswordEncoder passwordEncoder) {
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        Optional<UserProfile> userByEmail = userProfileRepository.findByEmail(email);

        if (userByEmail.isEmpty()) {
            throw new BadCredentialsException("Email not found");
        }

        UserProfile user = userByEmail.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Password is incorrect");
        }
        return new UsernamePasswordAuthenticationToken(email, null, user.getAuthorities());
    }
}
