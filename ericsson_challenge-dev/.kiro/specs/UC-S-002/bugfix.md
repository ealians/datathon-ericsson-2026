# Bugfix Requirements Document

## Introduction

Gli endpoint di autenticazione `POST /api/auth/login` e `POST /api/auth/signup` espongono la password dell'utente in chiaro nei log applicativi. I DTO `LoginDto` e `SignUpRequestDto` implementano un metodo `toString()` che include il campo `password` nella rappresentazione testuale. Il servizio `AuthServiceImpl` logga questi DTO come parametri nelle chiamate `logger.debug(...)`, causando la scrittura della password in chiaro nei file di log. Questa vulnerabilità viola la regola del security baseline: "Non introdurre segreti (chiavi, password, token) nel codice sorgente o nei log" e rappresenta un rischio CWE-532 (Insertion of Sensitive Information into Log File).

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN un utente effettua il login via `POST /api/auth/login` THEN il sistema logga l'intero oggetto `LoginDto` (inclusa la password in chiaro) tramite `logger.debug("Enter into AuthService.login : Parameters : {}", loginDto)` in `AuthServiceImpl.java:71`

1.2 WHEN un utente si registra via `POST /api/auth/signup` THEN il sistema logga l'intero oggetto `SignUpRequestDto` (inclusa la password in chiaro) tramite `logger.debug("Enter into AuthService.registerUser : Parameters : {}", signUpRequestDto)` in `AuthServiceImpl.java:105`

1.3 WHEN `LoginDto.toString()` viene invocato (esplicitamente o implicitamente dal logger) THEN il metodo restituisce una stringa contenente `password='<valore_in_chiaro>'`, esponendo la credenziale

1.4 WHEN `SignUpRequestDto.toString()` viene invocato THEN il metodo restituisce una stringa contenente `password='<valore_in_chiaro>'`, esponendo la credenziale

### Expected Behavior (Correct)

2.1 WHEN `LoginDto.toString()` viene invocato THEN il metodo SHALL restituire una rappresentazione che maschera il campo password (es. `password='[PROTECTED]'`), senza mai esporre il valore reale

2.2 WHEN `SignUpRequestDto.toString()` viene invocato THEN il metodo SHALL restituire una rappresentazione che maschera il campo password (es. `password='[PROTECTED]'`), senza mai esporre il valore reale

2.3 WHEN il servizio `AuthServiceImpl.login()` logga informazioni di debug THEN il sistema SHALL loggare solo l'email dell'utente, non l'intero DTO contenente la password

2.4 WHEN il servizio `AuthServiceImpl.registerUser()` logga informazioni di debug THEN il sistema SHALL loggare solo l'email dell'utente, non l'intero DTO contenente la password

### Unchanged Behavior (Regression Prevention)

3.1 WHEN `LoginDto.getPassword()` viene invocato nel processo di autenticazione THEN il sistema SHALL CONTINUE TO restituire la password in chiaro per il confronto con BCrypt (il getter non deve essere modificato)

3.2 WHEN `SignUpRequestDto.getPassword()` viene invocato nel processo di registrazione THEN il sistema SHALL CONTINUE TO restituire la password in chiaro per l'encoding con BCrypt

3.3 WHEN gli endpoint `/api/auth/login` e `/api/auth/signup` vengono invocati con credenziali valide THEN il sistema SHALL CONTINUE TO autenticare/registrare l'utente correttamente senza regressioni funzionali

3.4 WHEN il login fallisce per credenziali errate THEN il sistema SHALL CONTINUE TO restituire un errore appropriato e loggare solo l'email coinvolta (non la password tentata)

## Severity

**CRITICAL** — Esposizione di credenziali utente nei log applicativi (CWE-532). Un attaccante con accesso ai log (file system, log aggregator, SIEM) può ottenere password in chiaro di tutti gli utenti che effettuano login o registrazione.

## Affected Files

| File | Issue |
|------|-------|
| `model/dto/LoginDto.java` | `toString()` espone la password |
| `model/dto/request/SignUpRequestDto.java` | `toString()` espone la password |
| `service/impl/AuthServiceImpl.java` (line 71) | Logga `loginDto` che include la password |
| `service/impl/AuthServiceImpl.java` (line 105) | Logga `signUpRequestDto` che include la password |

## Fix Strategy

1. **`LoginDto.toString()`** — Sostituire il valore del campo password con `[PROTECTED]`
2. **`SignUpRequestDto.toString()`** — Sostituire il valore del campo password con `[PROTECTED]`
3. **`AuthServiceImpl.login()`** — Modificare il log alla riga 71 per loggare solo `loginDto.getEmail()` invece dell'intero DTO
4. **`AuthServiceImpl.registerUser()`** — Modificare il log alla riga 105 per loggare solo `signUpRequestDto.getEmail()` invece dell'intero DTO

## References

- CWE-532: Insertion of Sensitive Information into Log File
- OWASP Logging Cheat Sheet: "Never log sensitive data such as passwords"
- Project Security Baseline: "Non introdurre segreti (chiavi, password, token) nel codice sorgente o nei log"
