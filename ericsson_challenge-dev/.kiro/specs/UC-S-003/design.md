# Design Document

## UC-S-003 – Bugfix: Fallimento endpoint `createFirstUser`

## Overview

Questo documento descrive il design tecnico per risolvere il fallimento dell'endpoint `POST /api/auth/createFirstUser`. I fix si concentrano su due aree: iniezione corretta del `PasswordEncoder` (DI) e prevenzione della serializzazione circolare/esposizione password nella risposta JSON.

## Architecture

### Componenti coinvolti

```
┌─────────────────────────────────────┐
│         SecurityConfig              │
│  @Bean PasswordEncoder (BCrypt)     │
└──────────┬──────────────┬───────────┘
           │              │
     inject│        inject│
           ▼              ▼
┌──────────────────┐ ┌────────────────────────────┐
│  AuthServiceImpl │ │ CustomAuthenticationManager │
│  (uses DI bean)  │ │  (uses DI bean)            │
└────────┬─────────┘ └────────────────────────────┘
         │
         │ save + return
         ▼
┌──────────────────────────────────┐
│         UserProfile              │
│  @JsonIgnore password            │
│  @JsonIgnore eggUpInfo           │
└──────────────────────────────────┘
```

### Flusso corretto post-fix

```
POST /api/auth/createFirstUser
  → SecurityFilterChain: permitAll (/api/auth/**) ✅
  → JwtAuthenticationFilter: nessun token → continua ✅
  → AuthControllerImpl.createFirstUser()
  → AuthServiceImpl.createFirstUser()
    1. count() == 0 → procede
    2. Crea ruoli ROLE_ADMIN, ROLE_USER → save
    3. Crea UserProfile con passwordEncoder.encode("password")
       → passwordEncoder iniettato da SecurityConfig (DI) ✅
    4. ResponseEntity.ok(user) → Jackson serializza UserProfile
       → password: @JsonIgnore → esclusa ✅
       → eggUpInfo: @JsonIgnore → esclusa, nessun loop ✅
    5. Risposta 200 con JSON pulito (id, email, firstName, lastName, roles)
```

## Detailed Design

### Fix 1: Dependency Injection del PasswordEncoder

**Obiettivo**: Eliminare l'istanziazione locale di `BCryptPasswordEncoder` e usare il bean Spring definito in `SecurityConfig`.

#### AuthServiceImpl

**File**: `src/main/java/org/elis/ericsson/datathon/user_management/service/impl/AuthServiceImpl.java`

**Modifica**: Rimuovere qualsiasi istanziazione diretta `new BCryptPasswordEncoder()` e assicurare che il campo `passwordEncoder` sia:
- dichiarato `private final PasswordEncoder passwordEncoder`
- iniettato tramite costruttore `@Autowired`

```java
@Service
public class AuthServiceImpl implements AuthService {
    // ...altri campi...
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(
            // ...altri parametri...
            PasswordEncoder passwordEncoder) {
        // ...altre assegnazioni...
        this.passwordEncoder = passwordEncoder;
    }
}
```

#### CustomAuthenticationManager

**File**: `src/main/java/org/elis/ericsson/datathon/user_management/security/CustomAuthenticationManager.java`

**Modifica**: Stessa logica — il `PasswordEncoder` va iniettato via costruttore, non istanziato localmente.

```java
@Service
public class CustomAuthenticationManager implements AuthenticationManager {
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationManager(
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder) {
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }
}
```

**Impatto**: Nessun cambio funzionale (BCrypt è cross-istanza compatibile). Garantisce coerenza architetturale e singola fonte di configurazione per l'encoder.

---

### Fix 2: Annotazioni @JsonIgnore su UserProfile

**Obiettivo**: Prevenire l'esposizione della password e il loop di serializzazione bidirezionale nella risposta JSON.

**File**: `src/main/java/org/elis/ericsson/datathon/user_management/model/entity/UserProfile.java`

**Modifiche**:

| Campo | Annotazione | Motivo |
|-------|-------------|--------|
| `password` | `@JsonIgnore` | Mai esporre la password (anche se encodata) nelle risposte API |
| `eggUpInfo` | `@JsonIgnore` | Previene `StackOverflowError` da serializzazione circolare `UserProfile ↔ EggUpInfo` |

```java
@Column(name = "password")
@JsonIgnore
private String password;

@OneToOne(mappedBy = "creationUser")
@JsonIgnore
private EggUpInfo eggUpInfo;
```

