package org.elis.ericsson.datathon.user_management.service.impl;

import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.exception.ItemNotFoundException;
import org.elis.ericsson.datathon.user_management.repository.RoleRepository;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.elis.ericsson.datathon.user_management.service.UserProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    public UserProfileServiceImpl(UserProfileRepository userProfileRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    @Override
    public List<UserProfile> getAllProfiles() {
        return userProfileRepository.findAll();
    }

    @Override
    public UserProfile addProfile(UserProfile newProfile) {
        // Codifica la password prima di salvare il profilo
        Set<Role> roles = new HashSet<>();
        for (Role role : newProfile.getRoles()) {
            roles.add(roleRepository.findByName(role.getName()).orElseThrow(() -> new RuntimeException("Role not found")));
        }
        newProfile.setRoles(roles);
        String encodedPassword = passwordEncoder.encode(newProfile.getPassword());
        newProfile.setPassword(encodedPassword);

        return userProfileRepository.save(newProfile);
    }

    @Override
    public UserProfile editProfile(Long id, UserProfile updatedProfile) throws ItemNotFoundException {
        return userProfileRepository.findById(id)
                .map(existingProfile -> {
                    existingProfile.setFirstName(updatedProfile.getFirstName());
                    existingProfile.setLastName(updatedProfile.getLastName());
                    existingProfile.setEmail(updatedProfile.getEmail());
                    existingProfile.setUsername(updatedProfile.getUsername());
                    existingProfile.setPhoneNumber(updatedProfile.getPhoneNumber());

                    // Se una nuova password è presente, codificala e impostala
                    if (updatedProfile.getPassword() != null && !updatedProfile.getPassword().isEmpty()) {
                        String encodedPassword = passwordEncoder.encode(updatedProfile.getPassword());
                        existingProfile.setPassword(encodedPassword);
                    }

                    // Aggiorna i ruoli
                    existingProfile.setRoles(updatedProfile.getRoles());

                    return userProfileRepository.save(existingProfile);
                })
                .orElseThrow(() -> new ItemNotFoundException("User profile not found with id: " + id));
    }


    @Override
    public void deleteProfile(Long id) throws ItemNotFoundException {
        if (!userProfileRepository.existsById(id)) {
            throw new ItemNotFoundException("User profile not found with id: " + id);
        }
        userProfileRepository.deleteById(id);
    }

    @Override
    public UserProfile getProfileById(Long id) throws ItemNotFoundException {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("User profile not found with id: " + id));
    }
}
