package org.elis.ericsson.datathon.user_management.model.entity;

import org.elis.ericsson.datathon.user_management.configuration.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class RoleEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void roleClassShouldNotHaveEnableAutoConfiguration() {
        assertThat(Role.class.getAnnotation(EnableAutoConfiguration.class)).isNull();
    }

    @Test
    void whenRolePersisted_thenAuditFieldsArePopulated() {
        Role role = new Role("ROLE_TEST");
        Role saved = entityManager.persistAndFlush(role);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
