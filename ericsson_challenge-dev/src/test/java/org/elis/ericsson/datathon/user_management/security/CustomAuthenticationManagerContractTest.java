package org.elis.ericsson.datathon.user_management.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticationManagerContractTest {

    @Test
    void authenticateMethodShouldOverrideInterfaceMethod() throws NoSuchMethodException {
        // Verify that CustomAuthenticationManager declares authenticate(Authentication)
        // and that it properly overrides AuthenticationManager.authenticate(Authentication)
        Method classMethod = CustomAuthenticationManager.class.getDeclaredMethod(
                "authenticate", Authentication.class);

        Method interfaceMethod = AuthenticationManager.class.getDeclaredMethod(
                "authenticate", Authentication.class);

        // The class method should exist and have the same signature as the interface method
        assertThat(classMethod)
                .as("CustomAuthenticationManager should declare authenticate() method")
                .isNotNull();

        assertThat(classMethod.getReturnType())
                .as("authenticate() should return Authentication (matching interface contract)")
                .isEqualTo(interfaceMethod.getReturnType());

        // Verify the class actually implements AuthenticationManager
        assertThat(AuthenticationManager.class.isAssignableFrom(CustomAuthenticationManager.class))
                .as("CustomAuthenticationManager should implement AuthenticationManager")
                .isTrue();
    }

    @Test
    void authenticateMethodShouldHaveOverrideAnnotationInSource() throws Exception {
        // @Override has SOURCE retention so it's not available at runtime via reflection.
        // Instead, we verify the contract is fulfilled: the method exists in the interface
        // and the class properly implements it. The @Override annotation in the source code
        // ensures compile-time verification of the contract.
        Method method = CustomAuthenticationManager.class.getMethod("authenticate", Authentication.class);

        // Verify the declaring class is CustomAuthenticationManager (it overrides, not just inherits)
        assertThat(method.getDeclaringClass())
                .as("authenticate() should be declared (overridden) in CustomAuthenticationManager, not just inherited")
                .isEqualTo(CustomAuthenticationManager.class);
    }
}
