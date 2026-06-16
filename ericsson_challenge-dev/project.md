# Analisi Progetto – Ericsson Datathon 2026

## Panoramica

Applicazione monolitica **Spring Boot 3.3.5** (Java 17) per la gestione di profili utente, sviluppata per la Datathon Ericsson 2026 – Academy Generative AI. Espone sia API REST sia pagine web Thymeleaf, con autenticazione JWT stateless e autorizzazione basata su ruoli (ADMIN / USER).

---

## Stack Tecnologico

| Componente | Tecnologia | Versione |
|---|---|---|
| Runtime | Java | 17 |
| Framework | Spring Boot | 3.3.5 |
| Sicurezza | Spring Security 6 + JWT (jjwt) | 0.11.5 |
| ORM | Spring Data JPA / Hibernate | – |
| Database (dev) | H2 (file-based) | – |
| Database (docker) | PostgreSQL | 16 |
| Template Engine | Thymeleaf + Layout Dialect | 3.1.0 |
| UI | Bootstrap (WebJar) | 5.3.3 |
| Build Tool | Maven Wrapper | 3.x |
| Container | Docker multi-stage + Compose | – |

---

## Architettura a Layer

```
┌─────────────────────────────────────────────────────────┐
│                     Client (Browser)                     │
└────────────────┬────────────────────────┬───────────────┘
                 │ REST (JSON)            │ HTML (Thymeleaf)
┌────────────────▼────────────────┐ ┌─────▼───────────────┐
│   controller.impl (REST API)    │ │  controller.web     │
│  AuthControllerImpl             │ │  LoginPageController │
│  UserProfileControllerImpl      │ │  UserProfileWebCtrl  │
└────────────────┬────────────────┘ └─────┬───────────────┘
                 │                         │
┌────────────────▼─────────────────────────▼──────────────┐
│                    service.impl                          │
│  AuthServiceImpl · UserProfileServiceImpl               │
│  CustomUserDetailsService                               │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│                    repository (JPA)                      │
│  UserProfileRepository · RoleRepository                 │
│  RefreshTokenRepository · PasswordResetTokenRepository  │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│              Database (H2 dev / PostgreSQL docker)       │
└─────────────────────────────────────────────────────────┘
```

Un filtro JWT (`JwtAuthenticationFilter`) intercetta ogni richiesta HTTP, valida il token e popola il `SecurityContext`.

---

## Struttura Package

```
org.elis.ericsson.datathon.user_management
├── EricssonDatathonProjectApplication.java   # Entry point
├── configuration/
│   ├── SecurityConfig.java          # Filter chain, RBAC, BCrypt encoder
│   └── CorsConfig.java              # CORS aperto (allowedOrigin = "*")
├── constants/
│   ├── Endpoints.java               # Percorsi costanti (/api, /auth, /profiles)
│   ├── SecurityConstants.java       # JWT secret, token expiration
│   └── ExceptionMessages.java       # Messaggi di errore standard
├── controller/
│   ├── AuthController.java          # Interfaccia REST auth
│   ├── UserProfileController.java   # Interfaccia REST profili
│   ├── impl/
│   │   ├── AuthControllerImpl.java
│   │   └── UserProfileControllerImpl.java
│   └── web/
│       ├── LoginPageController.java
│       └── UserProfileWebController.java
├── model/
│   ├── dto/
│   │   ├── AuthResponseDTO.java
│   │   ├── LoginDto.java
│   │   ├── TokenRefreshResponseDto.java
│   │   └── request/
│   │       └── SignUpRequestDto.java
│   ├── entity/
│   │   ├── UserProfile.java
│   │   ├── Role.java
│   │   ├── RefreshToken.java
│   │   ├── PasswordResetToken.java
│   │   ├── UserPrincipal.java
│   │   └── eggup/
│   │       ├── EggUpInfo.java
│   │       ├── EggUpScore.java
│   │       └── EggUpTrait.java
│   ├── exception/
│   │   ├── ExpiredJwtException.java
│   │   ├── InvalidCredentialsException.java
│   │   ├── ItemNotFoundException.java
│   │   ├── ItemAlreadyExistsException.java
│   │   └── RequestError.java
│   ├── modelbase/
│   │   └── DateAudit.java           # MappedSuperclass (createdAt, updatedAt)
│   └── projection/
│       ├── UserMeInfo.java
│       └── UserDetailInfo.java
├── repository/
│   ├── UserProfileRepository.java
│   ├── RoleRepository.java
│   ├── RefreshTokenRepository.java
│   └── PasswordResetTokenRepository.java
├── security/
│   ├── JwtUtility.java              # Generazione/validazione token
│   ├── JwtAuthenticationFilter.java # Filtro OncePerRequest
│   ├── CustomAuthenticationManager.java
│   └── CurrentUser.java             # Annotazione custom
└── service/
    ├── AuthService.java
    ├── UserProfileService.java
    └── impl/
        ├── AuthServiceImpl.java
        ├── UserProfileServiceImpl.java
        └── CustomUserDetailsService.java
```

