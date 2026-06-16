package org.elis.ericsson.datathon.user_management.model.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class NoEnableAutoConfigOnEntitiesTest {

    @ParameterizedTest(name = "Entity {0} should not have @EnableAutoConfiguration")
    @ValueSource(classes = {
            Role.class,
            RefreshToken.class,
            UserProfile.class,
            PasswordResetToken.class
    })
    void entityShouldNotHaveEnableAutoConfiguration(Class<?> entityClass) {
        assertThat(entityClass.getAnnotation(EnableAutoConfiguration.class))
                .as("@EnableAutoConfiguration should not be present on %s", entityClass.getSimpleName())
                .isNull();
    }
}
