# User Profile Management System

**Datathon Ericsson 2026 - Academy Generative AI**

Applicazione Spring Boot per la gestione di profili utente con autenticazione JWT, gestione ruoli (ADMIN/USER) e interfaccia web Thymeleaf/Bootstrap.

---

## Tech Stack

| Componente | Tecnologia | Versione |
|---|---|---|
| Runtime | Java | 17 |
| Framework | Spring Boot | 3.3.5 |
| Security | Spring Security 6 + JWT (jjwt) | 0.11.5 |
| ORM | Spring Data JPA / Hibernate | - |
| Database (dev) | H2 (embedded, file-based) | - |
| Database (docker) | PostgreSQL | 16 |
| Template Engine | Thymeleaf + Layout Dialect | 3.1.0 |
| UI Framework | Bootstrap (WebJar) | 5.3.3 |
| Build Tool | Maven | 3.x |

---

## Quick Start

### Opzione 1: Docker Compose (consigliato)

**Prerequisiti:** Docker e Docker Compose installati.

```bash
# 1. Clona il repository
git clone <repository-url>
cd ericsson_challenge-dev

# 2. (Opzionale) Personalizza le variabili d'ambiente
cp .env.example .env
# Modifica .env con i tuoi valori

# 3. Avvia tutto
docker compose up --build

# 4. Apri il browser
# http://localhost:8080/login
```

Per fermare i servizi:
```bash
docker compose down

# Per rimuovere anche i dati del database:
docker compose down -v
```

### Opzione 2: Avvio Locale (sviluppo)

**Prerequisiti:** Java 17+, Maven 3.x

```bash
# 1. Clona il repository
git clone <repository-url>
cd ericsson_challenge-dev

# 2. Avvia l'applicazione (usa H2 embedded)
./mvnw spring-boot:run

# Su Windows:
mvnw.cmd spring-boot:run

# 3. Apri il browser
# http://localhost:8080/login
```

---

## Credenziali di Default

Al primo avvio viene creato un utente amministratore:

| Campo | Valore |
|---|---|
| Email | `admin@elis.org` |
| Password | `password` |
| Ruolo | ADMIN |

> **Nota:** Modificare la password di default dopo il primo accesso.

---

## Configurazione

### Variabili d'ambiente (Docker)

| Variabile | Default | Descrizione |
|---|---|---|
| `DB_NAME` | `datathon_db` | Nome database PostgreSQL |
| `DB_USERNAME` | `datathon_user` | Username database |
| `DB_PASSWORD` | `datathon_pass` | Password database |
| `DB_PORT` | `5432` | Porta database (host) |
| `APP_PORT` | `8080` | Porta applicazione (host) |

### Profili Spring

| Profilo | Database | Uso |
|---|---|---|
| (default) | H2 file-based (`./data/datathon_user_db`) | Sviluppo locale |
| `docker` | PostgreSQL | Docker Compose |

Per attivare un profilo:
```bash
# Via Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker

# Via variabile d'ambiente
SPRING_PROFILES_ACTIVE=docker java -jar target/user-management-datathon-1.0.0.jar
```

### H2 Console (solo profilo default)

Disponibile in sviluppo locale su `http://localhost:8080/h2-console`:

