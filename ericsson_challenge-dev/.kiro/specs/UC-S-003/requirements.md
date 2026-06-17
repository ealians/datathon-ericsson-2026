# UC-S-003 – Bugfix: Endpoint createFirstUser non esplicitamente pubblico

## Requisito funzionale

Al primo avvio dell'applicazione (database vuoto), l'endpoint `POST /api/auth/createFirstUser` deve essere accessibile **senza autenticazione** e deve creare un utente amministratore con le seguenti credenziali:

| Campo | Valore |
|---|---|
| Email | `admin@elis.org` |
| Password | `password` |
| Ruolo | ADMIN |

## Problema

L'endpoint `POST /api/auth/createFirstUser` è un endpoint di bootstrapping critico: consente di creare il primo utente admin quando il database è vuoto. Attualmente, il suo accesso pubblico dipende **implicitamente** dal pattern wildcard `/api/auth/**` → `permitAll()` nella `SecurityConfig`. Questa configurazione è fragile e presenta diversi problemi architetturali e di sicurezza.

Inoltre, il `JwtAuthenticationFilter` non implementa `shouldNotFilter()`, il che significa che viene eseguito su **tutte** le richieste, comprese quelle destinate a endpoint pubblici. Sebbene il filtro non blocchi attivamente le richieste senza token, genera log di warning inutili e aggiunge elaborazione non necessaria.

## Root Cause

| Componente | Stato | Problema |
|---|---|---|
| `SecurityConfig` → `permitAll()` per `/api/auth/**` | ✓ Presente | Accesso pubblico implicito via wildcard, non esplicito per `createFirstUser` |
| `JwtAuthenticationFilter` → `shouldNotFilter()` | ✗ **Mancante** | Il filtro JWT viene eseguito anche sugli endpoint pubblici |
| `AuthController` → dichiarazione `createFirstUser` | ✗ **Mancante** | L'endpoint esiste solo nell'implementazione, non nell'interfaccia |
| `Endpoints.java` → costante per il path | ✗ **Mancante** | Nessuna costante dedicata per il path `createFirstUser` |

## Analisi del flusso di sicurezza attuale

### Scenario: `POST /api/auth/createFirstUser` senza token JWT

1. **JwtAuthenticationFilter** (`doFilterInternal`):
   - `getJwtFromRequest(request)` → restituisce `null` (nessun header Authorization)
   - `getJwtFromCookie(request)` → restituisce `null` (nessun cookie authToken)
   - `StringUtils.hasText(null)` → `false` → salta il blocco di autenticazione
   - Logga warning: *"JWT token is either missing or invalid"*
   - Chiama `filterChain.doFilter(request, response)` → la richiesta prosegue

2. **AuthorizationFilter** (Spring Security):
   - Verifica se `/api/auth/createFirstUser` corrisponde a un matcher `permitAll()`
   - Il pattern `/api/auth/**` corrisponde → la richiesta è consentita
   - ⚠️ Se il pattern wildcard venisse modificato, l'endpoint diventerebbe silenziosamente protetto

3. **Rischio con `LoginUrlAuthenticationEntryPoint("/login")`**:
   - Se per qualsiasi ragione il match fallisse, la regola `anyRequest().authenticated()` si attiverebbe
   - Il client REST riceverebbe un **302 redirect a `/login`** invece di un 401 JSON
   - Questo renderebbe impossibile il bootstrap dell'applicazione via API

## Impatto

- **Bootstrap impossibile**: Se il wildcard pattern viene rimosso o modificato, non è possibile creare il primo utente
- **Log inquinati**: Warning inutile nel `JwtAuthenticationFilter` ad ogni chiamata senza token su endpoint pubblici
- **Fragilità architetturale**: L'endpoint non è dichiarato nell'interfaccia `AuthController`, violando il pattern contract-first del progetto
- **Nessuna costante dedicata**: Il path è hardcodato come stringa inline, violando le convenzioni sulle costanti

---

## Modifiche richieste

### 1. Aggiungere costante in `Endpoints.java`

**Path**: `src/main/java/org/elis/ericsson/datathon/user_management/constants/Endpoints.java`

```java
package org.elis.ericsson.datathon.user_management.constants;

public class Endpoints {
    private Endpoints() {
    }

    public static final String AUTH = "/auth";
    public static final String API = "/api";
    public static final String PROFILE = "/profiles";
    public static final String CREATE_FIRST_USER = "/createFirstUser";
}
```

### 2. Dichiarare il metodo nell'interfaccia `AuthController`

**Path**: `src/main/java/org/elis/ericsson/datathon/user_management/controller/AuthController.java`

Aggiungere la dichiarazione del metodo nell'interfaccia:

```java
@PostMapping("/createFirstUser")
ResponseEntity<?> createFirstUser(HttpServletRequest req) throws Exception;
```

### 3. Aggiungere `shouldNotFilter` al `JwtAuthenticationFilter`

