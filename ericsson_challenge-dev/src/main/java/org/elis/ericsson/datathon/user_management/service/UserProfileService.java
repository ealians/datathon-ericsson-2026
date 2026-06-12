package org.elis.ericsson.datathon.user_management.service;

import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.exception.ItemNotFoundException;

import java.util.List;

public interface UserProfileService {
    List<UserProfile> getAllProfiles();
    UserProfile addProfile(UserProfile newProfile);
    UserProfile editProfile(Long id, UserProfile updatedProfile) throws ItemNotFoundException;
    void deleteProfile(Long id) throws ItemNotFoundException;
    UserProfile getProfileById(Long id) throws ItemNotFoundException;
}
