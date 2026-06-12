package org.elis.ericsson.datathon.user_management.model.projection;

import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.entity.Role;

import java.util.Collection;

/**
 * A Projection for the {@link UserProfile} entity
 */
public interface UserMeInfo {
    Long getId();

    String getFirstName();

    String getLastName();

    String getEmail();


    Boolean getCredentialExpired();

    Collection<UserDetailInfo.RoleInfo> getRoles();


    /**
     * Projection for {@link Role}
     */
    interface RoleInfo {
        Long getId();

        String getName();
    }

}