**Risposta JSON risultante** (esempio):
```json
{
  "id": 1,
  "email": "admin@elis.org",
  "firstName": "firstName_admin",
  "lastName": "lastName_admin",
  "username": null,
  "phoneNumber": null,
  "roles": [
    { "id": 1, "name": "ROLE_ADMIN" },
    { "id": 2, "name": "ROLE_USER" }
  ],
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

---

## Testing Strategy

### Test di verifica

| Test | Tipo | Descrizione |
|------|------|-------------|
| `createFirstUser_success` | Integration | DB vuoto → POST → 200, JSON senza `password`/`eggUpInfo` |
| `createFirstUser_alreadyExists` | Integration | DB con utente → POST → exception con messaggio appropriato |
| `createFirstUser_rolesAssigned` | Integration | Verifica che `roles` contenga ROLE_ADMIN e ROLE_USER |
| `loginAfterCreate` | Integration (E2E) | createFirstUser → login con `admin@elis.org`/`password` → 200 + JWT |
| `passwordEncoder_isBeanInjected` | Unit | Verifica che `AuthServiceImpl` e `CustomAuthenticationManager` non istanziano `BCryptPasswordEncoder` direttamente |

### Proprietà di correttezza

1. **P1 – Password mai esposta**: Per qualsiasi risposta JSON contenente un `UserProfile`, il campo `password` NON DEVE essere presente
2. **P2 – Nessun loop di serializzazione**: La serializzazione di `UserProfile` DEVE terminare in tempo finito (nessun `StackOverflowError`)
3. **P3 – Singola fonte encoder**: Il `PasswordEncoder` usato in tutto il sistema DEVE essere lo stesso bean definito in `SecurityConfig`
4. **P4 – Compatibilità login**: Dopo `createFirstUser`, `login("admin@elis.org", "password")` DEVE ritornare un JWT valido

---

## File modificati (riepilogo)

| File | Modifica |
|------|----------|
| `service/impl/AuthServiceImpl.java` | Iniezione DI del `PasswordEncoder` (rimozione `new BCryptPasswordEncoder()`) |
| `security/CustomAuthenticationManager.java` | Iniezione DI del `PasswordEncoder` (rimozione `new BCryptPasswordEncoder()`) |
| `model/entity/UserProfile.java` | Aggiunta `@JsonIgnore` su `password` e `eggUpInfo` |

---

## Rischi e mitigazioni

| Rischio | Probabilità | Mitigazione |
|---------|-------------|-------------|
| Circular dependency Spring (SecurityConfig → ... → AuthServiceImpl → PasswordEncoder) | Bassa | `PasswordEncoder` è un bean stateless senza dipendenze cicliche |
| Campi `@JsonIgnore` rompono altri endpoint che servono quei dati | Bassa | Verificato: nessun endpoint espone `password`; `eggUpInfo` è accessibile via endpoint dedicati |
| Regressione login | Bassa | BCrypt cross-istanza compatibile; test E2E di verifica |

---

## Decisioni di design

1. **Perché `@JsonIgnore` anziché un DTO dedicato**: Minimizza le modifiche. L'entità è già usata come risposta in `createFirstUser` e `registerUser`. Introdurre un DTO richiederebbe refactoring più ampio (out of scope).
2. **Perché DI anziché lasciare `new BCryptPasswordEncoder()`**: Anche se funzionalmente equivalente ora, il bean DI garantisce che un eventuale cambio di encoder in `SecurityConfig` si propaghi automaticamente a tutti i consumatori.

---

## Feature Aggiuntiva: Pulsante "Crea Primo Admin" nella pagina di login

### Overview

Aggiungere un pulsante "Crea Primo Admin" nella pagina di login con visibilità controllata da un nuovo endpoint `GET /api/auth/adminExists`. Il pulsante fornisce feedback inline tramite Bootstrap alert e si nasconde dopo la creazione riuscita o se un admin esiste già.

### Architettura

```
┌──────────────────────────────────────────────────────────┐
│                    login.html (Thymeleaf)                 │
│  ┌─────────────────────────────────────────────────────┐ │
│  │  Bootstrap Card                                     │ │
│  │  ┌───────────────────────────────────────────────┐  │ │
│  │  │  Login Form (esistente)                       │  │ │
│  │  │  [Login Button]                               │  │ │
│  │  ├───────────────────────────────────────────────┤  │ │
│  │  │  [Crea Primo Admin Button] (condizionale)     │  │ │
│  │  │  [Feedback Alert] (condizionale)              │  │ │
│  │  └───────────────────────────────────────────────┘  │ │
│  └─────────────────────────────────────────────────────┘ │
└────────────────┬─────────────────────┬───────────────────┘
                 │ on page load        │ on button click
                 ▼                     ▼
┌────────────────────────┐  ┌──────────────────────────────┐
│ GET /api/auth/          │  │ POST /api/auth/              │
│     adminExists         │  │      createFirstUser         │
└────────┬───────────────┘  └──────────────┬───────────────┘
         │                                  │
         ▼                                  ▼
