# UC-S-003 – Bugfix Design: Endpoint createFirstUser non esplicitamente pubblico

## Overview

L'endpoint `POST /api/auth/createFirstUser` è un endpoint di bootstrapping critico che consente la creazione del primo utente amministratore quando il database è vuoto. Attualmente il suo accesso pubblico dipende implicitamente dal pattern wildcard `/api/auth/**` → `permitAll()`, il che è fragile. Inoltre, il `JwtAuthenticationFilter` viene eseguito inutilmente su endpoint pubblici generando log di warning spurii, il metodo non è dichiarato nell'interfaccia `AuthController` (violando il pattern contract-first), e non esiste una costante dedicata in `Endpoints.java`.

La fix prevede 4 modifiche minimali e non invasive su file esistenti per rendere l'accesso pubblico esplicito, eliminare log inutili, ripristinare la conformità al pattern architetturale e centralizzare la costante del path.

## Glossary

- **Bug_Condition (C)**: La condizione che causa il problema — l'endpoint `createFirstUser` è pubblico solo implicitamente via wildcard, il filtro JWT viene eseguito inutilmente, e il contratto interfaccia è incompleto
- **Property (P)**: Il comportamento desiderato — accesso pubblico esplicito con `permitAll()` dedicato, filtro JWT bypassato su endpoint pubblici, contratto interfaccia completo
- **Preservation**: Il comportamento esistente che deve rimanere invariato — autenticazione JWT su endpoint protetti, mouse-click/API flow per login/signup/logout, autorizzazione basata su ruoli
- **JwtAuthenticationFilter**: Il filtro in `security/JwtAuthenticationFilter.java` che intercetta ogni richiesta per estrarre e validare il token JWT
- **SecurityConfig**: La classe in `configuration/SecurityConfig.java` che definisce la catena di filtri di sicurezza e le regole di autorizzazione HTTP
- **AuthController**: L'interfaccia in `controller/AuthController.java` che definisce il contratto per tutti gli endpoint di autenticazione
- **Endpoints**: La classe di costanti in `constants/Endpoints.java` che centralizza i path degli endpoint

## Bug Details

### Bug Condition

Il bug si manifesta quando un client invoca `POST /api/auth/createFirstUser` senza token JWT. L'endpoint è accessibile solo grazie al wildcard `/api/auth/**` → `permitAll()`. Se quel wildcard venisse rimosso o ristretto, l'endpoint diventerebbe silenziosamente protetto, rendendo impossibile il bootstrap dell'applicazione. Inoltre, ad ogni invocazione senza token, il `JwtAuthenticationFilter` logga un warning inutile.

**Formal Specification:**
```
FUNCTION isBugCondition(request)
  INPUT: request of type HttpServletRequest
  OUTPUT: boolean
  
  RETURN request.method == "POST"
         AND request.path == "/api/auth/createFirstUser"
         AND request.header("Authorization") IS NULL
         AND request.cookie("authToken") IS NULL
         AND (
           explicitPermitAllRule(request.path) NOT EXISTS
           OR jwtFilterExecutesOn(request.path) == TRUE
           OR interfaceDeclaration("createFirstUser") NOT EXISTS
           OR endpointConstant("CREATE_FIRST_USER") NOT EXISTS
         )
END FUNCTION
```

### Examples

- **Esempio 1**: `POST /api/auth/createFirstUser` senza token → attualmente funziona grazie al wildcard, ma il `JwtAuthenticationFilter` logga `"JWT token is either missing or invalid"` → comportamento indesiderato (log spurio)
- **Esempio 2**: Se il wildcard `/api/auth/**` venisse cambiato in `/api/auth/login` → `createFirstUser` riceve un **302 redirect a `/login`** → bootstrap impossibile
- **Esempio 3**: Un nuovo sviluppatore cerca `createFirstUser` nell'interfaccia `AuthController` → non lo trova → assume che non esista → viola il principio contract-first
- **Edge case**: `GET /api/auth/createFirstUser` (metodo HTTP sbagliato) → deve restituire 405 Method Not Allowed, non 200

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- L'autenticazione JWT su tutti gli endpoint protetti (`/api/profiles/*`, `/profiles/*`) deve continuare a funzionare esattamente come prima
- Il login (`POST /api/auth/login`), signup (`POST /api/auth/signup`), logout (`POST /api/auth/logout`) devono continuare a funzionare
- Il refresh token (`POST /api/auth/refreshToken`) deve continuare a funzionare
- La protezione basata su ruoli (`@PreAuthorize`) deve continuare a funzionare
- Il redirect a `/login` per utenti non autenticati su endpoint protetti deve continuare a funzionare
- Le risorse statiche (`/webjars/**`, `/css/**`, `/js/**`) devono rimanere accessibili

