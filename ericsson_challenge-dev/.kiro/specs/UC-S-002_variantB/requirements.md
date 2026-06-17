# Bugfix Requirements Document

## Introduction

L'endpoint `POST /api/auth/signup` restituisce l'entità JPA `UserProfile` direttamente nella risposta HTTP. Il campo `password` dell'entità non è annotato con `@JsonIgnore`, pertanto viene serializzato da Jackson nel body JSON della response. Attualmente il codice esegue `result.setPassword(null)` prima del return, ma questo approccio è fragile: il campo appare comunque nella response come `"password": null` (information disclosure sulla struttura), e qualsiasi modifica futura al flusso o riutilizzo dell'entità in altri endpoint potrebbe esporre la password hashata (BCrypt). Questa vulnerabilità viola il principio "Non introdurre segreti (chiavi, password, token) nel codice sorgente o nei log" del security baseline e rappresenta un rischio CWE-200 (Exposure of Sensitive Information to an Unauthorized Actor).

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN un utente si registra via `POST /api/auth/signup` THEN il sistema restituisce `ResponseEntity<UserProfile>` — l'intera entità JPA — nella risposta JSON, incluso il campo `password` (attualmente impostato a `null` tramite `result.setPassword(null)` in `AuthServiceImpl.java:130`)

1.2 WHEN l'entità `UserProfile` viene serializzata da Jackson THEN il campo `password` viene incluso nel JSON di risposta (come `"password": null`) poiché non è annotato con `@JsonIgnore`, rivelando la struttura interna del modello dati

1.3 WHEN il metodo `setPassword(null)` viene rimosso, dimenticato o saltato in un refactoring futuro THEN la password hashata BCrypt viene esposta direttamente nella risposta API, consentendo attacchi offline brute-force/dictionary

1.4 WHEN l'entità `UserProfile` viene utilizzata in altri endpoint o contesti di serializzazione THEN il campo `password` rischia di essere esposto perché manca una protezione strutturale (annotazione) a livello di entità

### Expected Behavior (Correct)

2.1 WHEN l'entità `UserProfile` viene serializzata in una risposta JSON THEN il campo `password` SHALL essere escluso dalla serializzazione tramite l'annotazione `@JsonIgnore` sul campo, garantendo che non possa mai apparire nella response indipendentemente dal valore

2.2 WHEN un utente si registra via `POST /api/auth/signup` THEN il sistema SHALL continuare a restituire i dati dell'utente creato (id, email, firstName, lastName, ruoli) senza il campo password nella risposta

2.3 WHEN il campo `password` è annotato con `@JsonIgnore` THEN la deserializzazione in ingresso (setter) SHALL continuare a funzionare correttamente per gli endpoint che accettano `UserProfile` come input, utilizzando `@JsonProperty(access = Access.WRITE_ONLY)` invece di `@JsonIgnore` se necessario per mantenere la deserializzazione

### Unchanged Behavior (Regression Prevention)

3.1 WHEN `UserProfile.getPassword()` viene invocato internamente dal servizio di autenticazione THEN il sistema SHALL CONTINUE TO restituire la password hashata per il confronto BCrypt — il getter Java non deve essere influenzato dall'annotazione Jackson

3.2 WHEN un utente si registra con dati validi THEN il sistema SHALL CONTINUE TO creare l'utente, encodare la password con BCrypt e salvare nel database senza regressioni

3.3 WHEN il login viene effettuato dopo la registrazione THEN il sistema SHALL CONTINUE TO autenticare correttamente l'utente con la password fornita in fase di signup

3.4 WHEN l'endpoint `POST /api/auth/createFirstUser` restituisce un `CreateFirstUserResponseDto` THEN il sistema SHALL CONTINUE TO non esporre la password (il DTO dedicato già non include il campo password)

## Severity

**HIGH** — Esposizione della struttura del campo password nella risposta API (CWE-200). Rischio di escalation a CRITICAL se il `setPassword(null)` viene rimosso in un refactoring, esponendo hash BCrypt che consentirebbero attacchi offline.

## Affected Files

| File | Issue |
|------|-------|
| `model/entity/UserProfile.java` | Campo `password` manca di `@JsonProperty(access = Access.WRITE_ONLY)` per escluderlo dalla serializzazione JSON |
| `service/impl/AuthServiceImpl.java` (riga ~130) | Rimuovere il workaround `result.setPassword(null)` — non più necessario dopo l'annotazione |

## Fix Strategy

1. **`UserProfile.java`** — Aggiungere `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` sul campo `password`. Questo esclude il campo dalla serializzazione (output JSON) ma consente ancora la deserializzazione (input JSON) nel caso l'entità venga usata come request body altrove.

2. **`AuthServiceImpl.java`** — Rimuovere la riga `result.setPassword(null)` in `registerUser()`, poiché la protezione è ora strutturale a livello di annotazione e non richiede intervento runtime.

## References

- CWE-200: Exposure of Sensitive Information to an Unauthorized Actor
- OWASP API Security Top 10 — API3:2023 Broken Object Property Level Authorization
- Jackson `@JsonProperty(access = WRITE_ONLY)` documentation
- Project Security Baseline: "Non introdurre segreti (chiavi, password, token) nel codice sorgente o nei log"
