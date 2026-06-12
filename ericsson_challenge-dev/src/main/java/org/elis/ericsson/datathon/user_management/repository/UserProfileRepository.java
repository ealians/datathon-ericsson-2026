package org.elis.ericsson.datathon.user_management.repository;

import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.projection.UserMeInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {


    Optional<UserProfile> findByEmail(String email);

    Optional<UserMeInfo> getUserProjectedMeById(Long userId);

    boolean existsByEmail(String email);


    List<UserProfile> findAllByIdIn(List<Long> userIds);

    UserProfile findUserByEmail(String email);


}