**Scope:**
Tutte le richieste che NON coinvolgono l'endpoint `POST /api/auth/createFirstUser` devono essere completamente non influenzate dalla fix. Questo include:
- Tutte le operazioni CRUD sui profili
- Tutte le operazioni di autenticazione esistenti (login, signup, logout, refresh)
- Accesso alle pagine Thymeleaf
- Accesso alle risorse statiche
- Accesso agli endpoint di documentazione API (`/v3/api-docs/**`)

## Hypothesized Root Cause

Basandosi sull'analisi del codice sorgente, le cause radice sono:

1. **Assenza di regola `permitAll()` esplicita**: In `SecurityConfig.java` (linea 34), l'endpoint `createFirstUser` è coperto solo dal wildcard `/api/auth/**`. Non esiste una regola esplicita `requestMatchers(HttpMethod.POST, "/api/auth/createFirstUser").permitAll()`. Questo rende l'accesso fragile e implicito.

2. **Assenza di `shouldNotFilter()` nel `JwtAuthenticationFilter`**: La classe `JwtAuthenticationFilter` (linea 29) estende `OncePerRequestFilter` ma non override `shouldNotFilter()`. Di conseguenza, `doFilterInternal` viene invocato su TUTTE le richieste, incluse quelle destinate a endpoint pubblici. Alla linea 44, quando non c'è token, viene loggato un warning inutile.

3. **Metodo non dichiarato nell'interfaccia**: `AuthController.java` dichiara 7 metodi (login, signup, logout, refreshToken, tokenResetPassword, getPossibleRoles, getSession) ma NON `createFirstUser`. Il metodo esiste solo in `AuthControllerImpl.java` (linea 97), violando il pattern contract-first del progetto.

4. **Assenza di costante in `Endpoints.java`**: La classe `Endpoints.java` definisce `AUTH`, `API`, `PROFILE` ma non ha una costante per il path `createFirstUser`. Il path è hardcodato come stringa inline nell'annotazione `@PostMapping("/createFirstUser")`.

## Correctness Properties

Property 1: Bug Condition - Accesso pubblico esplicito a createFirstUser

_For any_ richiesta `POST /api/auth/createFirstUser` senza token JWT, il sistema con la fix applicata SHALL consentire l'accesso senza autenticazione tramite una regola `permitAll()` esplicita (non dipendente dal wildcard), il `JwtAuthenticationFilter` SHALL essere bypassato (nessun log di warning generato), e il metodo SHALL essere dichiarato nell'interfaccia `AuthController`.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

Property 2: Preservation - Comportamento invariato su endpoint protetti

_For any_ richiesta destinata a un endpoint protetto (non pubblico), il sistema con la fix applicata SHALL comportarsi esattamente come il sistema originale: il `JwtAuthenticationFilter` SHALL essere eseguito, l'autenticazione JWT SHALL essere richiesta, e le regole di autorizzazione basate su ruoli SHALL essere applicate identicamente.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

## Fix Implementation

### Changes Required

Assumendo che la nostra analisi delle cause radice sia corretta:

**File 1**: `src/main/java/org/elis/ericsson/datathon/user_management/constants/Endpoints.java`

**Modifica**: Aggiungere la costante `CREATE_FIRST_USER`

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

---

**File 2**: `src/main/java/org/elis/ericsson/datathon/user_management/controller/AuthController.java`

**Modifica**: Aggiungere la dichiarazione del metodo `createFirstUser` nell'interfaccia

```java
@PostMapping("/createFirstUser")
ResponseEntity<?> createFirstUser(HttpServletRequest req) throws Exception;
```

