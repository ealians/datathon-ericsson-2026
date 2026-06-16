package org.elis.ericsson.datathon.user_management.configuration;

import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserProfileAuditTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void whenUserProfilePersisted_thenAuditFieldsAreSet() {
        UserProfile user = new UserProfile();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPassword("encoded_password");

        UserProfile saved = entityManager.persistAndFlush(user);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