| Campo | Valore |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/datathon_user_db` |
| Username | `admin` |
| Password | vedi `application.properties` |

---

## Endpoint API

### Autenticazione (`/api/auth`)

| Metodo | Endpoint | Descrizione | Auth |
|---|---|---|---|
| POST | `/api/auth/login` | Login con email/password | No |
| POST | `/api/auth/signup` | Registrazione nuovo utente | No |
| POST | `/api/auth/logout` | Logout (invalida sessione) | JWT |
| POST | `/api/auth/refreshToken` | Rinnova access token | JWT |
| GET | `/api/auth/getPossibleRoles` | Lista ruoli disponibili | JWT |
| GET | `/api/auth/getSession` | Info sessione corrente | JWT |

### Profili Utente (`/api/profiles`)

| Metodo | Endpoint | Descrizione | Auth |
|---|---|---|---|
| GET | `/api/profiles` | Lista tutti i profili | JWT |
| DELETE | `/api/profiles/{id}` | Elimina profilo | JWT + ADMIN |

### Pagine Web (Thymeleaf)

| Metodo | Endpoint | Descrizione | Auth |
|---|---|---|---|
| GET | `/login` | Pagina di login | No |
| GET | `/profiles` | Lista profili | Autenticato |
| GET | `/profiles/add-profile` | Form nuovo profilo | ADMIN |
| GET | `/profiles/edit/{id}` | Form modifica profilo | Autenticato |
| POST | `/profiles/add` | Salva nuovo profilo | ADMIN |
| POST | `/profiles/edit/{id}` | Salva modifica profilo | Autenticato |

### Esempio di chiamata API

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@elis.org", "password": "password"}'

# Lista profili (con token JWT ottenuto dal login)
curl http://localhost:8080/api/profiles \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

## Struttura del Progetto

```
ericsson_challenge-dev/
├── Dockerfile                          # Build multi-stage
├── docker-compose.yml                  # Orchestrazione servizi
├── .env.example                        # Template variabili d'ambiente
├── pom.xml                             # Dipendenze Maven
├── mvnw / mvnw.cmd                     # Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/org/elis/ericsson/datathon/user_management/
│   │   │   ├── EricssonDatathonProjectApplication.java     # Entry point
│   │   │   ├── configuration/                              # Security, CORS
│   │   │   ├── constants/                                  # Costanti (endpoint, messaggi, security)
│   │   │   ├── controller/
│   │   │   │   ├── impl/                                   # REST API controllers
│   │   │   │   └── web/                                    # Thymeleaf controllers
│   │   │   ├── model/
│   │   │   │   ├── dto/                                    # Data Transfer Objects
│   │   │   │   ├── entity/                                 # JPA Entities
│   │   │   │   ├── exception/                              # Custom exceptions
│   │   │   │   └── projection/                             # JPA Projections
│   │   │   ├── repository/                                 # Spring Data repositories
│   │   │   ├── security/                                   # JWT filter, utility
│   │   │   └── service/                                    # Business logic
│   │   └── resources/
│   │       ├── application.properties                      # Config default (H2)
│   │       ├── application-docker.properties               # Config Docker (PostgreSQL)
│   │       └── templates/                                  # Thymeleaf HTML
│   └── test/                                               # Test JUnit
```

---

## Entita JPA

| Entity | Tabella | Descrizione |
|---|---|---|
| `UserProfile` | `users` | Profilo utente (email, nome, password, ruoli) |
| `Role` | `roles` | Ruoli (ROLE_ADMIN, ROLE_USER) |
| `RefreshToken` | `refresh_token` | Token per rinnovo sessione |
| `PasswordResetToken` | `password_reset_token` | Token per reset password |
| `EggUpInfo` | `eggup_user` | Info assessment EggUp |
| `EggUpScore` | `eggup_score` | Punteggi assessment |
| `EggUpTrait` | `eggup_trait` | Dettaglio tratti |

---

## Sviluppo

### Build del progetto

```bash
# Compilazione
./mvnw clean package

# Compilazione senza test
./mvnw clean package -DskipTests

# Esecuzione test
./mvnw test
```

### Rebuild Docker dopo modifiche

```bash
docker compose up --build
```

---

## Troubleshooting

| Problema | Soluzione |
|---|---|
| `403 Forbidden` | Verificare che il token JWT sia valido e che l'utente abbia il ruolo necessario |
| `Token JWT scaduto` | Effettuare nuovamente il login o usare il refresh token |
| `Errore connessione DB` | Verificare le credenziali in `application.properties` o nelle variabili d'ambiente |
| `Docker: postgres non si avvia` | Verificare che la porta 5432 non sia gia in uso (`docker compose down` e riprovare) |
| `Docker: app non si connette a postgres` | Il container postgres deve essere healthy prima che l'app parta (gestito da `depends_on`) |
| `H2 Console non accessibile` | Disponibile solo con profilo default, non con profilo `docker` |