---

## Modello Dati (Entità JPA)

```
┌──────────────┐       M:N (users_roles)       ┌──────────┐
│  UserProfile │◄──────────────────────────────►│   Role   │
│  (users)     │                                │  (roles) │
└──────┬───────┘                                └──────────┘
       │ 1:1
       ├──────────────► RefreshToken (refresh_token)
       │ 1:1
       ├──────────────► PasswordResetToken (password_reset_token)
       │ 1:1
       └──────────────► EggUpInfo (eggup_user)
                             │ 1:1
                             └──────► EggUpScore (eggup_score)
                                          │ 1:N
                                          └──────► EggUpTrait (eggup_trait)
```

### Dettaglio campi principali

| Entity | Tabella | Campi chiave |
|---|---|---|
| UserProfile | `users` | id, username, email, firstName, lastName, phoneNumber, password, roles |
| Role | `roles` | id, name (ROLE_ADMIN, ROLE_USER) |
| RefreshToken | `refresh_token` | id, user, token, expiryDate |
| PasswordResetToken | `password_reset_token` | id, user, token, expiryDate, used, isSignUp |
| EggUpInfo | `eggup_user` | id, eggUpUserGuid, username, password, assessmentUrl, authenticationToken |
| EggUpScore | `eggup_score` | id, testName, coverageIndex, duration, date |
| EggUpTrait | `eggup_trait` | id, traitId, traitName, score, macroName, macroScore, macroWeight, count |

Tutte le entità ereditano `DateAudit` → `createdAt`, `updatedAt` (JPA Auditing).

---

## Sicurezza

| Aspetto | Dettaglio |
|---|---|
| Algoritmo firma | HS512 (HMAC-SHA512) |
| Durata access token | ~150 minuti |
| Durata refresh token | 12 ore |
| Sorgente token | Header `Authorization: Bearer …` oppure cookie `authToken` |
| Encoding password | BCrypt |
| Gestione sessione | STATELESS (nessuna sessione lato server) |
| CSRF | Disabilitato |
| CORS | Aperto a tutte le origini |
| Utente seed | `admin@elis.org` / `password` (ROLE_ADMIN + ROLE_USER) |

### Flusso autenticazione

1. Client invia `POST /api/auth/login` con email/password
2. `CustomAuthenticationManager` verifica credenziali con BCrypt
3. `JwtUtility` genera access token + refresh token
4. Client riceve `AuthResponseDTO` (token, refreshToken, info utente)
5. Ogni richiesta successiva passa per `JwtAuthenticationFilter` che valida il JWT e popola il `SecurityContext`

---

## Endpoint API

### Autenticazione – `/api/auth`

| Metodo | Path | Descrizione | Auth richiesta |
|---|---|---|---|
| POST | `/login` | Login email/password → JWT | No |
| POST | `/signup` | Registrazione utente | No |
| POST | `/logout` | Invalida sessione | JWT |
| POST | `/refreshToken` | Rinnova access token | JWT |
| POST | `/createFirstUser` | Seed utente admin (primo avvio) | No |
| POST | `/tokenResetPassword` | Validazione token reset password | No |
| GET | `/me` | Info utente corrente | JWT (ROLE_USER) |
| GET | `/getPossibleRoles` | Lista ruoli | JWT |
| GET | `/getSession` | Info sessione attiva | JWT |

### Profili – `/api/profiles`

| Metodo | Path | Descrizione | Auth richiesta |
|---|---|---|---|
| GET | `/` | Lista tutti i profili | JWT |
| DELETE | `/{id}` | Elimina profilo | JWT + ROLE_ADMIN |

