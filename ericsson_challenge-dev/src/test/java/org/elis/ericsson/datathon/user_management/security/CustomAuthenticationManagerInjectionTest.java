package org.elis.ericsson.datathon.user_management.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CustomAuthenticationManagerInjectionTest {

    @Autowired
    private CustomAuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoderShouldBeInjectedFromContext() throws Exception {
        Field encoderField = CustomAuthenticationManager.class.getDeclaredField("passwordEncoder");
        encoderField.setAccessible(true);
        Object injectedEncoder = encoderField.get(authenticationManager);

        assertThat(injectedEncoder).isSameAs(passwordEncoder);
    }

    @Test
    void constructorShouldAcceptPasswordEncoderParameter() {
        var constructors = CustomAuthenticationManager.class.getConstructors();
        boolean hasEncoderParam = Arrays.stream(constructors)
                .anyMatch(c -> Arrays.asList(c.getParameterTypes()).contains(PasswordEncoder.class));

        assertThat(hasEncoderParam)
                .as("Constructor should accept PasswordEncoder parameter for injection")
                .isTrue();
    }
}
