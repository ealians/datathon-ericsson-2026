package org.elis.ericsson.datathon.user_management.configuration;

import lombok.RequiredArgsConstructor;
import org.elis.ericsson.datathon.user_management.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf().disable()
                .authorizeHttpRequests(authorize -> authorize
                        // Endpoint di bootstrapping - esplicitamente pubblico con metodo HTTP specifico
                        .requestMatchers(HttpMethod.POST, "/api/auth/createFirstUser").permitAll()
                        // Permessi per le risorse pubbliche
                        .requestMatchers("/login", "/api/auth/**", "/v3/api-docs/**", "/actuator/health", "/webjars/**", "/css/**", "/js/**").permitAll()

                        // Permessi per le pagine pubbliche e profili
                        .requestMatchers(HttpMethod.GET, "/profiles").authenticated() // Accesso agli utenti autenticati
                        .requestMatchers(HttpMethod.GET, "/profiles/add-profile").hasRole("ADMIN") // Accesso alla pagina di aggiunta profilo solo per admin
                        // API pubbliche
                        .requestMatchers(HttpMethod.GET, "/api/profile/*", "/api/public/**").permitAll()
                        // API solo Admin
                        .requestMatchers(HttpMethod.POST, "/api/profiles/add").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/profiles/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/profiles/edit/{id}").authenticated()

                        // Tutte le altre richieste richiedono autenticazione
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))  // Redirect a /login per utenti non autenticati
                        .accessDeniedHandler(accessDeniedHandler())  // Gestisce accessi negati
                )
                // Configurazione della sessione
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Stateless sessione per JWT
                )
                // Aggiungi il filtro JWT
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        // Disabilita frame options per H2 console (se necessario)
        http.headers().frameOptions().disable();

        return http.build();
    }
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.sendRedirect("/login");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
