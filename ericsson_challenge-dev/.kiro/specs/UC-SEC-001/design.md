# UC-SEC-001 — Design Document

## Panoramica

Remediation di 12 vulnerabilità di sicurezza raggruppate in 4 aree di intervento:

1. **Secrets Management** (V-001, V-002)
2. **Access Control** (V-003, V-007)
3. **Input Validation & Information Disclosure** (V-004, V-006, V-010, V-011)
4. **Transport & Configuration Security** (V-005, V-008, V-009, V-012)

## 1. Secrets Management

### V-001: JWT Secret → Environment Variable

```java
// SecurityConstants.java — BEFORE:
public static final String JWT_SECRET = "hardcoded...";

// AFTER:
public static final String JWT_SECRET = System.getenv("JWT_SECRET") != null
    ? System.getenv("JWT_SECRET")
    : "dev-only-fallback-key-min-64-bytes-aaaaaaaaaaaaaaaaaaaaaaaaa";
```

- In `docker-compose.yml` aggiungere `JWT_SECRET` come env var del servizio `app`
- Aggiungere `JWT_SECRET` nel `.env.example`

### V-002: DB Password → Environment Variable in default profile

```properties
# application.properties — AFTER:
spring.datasource.password=${DB_PASSWORD:dev_password}
```

## 2. Access Control

### V-003: Disabilitare createFirstUser in produzione

Approccio: Proteggere con `@PreAuthorize("hasRole('ADMIN')")` + controllare che non ci siano utenti nel DB. In questo modo solo il primo avvio (con 0 utenti) permette la creazione senza auth, ma la password non sarà più hardcoded — sarà letta da env var.

```java
@PostMapping("/createFirstUser")
public ResponseEntity<?> createFirstUser(HttpServletRequest req) throws Exception {
    if (userProfileRepository.count() > 0) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Setup already completed");
    }
    String adminPassword = System.getenv("ADMIN_INITIAL_PASSWORD");
    if (adminPassword == null || adminPassword.isBlank()) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ADMIN_INITIAL_PASSWORD env not set");
    }
    // ... crea utente con adminPassword
}
```

### V-007: IDOR Protection su editProfile

```java
@PostMapping("/edit/{id}")
public String editProfile(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, ...) {
    if (!principal.getId().equals(id) && !principal.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
        throw new AccessDeniedException("Cannot edit another user's profile");
    }
    // ... proceed
}
```

## 3. Input Validation & Information Disclosure

### V-004: Rimuovere password da toString()

```java
// LoginDto.java
@Override
public String toString() {
    return "LoginDto{email='" + email + "'}";
}

// SignUpRequestDto.java
@Override
public String toString() {
    return "SignUpRequestDto{firstName='" + firstName + "', lastName='" + lastName + "', email='" + email + "'}";
}
```

### V-006: Validazione LoginDto

```java
public class LoginDto {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
}
```

### V-010: Password policy

```java
// SignUpRequestDto.java
@NotBlank
@Size(min = 8, message = "La password deve avere almeno 8 caratteri")
private String password;
```

### V-011: DTO di risposta per registrazione

L'endpoint `registerUser` restituirà `ResponseEntity<AuthResponseDTO>` invece di `ResponseEntity<UserProfile>`, riutilizzando il DTO esistente. In alternativa, ritornare solo HTTP 201 con l'id.

## 4. Transport & Configuration Security

### V-005: CORS restrittivo

```java
// CorsConfig.java
String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS") != null
    ? System.getenv("CORS_ALLOWED_ORIGINS")
    : "http://localhost:8080";
config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
```

### V-008: jjwt API moderna

```java
// Token generation:
Jwts.builder()
    .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()), SignatureAlgorithm.HS512)
    ...

// Token parsing:
Jwts.parserBuilder()
    .setSigningKey(Keys.hmacShaKeyFor(JWT_SECRET.getBytes()))
    .build()
    .parseClaimsJws(token)
```

### V-009: Security Headers

```java
// SecurityConfig.java — rimuovere:
http.headers().frameOptions().disable();

// Sostituire con:
http.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
);
```

### V-012: Token expiration

```java
// SecurityConstants.java
public static final int TOKEN_EXPIRATION = 15 * 60 * 1000; // 15 minuti
```

## File Modificati

| File | Modifiche |
|------|-----------|
| `constants/SecurityConstants.java` | JWT_SECRET da env, TOKEN_EXPIRATION 15min |
| `application.properties` | DB password da env |
| `docker-compose.yml` | Aggiungere JWT_SECRET, ADMIN_INITIAL_PASSWORD |
| `model/dto/LoginDto.java` | @NotBlank, @Email, toString senza password |
| `model/dto/request/SignUpRequestDto.java` | @Size(min=8), toString senza password |
| `configuration/CorsConfig.java` | Origin da env var |
| `configuration/SecurityConfig.java` | Headers, rimuovere frameOptions disable |
| `security/JwtUtility.java` | parserBuilder API |
| `service/impl/AuthServiceImpl.java` | createFirstUser password da env, registerUser response DTO |
| `controller/web/UserProfileWebController.java` | IDOR check su editProfile |