---

**File 3**: `src/main/java/org/elis/ericsson/datathon/user_management/security/JwtAuthenticationFilter.java`

**Modifica**: Aggiungere override di `shouldNotFilter()` per bypassare endpoint pubblici

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.equals("/api/auth/createFirstUser")
        || path.equals("/api/auth/login")
        || path.equals("/api/auth/signup")
        || path.equals("/api/auth/refreshToken")
        || path.equals("/login")
        || path.startsWith("/webjars/")
        || path.startsWith("/css/")
        || path.startsWith("/js/");
}
```

**Motivazione**: Evitare l'esecuzione del filtro JWT su endpoint che per definizione non richiedono autenticazione. Questo elimina i log di warning spurii (`"JWT token is either missing or invalid"`) e riduce l'elaborazione non necessaria su richieste pubbliche.

---

**File 4**: `src/main/java/org/elis/ericsson/datathon/user_management/configuration/SecurityConfig.java`

**Modifica**: Aggiungere regola esplicita `permitAll()` con vincolo di metodo HTTP PRIMA del wildcard

```java
.authorizeHttpRequests(authorize -> authorize
    // Endpoint di bootstrapping - esplicitamente pubblico con metodo HTTP specifico
    .requestMatchers(HttpMethod.POST, "/api/auth/createFirstUser").permitAll()
    // Permessi per le risorse pubbliche (wildcard esistente mantenuto per retrocompatibilità)
    .requestMatchers("/login", "/api/auth/**", "/v3/api-docs/**", "/actuator/health", "/webjars/**", "/css/**", "/js/**").permitAll()
    // ... resto della configurazione invariato
)
```

**Motivazione**: Dichiarare esplicitamente il `permitAll()` con metodo HTTP specifico (`POST`) rende la configurazione auto-documentante e resistente a future modifiche del wildcard pattern. La regola DEVE precedere il wildcard per chiarezza semantica e precedenza nella catena di Spring Security.

### Ordine di applicazione consigliato

1. `Endpoints.java` → aggiungere costante (nessuna dipendenza)
2. `AuthController.java` → aggiungere dichiarazione metodo (nessuna dipendenza)
3. `JwtAuthenticationFilter.java` → aggiungere `shouldNotFilter()` (nessuna dipendenza)
4. `SecurityConfig.java` → aggiungere regola esplicita (nessuna dipendenza)
5. `AuthControllerImpl.java` → aggiungere `@Override` al metodo `createFirstUser` (dipende dal punto 2)

### Data Flow dopo la fix

```
Client → POST /api/auth/createFirstUser (no JWT)
  │
  ├─ JwtAuthenticationFilter.shouldNotFilter() → TRUE → filtro bypassato (no log warning)
  │
  ├─ AuthorizationFilter:
  │   └─ Match: requestMatchers(POST, "/api/auth/createFirstUser").permitAll() → CONSENTITO
  │
  └─ AuthControllerImpl.createFirstUser(req) → AuthService.createFirstUser(req) → 200 OK
```

## Testing Strategy

### Validation Approach

La strategia di test segue un approccio in due fasi: prima, verificare i counterexample che dimostrano il bug sul codice non fixato, poi verificare che la fix funzioni correttamente e preservi il comportamento esistente.

### Exploratory Bug Condition Checking

**Goal**: Verificare i counterexample che dimostrano il bug PRIMA di implementare la fix. Confermare o confutare l'analisi delle cause radice.

**Test Plan**: Scrivere test che verificano il comportamento dell'endpoint `createFirstUser` e del `JwtAuthenticationFilter` sul codice non fixato per osservare i fallimenti e comprendere la causa radice.

**Test Cases**:
1. **JwtFilter su createFirstUser**: Verificare che `shouldNotFilter()` non esiste (metodo non override) → il filtro viene eseguito su `/api/auth/createFirstUser` (counterexample sul codice non fixato)
2. **Interfaccia incompleta**: Verificare che `AuthController` non dichiara `createFirstUser` → violazione contract-first (counterexample sul codice non fixato)
3. **Costante mancante**: Verificare che `Endpoints.CREATE_FIRST_USER` non esiste → compilazione fallisce (counterexample sul codice non fixato)
4. **Regola esplicita mancante**: Verificare che non esiste una regola `requestMatchers(POST, "/api/auth/createFirstUser")` nel `SecurityConfig` (counterexample sul codice non fixato)

**Expected Counterexamples**:
- Il `JwtAuthenticationFilter` esegue `doFilterInternal` su `/api/auth/createFirstUser` e logga warning
- `AuthController.java` non contiene la firma `createFirstUser`
- `Endpoints.java` non contiene la costante `CREATE_FIRST_USER`
- `SecurityConfig.java` non ha regola esplicita per `createFirstUser`

### Fix Checking

**Goal**: Verificare che per tutti gli input dove la bug condition è vera, la funzione fixata produce il comportamento atteso.

**Pseudocode:**
```
FOR ALL request WHERE isBugCondition(request) DO
  result := securityChain_fixed(request)
  ASSERT result.status == 200
  ASSERT jwtFilter.shouldNotFilter(request) == TRUE
  ASSERT noWarningLogGenerated(request)
