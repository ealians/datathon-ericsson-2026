# UC-SEC-001 — Security Hardening: Vulnerability Remediation

## Obiettivo

Risolvere le 12 vulnerabilità identificate nel vulnerability report, con priorità alle 3 CRITICAL e alle 4 HIGH.

---

## Requisiti Funzionali

| ID | Vulnerabilità | Requisito |
|----|---------------|-----------|
| RF-01 | V-001 | Il JWT secret deve essere letto da variabile d'ambiente, non hardcoded |
| RF-02 | V-002 | Le credenziali DB del profilo default devono usare variabili d'ambiente |
| RF-03 | V-003 | L'endpoint `createFirstUser` deve essere disabilitato o protetto |
| RF-04 | V-004 | I DTO non devono esporre password nel `toString()` |
| RF-05 | V-005 | CORS deve essere restrittivo (configurabile via env var) |
| RF-06 | V-006 | `LoginDto` deve avere validazione Jakarta (`@NotBlank`, `@Email`) |
| RF-07 | V-007 | L'edit profilo deve verificare che l'utente modifichi solo il proprio profilo (o sia admin) |
| RF-08 | V-008 | Migrare a jjwt parserBuilder API non-deprecated |
| RF-09 | V-009 | Abilitare security headers (HSTS, X-Content-Type-Options, X-Frame-Options) |
| RF-10 | V-010 | Password policy: minimo 8 caratteri |
| RF-11 | V-011 | Endpoint registerUser deve tornare un DTO, non l'entity |
| RF-12 | V-012 | Ridurre token expiration a 15 minuti |

## Requisiti Non-Funzionali

| ID | Requisito |
|----|-----------|
| RNF-01 | Zero downtime — le fix non devono rompere il flusso di autenticazione esistente |
| RNF-02 | Backward compatibility con i client che usano i token JWT (dopo la rotazione del secret i token esistenti saranno invalidati — accettabile) |
| RNF-03 | I test esistenti devono continuare a passare |

---

## Criteri di Accettazione

### AC-01: JWT Secret esternalizzato

```gherkin
Given l'applicazione è in esecuzione
When si ispeziona SecurityConstants.java
Then il JWT_SECRET è letto da System.getenv("JWT_SECRET") con un fallback solo per dev
  And il valore hardcoded precedente non è più nel codice
```

### AC-02: createFirstUser protetto

```gherkin
Given l'applicazione è in esecuzione con utenti nel DB
When un utente anonimo chiama POST /api/auth/createFirstUser
Then riceve 403 Forbidden o 404 Not Found
```

### AC-03: Password non nei log

```gherkin
Given un utente effettua il login
When il logger stampa il LoginDto
Then il campo password mostra "[REDACTED]" e non il valore reale
```

### AC-04: CORS restrittivo

```gherkin
Given l'applicazione è in esecuzione in profilo docker
When un browser da origin "http://evil.com" fa una richiesta API
Then la risposta non contiene Access-Control-Allow-Origin: *
```

### AC-05: LoginDto validato

```gherkin
Given un client invia POST /api/auth/login con body {"email":"","password":""}
Then riceve 400 Bad Request con errori di validazione
```

### AC-06: IDOR protetto

```gherkin
Given un utente USER con id=1 è autenticato
When chiama POST /profiles/edit/2 (profilo di un altro utente)
Then riceve 403 Forbidden
```

### AC-07: Password policy

```gherkin
Given un utente tenta la registrazione con password "abc"
When invia POST /api/auth/signup
Then riceve 400 con errore "password deve avere almeno 8 caratteri"
```

### AC-08: Security headers presenti

```gherkin
Given l'applicazione è in esecuzione
When si effettua una richiesta HTTP
Then la risposta contiene X-Content-Type-Options: nosniff
  And la risposta contiene X-Frame-Options: DENY
```
