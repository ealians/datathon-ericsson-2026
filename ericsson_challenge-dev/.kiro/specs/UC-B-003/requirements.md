# UC-B-003 – Bugfix: Problemi multipli in CustomAuthenticationManager

## Problema

La classe `CustomAuthenticationManager` presenta quattro difetti che violano le best practice Spring Boot, le convenzioni del progetto (constructor injection) e la correttezza del codice Java:

1. **PasswordEncoder non iniettato** — istanziato direttamente con `new BCryptPasswordEncoder()` anziché iniettato tramite il bean definito in `SecurityConfig.passwordEncoder()`
2. **Uso scorretto di `orElseGet`** — dopo il check `isEmpty()`, viene usato `userByEmail.orElseGet(userByEmail::get)` che è ridondante e semanticamente confuso; dovrebbe essere un semplice `.get()`
3. **Conversione a stringa fragile** — `authentication.getPrincipal() + ""` e `authentication.getCredentials() + ""` usano concatenazione anziché un cast esplicito a `String`
4. **Mancanza di `@Override`** — il metodo `authenticate` implementa l'interfaccia `AuthenticationManager` ma non è annotato con `@Override`

## Root Cause

| # | Componente | Stato | Problema |
|---|---|---|---|
| 1 | `PasswordEncoder passwordEncoder = new BCryptPasswordEncoder()` | ✗ **Errato** | Viola constructor injection; ignora il bean `SecurityConfig.passwordEncoder()` |
| 2 | `userByEmail.orElseGet(userByEmail::get)` | ✗ **Errato** | Ridondante: `orElseGet` invoca `get()` sull'Optional stesso, equivalente a un semplice `.get()` dopo il check `isEmpty()` |
| 3 | `authentication.getPrincipal() + ""` | ✗ **Fragile** | Se `getPrincipal()` è `null`, produce la stringa `"null"` anziché fallire esplicitamente |
| 4 | Metodo `authenticate` senza `@Override` | ✗ **Mancante** | Viola le best practice Java; impedisce il controllo a compile-time della firma |

---

## Modifiche richieste

### 1. Iniettare `PasswordEncoder` tramite constructor injection

**Prima (codice errato):**
```java
private final UserProfileRepository userProfileRepository;

final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

public CustomAuthenticationManager(UserProfileRepository userProfileRepository) {
    this.userProfileRepository = userProfileRepository;
}
```

**Dopo (codice corretto):**
```java
private final UserProfileRepository userProfileRepository;
private final PasswordEncoder passwordEncoder;

public CustomAuthenticationManager(UserProfileRepository userProfileRepository,
                                   PasswordEncoder passwordEncoder) {
    this.userProfileRepository = userProfileRepository;
    this.passwordEncoder = passwordEncoder;
}
```

**Motivazione**: Il bean `PasswordEncoder` è già definito in `SecurityConfig.passwordEncoder()`. Istanziarne uno nuovo viola il principio di Single Source of Truth, rende impossibile sostituire l'encoder nei test, e ignora eventuali personalizzazioni future del bean. La constructor injection è la convenzione del progetto (guardrails: "Use constructor injection (not field injection) for Spring beans").

---

### 2. Sostituire `orElseGet(userByEmail::get)` con `.get()`

**Prima (codice errato):**
```java
UserProfile user = userByEmail.orElseGet(userByEmail::get);
```

**Dopo (codice corretto):**
```java
UserProfile user = userByEmail.get();
```

**Motivazione**: Dopo il check `if (userByEmail.isEmpty()) { throw ... }`, l'Optional è garantito come presente. L'uso di `orElseGet(userByEmail::get)` è logicamente equivalente a `.get()` ma introduce confusione: `orElseGet` è pensato per fornire un valore alternativo, non per richiamare `.get()` sullo stesso Optional. Il codice diventa auto-referenziale e difficile da leggere.

---

### 3. Usare cast esplicito a `String` anziché concatenazione

**Prima (codice errato):**
```java
String email = authentication.getPrincipal() + "";
String password = authentication.getCredentials() + "";
```