END FOR
```

### Preservation Checking

**Goal**: Verificare che per tutti gli input dove la bug condition NON è vera, la funzione fixata produce lo stesso risultato della funzione originale.

**Pseudocode:**
```
FOR ALL request WHERE NOT isBugCondition(request) DO
  ASSERT securityChain_original(request) == securityChain_fixed(request)
END FOR
```

**Testing Approach**: Il property-based testing è raccomandato per il preservation checking perché:
- Genera automaticamente molti casi di test sull'intero dominio di input
- Cattura edge case che i test unitari manuali potrebbero perdere
- Fornisce garanzie forti che il comportamento è invariato per tutti gli input non-buggy

**Test Plan**: Osservare il comportamento sul codice NON fixato per richieste a endpoint protetti, poi scrivere property-based test che catturino quel comportamento.

**Test Cases**:
1. **Preservation autenticazione**: Verificare che `POST /api/profiles/add` continua a richiedere autenticazione e ruolo ADMIN
2. **Preservation filtro JWT su endpoint protetti**: Verificare che `JwtAuthenticationFilter.shouldNotFilter()` restituisce `false` per path come `/api/profiles/add`
3. **Preservation redirect login**: Verificare che endpoint protetti senza token generano redirect a `/login`
4. **Preservation risorse statiche**: Verificare che `/webjars/**`, `/css/**`, `/js/**` rimangono accessibili

### Unit Tests

- Test `shouldNotFilter()` restituisce `true` per `/api/auth/createFirstUser`
- Test `shouldNotFilter()` restituisce `true` per `/api/auth/login`
- Test `shouldNotFilter()` restituisce `true` per `/login`
- Test `shouldNotFilter()` restituisce `true` per path che iniziano con `/webjars/`, `/css/`, `/js/`
- Test `shouldNotFilter()` restituisce `false` per `/api/profiles/add`
- Test `shouldNotFilter()` restituisce `false` per `/api/auth/me` (endpoint autenticato sotto auth)
- Test che `AuthController` dichiara il metodo `createFirstUser`
- Test che `Endpoints.CREATE_FIRST_USER` ha valore `"/createFirstUser"`

### Property-Based Tests

- Generare path casuali che matchano il pattern degli endpoint pubblici e verificare che `shouldNotFilter()` restituisce `true`
- Generare path casuali che NON matchano pattern pubblici e verificare che `shouldNotFilter()` restituisce `false`
- Generare richieste HTTP casuali a endpoint protetti e verificare che il comportamento di autorizzazione è identico pre/post fix

### Integration Tests

- Test end-to-end: `POST /api/auth/createFirstUser` senza token → 200 OK (con DB vuoto)
- Test end-to-end: `POST /api/auth/createFirstUser` senza token → nessun 302 redirect
- Test end-to-end: `POST /api/profiles/add` senza token → redirect a `/login` (preservation)
- Test end-to-end: `POST /api/profiles/add` con token ADMIN → 200 OK (preservation)
- Test che i log non contengono `"JWT token is either missing or invalid"` dopo chiamata a `createFirstUser`