┌──────────────────────────────────────────────────────────┐
│              AuthControllerImpl                           │
│              (esistente + nuovo metodo adminExists)       │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│              AuthServiceImpl                              │
│              (esistente + nuovo metodo adminExists)       │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│              UserProfileRepository                        │
│              (metodo count() esistente)                   │
└──────────────────────────────────────────────────────────┘
```

### Design Dettagliato

#### 1. Nuovo Endpoint Backend: `GET /api/auth/adminExists`

**Costante in Endpoints.java:**
```java
public static final String ADMIN_EXISTS = "/adminExists";
```

**Interfaccia AuthController:**
```java
@GetMapping("/adminExists")
ResponseEntity<Boolean> adminExists();
```

**Implementazione AuthControllerImpl:**
```java
@Override
@GetMapping("/adminExists")
public ResponseEntity<Boolean> adminExists() {
    return authService.adminExists();
}
```

**Interfaccia AuthService:**
```java
ResponseEntity<Boolean> adminExists();
```

**Implementazione AuthServiceImpl:**
```java
@Override
public ResponseEntity<Boolean> adminExists() {
    boolean exists = userProfileRepository.count() > 0;
    return ResponseEntity.ok(exists);
}
```

**Razionale**: Riusa la stessa logica `count() > 0` già presente in `createFirstUser`. L'endpoint è accessibile pubblicamente (coperto dalla regola `/api/auth/**` permitAll in SecurityConfig) e ritorna solo un booleano senza esporre dati sensibili.

#### 2. Modifiche Frontend: `login.html`

**HTML del pulsante** (dopo `</form>`, dentro `<div class="card-body">`):

```html
<!-- Pulsante Creazione Admin (nascosto di default, visibile solo se nessun admin esiste) -->
<button type="button" id="createAdminBtn" class="btn btn-success w-100 mt-3"
        style="display: none;" onclick="createFirstAdmin()">
    Crea Primo Admin
</button>

<!-- Container Feedback Alert -->
<div id="adminFeedback" class="mt-3" style="display: none;"></div>
```

**JavaScript** (nel blocco `<script>` inline esistente):

```javascript
// Verifica esistenza admin al caricamento pagina
async function checkAdminExists() {
    try {
        const response = await fetch("/api/auth/adminExists");
        if (response.ok) {
            const exists = await response.json();
            const btn = document.getElementById("createAdminBtn");
            btn.style.display = exists ? "none" : "block";
        }
    } catch (e) {
        console.error("Errore verifica esistenza admin", e);
    }
}

async function createFirstAdmin() {
    const btn = document.getElementById("createAdminBtn");
    const feedbackDiv = document.getElementById("adminFeedback");
    btn.disabled = true;

    try {
        const response = await fetch("/api/auth/createFirstUser", {
            method: "POST",
            headers: { "Content-Type": "application/json" }
        });

        if (response.ok) {
            btn.style.display = "none";
            feedbackDiv.innerHTML = '<div class="alert alert-success">Admin creato con successo!</div>';
            feedbackDiv.style.display = "block";
        } else {
            const errorText = await response.text();
            btn.disabled = false;
            feedbackDiv.innerHTML = '<div class="alert alert-danger">Errore: ' + escapeHtml(errorText) + '</div>';
            feedbackDiv.style.display = "block";
        }
    } catch (e) {
        btn.disabled = false;
        feedbackDiv.innerHTML = '<div class="alert alert-danger">Errore di rete.</div>';
        feedbackDiv.style.display = "block";
    }
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text;
    return div.innerHTML;
}
```

Aggiornare `window.onload`:
```javascript
window.onload = function() {
    checkAuthToken();
    checkAdminExists();
};
```

### Sicurezza

- L'endpoint `GET /api/auth/adminExists` è già coperto dalla regola `/api/auth/**` permitAll — nessuna modifica a SecurityConfig necessaria.
- L'endpoint ritorna solo un booleano; non espone dati utente.
- Protezione XSS tramite `escapeHtml()` sui messaggi di errore.

### Proprietà di correttezza aggiuntive

5. **P5 – Correttezza adminExists**: Per qualsiasi stato del database, `GET /api/auth/adminExists` ritorna `true` se e solo se `userProfileRepository.count() > 0`
6. **P6 – Feedback corretto**: Per qualsiasi risposta HTTP ricevuta dopo click del pulsante, l'alert usa classe `alert-success` se e solo se lo status è 200

### File modificati (aggiuntivi)

| File | Modifica |
|------|----------|
| `constants/Endpoints.java` | Aggiunta costante `ADMIN_EXISTS` |
| `controller/AuthController.java` | Aggiunta firma `adminExists()` |
| `controller/impl/AuthControllerImpl.java` | Implementazione `adminExists()` |
| `service/AuthService.java` | Aggiunta firma `adminExists()` |
| `service/impl/AuthServiceImpl.java` | Implementazione `adminExists()` |
| `src/main/resources/templates/login.html` | Pulsante + JavaScript |