**Dopo (codice corretto):**
```java
String email = (String) authentication.getPrincipal();
String password = (String) authentication.getCredentials();
```

**Motivazione**: La concatenazione con stringa vuota è un anti-pattern:
- Se `getPrincipal()` è `null`, produce `"null"` (stringa letterale) anziché lanciare un'eccezione esplicita
- Se il tipo di ritorno non è `String` (ad es. `UserDetails`), produce una rappresentazione `toString()` potenzialmente inutile
- Il cast esplicito fallisce in modo chiaro e immediato con `ClassCastException` se il tipo è inatteso, rendendo il debug più semplice
- Nel contesto di `UsernamePasswordAuthenticationToken`, principal e credentials sono sempre `String`

---

### 4. Aggiungere `@Override` al metodo `authenticate`

**Prima (codice errato):**
```java
public Authentication authenticate(Authentication authentication) throws AuthenticationException {
```

**Dopo (codice corretto):**
```java
@Override
public Authentication authenticate(Authentication authentication) throws AuthenticationException {
```

**Motivazione**: `@Override` garantisce il controllo a compile-time che la firma del metodo corrisponda a quella dell'interfaccia `AuthenticationManager`. Senza l'annotazione, un errore di battitura nella firma produrrebbe silenziosamente un metodo nuovo anziché un override, causando bug a runtime.

---

### Riepilogo: file corretto completo

**Path**: `src/main/java/org/elis/ericsson/datathon/user_management/security/CustomAuthenticationManager.java`

```java
package org.elis.ericsson.datathon.user_management.security;

import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomAuthenticationManager implements AuthenticationManager {

    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationManager(UserProfileRepository userProfileRepository,
                                       PasswordEncoder passwordEncoder) {
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        Optional<UserProfile> userByEmail = userProfileRepository.findByEmail(email);

        if (userByEmail.isEmpty()) {
            throw new BadCredentialsException("Email not found");
        }

        UserProfile user = userByEmail.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Password is incorrect");
        }
        return new UsernamePasswordAuthenticationToken(email, null, user.getAuthorities());
    }
}
```

**Import rimosso**: `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder` — non più necessario poiché l'encoder è iniettato dal contesto Spring.

---

## Specifiche di test

### Test 1: Avvio del contesto applicativo (regressione)

**Classe**: `EricssonDatathonProjectApplicationTests` (esistente)  
**Metodo**: `contextLoads()`  
**Scopo**: Verificare che il contesto Spring si avvii senza errori dopo il refactoring del costruttore.  
**Comportamento atteso**: Il test passa senza eccezioni (il bean `PasswordEncoder` viene iniettato correttamente).

### Test 2: Autenticazione con credenziali valide

**Classe**: `src/test/java/org/elis/ericsson/datathon/user_management/security/CustomAuthenticationManagerTest.java`

```java
package org.elis.ericsson.datathon.user_management.security;

import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.elis.ericsson.datathon.user_management.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationManagerTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CustomAuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        authenticationManager = new CustomAuthenticationManager(userProfileRepository, passwordEncoder);
    }

    @Test
    void whenValidCredentials_thenReturnsAuthentication() {
        String email = "user@example.com";
        String rawPassword = "password123";
        String encodedPassword = "$2a$10$encodedHash";

        UserProfile user = new UserProfile();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setRoles(Set.of(new Role("ROLE_USER")));

        when(userProfileRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        Authentication request = new UsernamePasswordAuthenticationToken(email, rawPassword);
        Authentication result = authenticationManager.authenticate(request);

        assertThat(result).isNotNull();
        assertThat(result.getPrincipal()).isEqualTo(email);
        assertThat(result.getAuthorities()).isNotEmpty();
    }

    @Test
    void whenEmailNotFound_thenThrowsBadCredentials() {
        String email = "unknown@example.com";
        when(userProfileRepository.findByEmail(email)).thenReturn(Optional.empty());

        Authentication request = new UsernamePasswordAuthenticationToken(email, "password");

        assertThatThrownBy(() -> authenticationManager.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email not found");
    }

    @Test
    void whenPasswordIncorrect_thenThrowsBadCredentials() {
        String email = "user@example.com";
        String rawPassword = "wrongPassword";
        String encodedPassword = "$2a$10$encodedHash";

        UserProfile user = new UserProfile();
        user.setEmail(email);
        user.setPassword(encodedPassword);

        when(userProfileRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        Authentication request = new UsernamePasswordAuthenticationToken(email, rawPassword);

        assertThatThrownBy(() -> authenticationManager.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Password is incorrect");
    }
}
```

