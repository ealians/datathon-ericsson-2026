# Security Baseline

## Autenticazione

- Autenticazione stateless basata su JWT (libreria jjwt 0.11.5, algoritmo HS512)
- Token trasportato via header `Authorization: Bearer <token>` oppure cookie `authToken`
- Scadenza access token: 5 ore (`TOKEN_EXPIRATION = 30*60*1000*10`)
- Refresh token persistito su DB con scadenza 12 ore
- Password hashing: BCrypt (`BCryptPasswordEncoder`)
- Il JWT secret DEVE essere esternalizzato in variabili d'ambiente; non hardcodare in codice sorgente

## Autorizzazione

- Due ruoli: `ROLE_ADMIN` (gestione completa), `ROLE_USER` (accesso base)
- Protezione method-level via `@PreAuthorize` (annotation-based)
- Endpoint pubblici: `/login`, `/api/auth/**`, `/v3/api-docs/**`, `/actuator/health`, risorse statiche
- Operazioni CRUD profili riservate ad ADMIN (`POST /api/profiles/add`, `DELETE /api/profiles/{id}`)
- Tutte le richieste non esplicitamente consentite richiedono autenticazione

## Sessioni e CSRF

- Sessione: `SessionCreationPolicy.STATELESS` — nessuna sessione server-side
- CSRF disabilitato (accettabile con autenticazione stateless via Bearer token)
- Se il cookie `authToken` viene usato dal frontend, valutare flag `HttpOnly`, `Secure` e `SameSite`

## CORS

- Origini consentite: `*` (tutte) — restringere in produzione a domini noti
- Tutti i metodi e header consentiti — limitare ai soli necessari in produzione

## Trasporto e Headers

- In produzione abilitare HTTPS e HSTS
- `X-Frame-Options` attualmente disabilitato (per H2 console dev) — riabilitare in produzione con `DENY` o `SAMEORIGIN`
- Considerare header `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Content-Security-Policy`

## Container e Infrastruttura

- Docker multi-stage build: build con Maven, runtime con JRE Alpine
- Container eseguito come utente non-root (`appuser`)
- Credenziali DB iniettate via variabili d'ambiente (`.env`), non hardcodate nell'immagine
- H2 console disabilitata nel profilo Docker
- PostgreSQL con healthcheck configurato

## Regole per lo Sviluppo

- Non introdurre segreti (chiavi, password, token) nel codice sorgente o nei log
- Utilizzare Jakarta Validation sui DTO di input (`@NotNull`, `@Size`, `@Email`, ecc.)
- Non disabilitare o indebolire la SecurityFilterChain senza approvazione esplicita
- Mantenere separazione tra logica di autenticazione (security package) e logica di business (service package)
- Ogni nuovo endpoint deve dichiarare esplicitamente le autorizzazioni richieste nella SecurityConfig