### Pagine Web (Thymeleaf)

| Metodo | Path | Descrizione | Auth richiesta |
|---|---|---|---|
| GET | `/login` | Form di login | No |
| GET | `/profiles` | Tabella profili | Autenticato |
| GET | `/profiles/add-profile` | Form aggiunta | ROLE_ADMIN |
| POST | `/profiles/add` | Salva nuovo profilo | ROLE_ADMIN |
| GET | `/profiles/edit/{id}` | Form modifica | Autenticato |
| POST | `/profiles/edit/{id}` | Salva modifiche | Autenticato |

---

## Frontend (Thymeleaf + Bootstrap)

- **Layout**: `base.html` con Thymeleaf Layout Dialect (navbar, footer, slot `content`)
- **Pagine**: login.html, profiles.html, addProfile.html, editProfile.html
- **Logica lato client** (JavaScript nelle pagine):
  - Login via `fetch` API → salvataggio JWT in cookie + localStorage
  - Caricamento profili via AJAX con header `Authorization`
  - Decodifica JWT lato client per mostrare/nascondere pulsanti in base al ruolo
  - Eliminazione profilo via `DELETE` API call

---

## Deployment

### Docker Compose (produzione/staging)

```yaml
services:
  postgres:    # PostgreSQL 16 Alpine, healthcheck, volume persistente
  app:         # Spring Boot app, profilo "docker", dipende da postgres healthy
```

### Dockerfile (multi-stage)

1. **Build**: `maven:3.9-eclipse-temurin-17` → `mvn clean package -DskipTests`
2. **Runtime**: `eclipse-temurin:17-jre-alpine`, utente non-root, porta 8080

### Profili Spring

| Profilo | Database | Uso |
|---|---|---|
| default | H2 file `./data/datathon_user_db` | Sviluppo locale |
| docker | PostgreSQL (host da env vars) | Docker Compose |

---

## Integrazioni Esterne

### EggUp (Assessment)

Modello dati per l'integrazione con la piattaforma EggUp (assessment di soft skill / tratti personalità):

- `EggUpInfo`: credenziali utente EggUp, URL assessment, token di autenticazione
- `EggUpScore`: risultati aggregati (test name, coverage index, duration)
- `EggUpTrait`: dettaglio singoli tratti con punteggi e macro-categorie

Al momento non è presente codice client attivo per comunicare con l'API EggUp – il modello sembra predisposto per un'integrazione futura o per un import manuale dei dati.

---

## Dipendenze Maven (principali)

| Dipendenza | Scopo |
|---|---|
| spring-boot-starter-data-jpa | ORM, Spring Data repositories |
| spring-boot-starter-security | Spring Security 6 |
| spring-boot-starter-thymeleaf | Template engine |
| spring-boot-starter-web | REST + MVC |
| thymeleaf-extras-springsecurity6 | Integrazione sec: tag |
| jjwt-api / jjwt-impl / jjwt-jackson | Gestione JWT |
| h2 | Database embedded (dev) |
| postgresql | Driver PostgreSQL (docker) |
| lombok | Riduzione boilerplate |
| jakarta.validation-api + hibernate-validator | Validazione DTO |
| bootstrap (webjar) | UI CSS/JS |
| thymeleaf-layout-dialect | Layout templates |
| commons-lang | Utility (RandomStringUtils per EggUp) |
| jackson-datatype-jsr310 | Serializzazione date Java 8+ |

---

## Note e Considerazioni

1. **JWT secret hardcoded** in `SecurityConstants.java` – in produzione andrebbe esternalizzato in variabili d'ambiente o vault.
2. **CORS completamente aperto** (`allowedOrigin = "*"`) – da restringere per ambienti non-dev.
3. **Password di default** (`admin@elis.org` / `password`) – da cambiare obbligatoriamente dopo il primo avvio.
4. **Il controller REST dei profili** espone solo GET all e DELETE; le operazioni CRUD complete (add/edit) sono gestite solo tramite le pagine web Thymeleaf.
5. **Nessun GlobalExceptionHandler** – le eccezioni custom esistono ma manca un `@ControllerAdvice` per gestirle uniformemente nelle risposte API.
6. **Test**: presente la struttura `src/test` ma non analizzata nel dettaglio.
