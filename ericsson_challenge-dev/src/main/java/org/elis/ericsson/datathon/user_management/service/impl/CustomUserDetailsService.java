package org.elis.ericsson.datathon.user_management.service.impl;

import org.elis.ericsson.datathon.user_management.model.entity.UserPrincipal;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.exception.ItemNotFoundException;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to handle user details.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserProfileRepository userProfileRepository;
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    public CustomUserDetailsService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Load user by email.
     *
     * @param email The username.
     * @return The user details.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            logger.debug("Entering loadUserByUsername with email: {}", email);
            UserProfile userProfile = userProfileRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("User not found with email: " + email)
                    );

            return UserPrincipal.create(userProfile);
        } catch (Exception e) {
            logger.error("Error in loadUserByUsername for email: {}", email, e);
            throw e;
        }
    }

    /**
     * Load user by id.
     *
     * @param id The id of the user.
     * @return The UserDetails.
     */
    @Transactional
    public UserDetails loadUserById(Long id) {
        logger.debug("Entering loadUserById with id: {}", id);
        UserProfile user = userProfileRepository.findById(id).orElseThrow(
                () -> new ItemNotFoundException("User not found with id: " + id)
        );

        return UserPrincipal.create(user);
    }
}
