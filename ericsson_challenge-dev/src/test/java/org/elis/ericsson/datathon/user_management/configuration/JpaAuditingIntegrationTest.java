package org.elis.ericsson.datathon.user_management.configuration;

import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class JpaAuditingIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void whenEntityPersisted_thenCreatedAtIsPopulated() {
        Role role = new Role("ROLE_TEST");
        Role saved = entityManager.persistAndFlush(role);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void whenEntityUpdated_thenUpdatedAtChanges() {
        Role role = new Role("ROLE_TEST");
        Role saved = entityManager.persistAndFlush(role);

        saved.setName("ROLE_UPDATED");
        entityManager.persistAndFlush(saved);
        entityManager.clear();

        Role reloaded = entityManager.find(Role.class, saved.getId());
        assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(reloaded.getCreatedAt());
    }
}
