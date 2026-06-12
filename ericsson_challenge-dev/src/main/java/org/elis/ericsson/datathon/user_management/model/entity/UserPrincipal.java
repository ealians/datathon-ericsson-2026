package org.elis.ericsson.datathon.user_management.model.entity;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class represents the user principal.
 */
public class UserPrincipal implements UserDetails {
    @Getter
    private final Long id;
    @Getter
    private final String email;
    private final String password;
    // Ensure `authorities` is correctly initialized
    @Getter
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    /**
     * Instantiates a new User principal.
     * @param id the id to set
     * @param email the email to set
     * @param password the password to set
     * @param authorities the authorities to set
     */
    public UserPrincipal(Long id, String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    /**
     * Instantiates a new User principal from a UserProfile.
     * @param user the user to create
     */
    public static UserPrincipal create(UserProfile user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

    @Override
    public String getPassword() {
        return password; // Ensure this returns the correct encoded password from the database
    }

    @Override
    public String getUsername() {
        return email; // Assuming email is used as the username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Or retrieve this from user data if applicable
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Or retrieve this from user data if applicable
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
