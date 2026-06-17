# Project Steering

## Contesto Progetto

Applicazione Spring Boot 3.3.5 (Java 17) per la gestione profili utente – Datathon Ericsson 2026.

## Convenzioni

- Package base: `org.elis.ericsson.datathon.user_management`
- Pattern architetturale: Controller (interface) → impl → Service (interface) → impl → Repository
- Database: H2 (dev), PostgreSQL (docker)
- Autenticazione: JWT stateless (jjwt 0.11.5)
- Frontend: Thymeleaf + Bootstrap 5.3.3
- Build: Maven Wrapper
- When creating bugfixing specifications, use the name requirements.md

## Struttura Endpoint

- API REST: `/api/auth/*`, `/api/profiles/*`
- Pagine Web: `/login`, `/profiles/*`

## Ruoli

- `ROLE_ADMIN`: gestione completa utenti
- `ROLE_USER`: accesso base


## Test environment
- Non eseguire `mvn` direttamente sulla macchina host; utilizzare il container Docker (WSL) per build, test e run

