package org.elis.ericsson.datathon.user_management.controller.impl;

import jakarta.transaction.Transactional;
import org.elis.ericsson.datathon.user_management.controller.UserProfileController;

import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.exception.ItemNotFoundException;
import org.elis.ericsson.datathon.user_management.repository.RefreshTokenRepository;
import org.elis.ericsson.datathon.user_management.repository.RoleRepository;
import org.elis.ericsson.datathon.user_management.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static org.elis.ericsson.datathon.user_management.constants.Endpoints.API;
import static org.elis.ericsson.datathon.user_management.constants.Endpoints.PROFILE;

@RestController
@RequestMapping(API + PROFILE)
public class UserProfileControllerImpl implements UserProfileController {

    private final UserProfileService userProfileService;

    private final RefreshTokenRepository refreshTokenRepository;



    public UserProfileControllerImpl(UserProfileService userProfileService, RefreshTokenRepository refreshTokenRepository) {
        this.userProfileService = userProfileService;
        this.refreshTokenRepository = refreshTokenRepository;

    }

    @Override
    public ResponseEntity<List<UserProfile>> getAllProfiles() {
        List<UserProfile> profiles = userProfileService.getAllProfiles();
        return ResponseEntity.ok(profiles);
    }

    @Transactional
    @Override
    public ResponseEntity<Void> deleteProfile(@PathVariable Long id) throws ItemNotFoundException {
        refreshTokenRepository.deleteByUserId(id);
        userProfileService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }

}
