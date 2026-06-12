package org.elis.ericsson.datathon.user_management.model.projection;

import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.model.entity.Role;

import java.util.Collection;

/**
 * A Projection for the {@link UserProfile} entity
 */
public interface UserDetailInfo {
    Long getId();

    String getFirstName();

    String getLastName();

    String getEmail();


    Boolean getCredentialExpired();

    Collection<RoleInfo> getRoles();



    /**
     * Projection for {@link Role}
     */
    interface RoleInfo {
        Long getId();

        String getName();
    }

}