### Test 3: Verifica che il PasswordEncoder è iniettato (non istanziato internamente)

**Classe**: `src/test/java/org/elis/ericsson/datathon/user_management/security/CustomAuthenticationManagerInjectionTest.java`

```java
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
```

### Test 4: Verifica che il metodo authenticate ha @Override

**Classe**: `src/test/java/org/elis/ericsson/datathon/user_management/security/CustomAuthenticationManagerContractTest.java`

```java
package org.elis.ericsson.datathon.user_management.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticationManagerContractTest {

    @Test
    void authenticateMethodShouldHaveOverrideAnnotation() throws NoSuchMethodException {
        Method authenticateMethod = CustomAuthenticationManager.class.getMethod(
                "authenticate", Authentication.class);

        assertThat(authenticateMethod.getAnnotation(Override.class))
                .as("authenticate() should be annotated with @Override")
                .isNotNull();
    }
}
```

---

## Criteri di accettazione

| # | Criterio | Verifica |
|---|---|---|
| AC-1 | `PasswordEncoder` è iniettato via constructor injection (non istanziato con `new`) | Test `passwordEncoderShouldBeInjectedFromContext` → PASS |
| AC-2 | Il costruttore accetta `PasswordEncoder` come parametro | Test `constructorShouldAcceptPasswordEncoderParameter` → PASS |
| AC-3 | L'import `BCryptPasswordEncoder` è rimosso dal file | Ispezione codice |
| AC-4 | `userByEmail.orElseGet(userByEmail::get)` è sostituito con `userByEmail.get()` | Ispezione codice |
| AC-5 | La conversione `+ ""` è sostituita con cast esplicito `(String)` | Ispezione codice |
| AC-6 | Il metodo `authenticate` ha l'annotazione `@Override` | Test `authenticateMethodShouldHaveOverrideAnnotation` → PASS |
| AC-7 | L'autenticazione con credenziali valide restituisce un token con le authorities corrette | Test `whenValidCredentials_thenReturnsAuthentication` → PASS |
| AC-8 | L'autenticazione con email inesistente lancia `BadCredentialsException` | Test `whenEmailNotFound_thenThrowsBadCredentials` → PASS |
| AC-9 | L'autenticazione con password errata lancia `BadCredentialsException` | Test `whenPasswordIncorrect_thenThrowsBadCredentials` → PASS |
| AC-10 | Il test `contextLoads()` esistente passa senza errori | `mvn test -Dtest=EricssonDatathonProjectApplicationTests` → BUILD SUCCESS |
| AC-11 | Nessuna regressione sui test esistenti | `mvn test` → tutti i test pre-esistenti passano |
| AC-12 | Nessuna nuova dipendenza Maven aggiunta | `pom.xml` invariato |

---

## Note implementative

- La fix modifica un solo file di produzione: `CustomAuthenticationManager.java`
- Nessun file nuovo di produzione
- Nessuna modifica al `pom.xml`
- Il bean `PasswordEncoder` già esiste in `SecurityConfig.passwordEncoder()` — il refactoring lo riutilizza correttamente
- La modifica al costruttore è trasparente: Spring Boot risolve automaticamente il bean `PasswordEncoder` tramite auto-wiring del costruttore
- Compatibile con entrambi i profili (H2 dev, PostgreSQL docker)
- Nessuna modifica alla logica di business (il comportamento del metodo `authenticate` rimane identico)
- Segue le guardrails del progetto: constructor injection, nessuna dipendenza aggiunta, codice nel package corretto