**Path**: `src/main/java/org/elis/ericsson/datathon/user_management/security/JwtAuthenticationFilter.java`

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.equals("/api/auth/createFirstUser")
        || path.equals("/api/auth/login")
        || path.equals("/login")
        || path.startsWith("/webjars/")
        || path.startsWith("/css/")
        || path.startsWith("/js/");
}
```

**Motivazione**: Evitare l'esecuzione del filtro JWT su endpoint che non richiedono autenticazione. Questo elimina i log di warning spurii e riduce l'elaborazione non necessaria.

### 4. Rendere esplicito il `permitAll` per `createFirstUser` nella `SecurityConfig`

**Path**: `src/main/java/org/elis/ericsson/datathon/user_management/configuration/SecurityConfig.java`

Modificare la regola `requestMatchers` aggiungendo il path esplicito:

```java
.authorizeHttpRequests(authorize -> authorize
    // Endpoint di bootstrapping - deve essere esplicitamente pubblico
    .requestMatchers(HttpMethod.POST, "/api/auth/createFirstUser").permitAll()
    // Permessi per le risorse pubbliche
    .requestMatchers("/login", "/api/auth/**", "/v3/api-docs/**", "/actuator/health", "/webjars/**", "/css/**", "/js/**").permitAll()
    // ... resto della configurazione invariato
)
```

**Motivazione**: Dichiarare esplicitamente il `permitAll` con metodo HTTP specifico (`POST`) rende la configurazione auto-documentante e resistente a future modifiche del wildcard pattern.

---

## Specifiche di test

### Test 1: Accesso pubblico a createFirstUser senza autenticazione

**Classe**: `src/test/java/org/elis/ericsson/datathon/user_management/bugfix/CreateFirstUserPublicAccessTest.java`

```java
package org.elis.ericsson.datathon.user_management.bugfix;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CreateFirstUserPublicAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createFirstUser_withoutAuthentication_shouldNotReturn401Or302() throws Exception {
        mockMvc.perform(post("/api/auth/createFirstUser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void createFirstUser_withoutAuthentication_shouldNotRedirectToLogin() throws Exception {
        mockMvc.perform(post("/api/auth/createFirstUser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(org.hamcrest.Matchers.not(302)));
    }
}
```

### Test 2: Verifica che il JwtAuthenticationFilter non processi endpoint pubblici

**Classe**: `src/test/java/org/elis/ericsson/datathon/user_management/bugfix/JwtFilterSkipPublicEndpointsTest.java`

```java
package org.elis.ericsson.datathon.user_management.bugfix;

import jakarta.servlet.http.HttpServletRequest;
import org.elis.ericsson.datathon.user_management.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtFilterSkipPublicEndpointsTest {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldNotFilter_createFirstUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/auth/createFirstUser");

        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_login() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/auth/login");

        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldFilter_protectedEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/profiles/add");

        assertThat(jwtAuthenticationFilter.shouldNotFilter(request)).isFalse();
    }
}
```

---

## Criteri di accettazione

| # | Criterio | Verifica |
|---|---|---|
| AC-1 | L'endpoint `POST /api/auth/createFirstUser` è accessibile senza token JWT e restituisce 200 OK (con DB vuoto) | Test `createFirstUser_withoutAuthentication_shouldNotReturn401Or302` → PASS |
| AC-2 | L'endpoint NON restituisce un 302 redirect a `/login` quando chiamato senza autenticazione | Test `createFirstUser_withoutAuthentication_shouldNotRedirectToLogin` → PASS |
| AC-3 | Il `JwtAuthenticationFilter` non viene eseguito per il path `/api/auth/createFirstUser` | Test `shouldNotFilter_createFirstUser` → PASS |
| AC-4 | Il `JwtAuthenticationFilter` continua a essere eseguito per endpoint protetti | Test `shouldFilter_protectedEndpoint` → PASS |
| AC-5 | L'endpoint è dichiarato nell'interfaccia `AuthController` | Verifica che `AuthController.java` contenga la firma del metodo |
| AC-6 | La costante `CREATE_FIRST_USER` è presente in `Endpoints.java` | Verifica path file |
| AC-7 | Il `SecurityConfig` dichiara esplicitamente `permitAll()` per `POST /api/auth/createFirstUser` | Verifica configurazione |
| AC-8 | Nessuna regressione sui test esistenti | `mvn test` → tutti i test pre-esistenti passano |
| AC-9 | Nessuna nuova dipendenza Maven aggiunta | `pom.xml` invariato |

---

## Note implementative

- La fix è minimale e non invasiva: modifica 4 file esistenti senza aggiungerne di nuovi (escludendo i test)
- Nessuna modifica al `pom.xml`
- Compatibile con entrambi i profili (H2 dev, PostgreSQL docker)
- Segue il pattern architetturale del progetto (controller interface → impl, costanti in `Endpoints.java`)
- La regola esplicita `permitAll()` nel `SecurityConfig` è posizionata PRIMA del wildcard `/api/auth/**` per chiarezza e precedenza
- Il `shouldNotFilter` include anche `/api/auth/login` e risorse statiche per coerenza
