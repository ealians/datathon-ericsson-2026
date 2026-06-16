package org.elis.ericsson.datathon.user_management.model.entity;

import org.elis.ericsson.datathon.user_management.configuration.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class RefreshTokenEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void refreshTokenClassShouldNotHaveEnableAutoConfiguration() {
        assertThat(RefreshToken.class.getAnnotation(EnableAutoConfiguration.class)).isNull();
    }

    @Test
    void whenRefreshTokenPersisted_thenAuditFieldsArePopulated() {
        UserProfile user = new UserProfile();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPassword("encoded_password");
        UserProfile savedUser = entityManager.persistAndFlush(user);

        RefreshToken token = new RefreshToken();
        token.setUser(savedUser);
        token.setToken("test-refresh-token-123");
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        RefreshToken saved = entityManager.persistAndFlush(token);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
