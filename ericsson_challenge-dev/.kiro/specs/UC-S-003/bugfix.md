# Bugfix Requirements Document

## Introduction

L'endpoint `POST /api/auth/createFirstUser` (UC-S-003) presenta molteplici problemi di sicurezza, conformità architetturale e qualità del codice che lo rendono inadatto a un ambiente di produzione. Le credenziali dell'utente admin sono hardcoded nel codice sorgente, il metodo non è dichiarato nell'interfaccia del controller, manca la validazione dell'input tramite DTO, la risposta espone dati sensibili (password encodata), e il path non è definito come costante in `Endpoints.java`. Questo bugfix mira a rendere l'endpoint conforme alle best practice del progetto.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN il metodo `createFirstUser` viene invocato THEN il sistema crea sempre un utente admin con email `admin@elis.org` e password `password` hardcoded nel codice sorgente, violando le regole di sicurezza del progetto

1.2 WHEN il metodo `createFirstUser` viene invocato THEN il sistema accetta un `HttpServletRequest` generico senza alcuna validazione dei dati in ingresso (nessun DTO con Jakarta Validation)

1.3 WHEN il metodo `createFirstUser` completa con successo THEN il sistema restituisce l'entità `UserProfile` completa nella risposta, esponendo la password encodata e dati interni sensibili

1.4 WHEN si esamina l'interfaccia `AuthController.java` THEN il metodo `createFirstUser` non è dichiarato nell'interfaccia, violando il pattern architetturale Controller interface → impl del progetto

1.5 WHEN si esamina `Endpoints.java` THEN il path `/createFirstUser` non è definito come costante, violando la convenzione del progetto di centralizzare i path in costanti

1.6 WHEN il service `AuthServiceImpl.createFirstUser` viene invocato THEN il tipo di ritorno è `ResponseEntity<?>` con parametro `HttpServletRequest`, non conforme al pattern del progetto che prevede DTO tipizzati in input e output

### Expected Behavior (Correct)

2.1 WHEN il metodo `createFirstUser` viene invocato con un DTO valido THEN il sistema SHALL utilizzare email e password forniti dal client nel DTO di richiesta, senza credenziali hardcoded nel codice sorgente

2.2 WHEN il metodo `createFirstUser` viene invocato THEN il sistema SHALL validare l'input tramite un DTO dedicato con annotazioni Jakarta Validation (`@NotBlank`, `@Email`, `@Size`) e rifiutare richieste non valide con errore 400

2.3 WHEN il metodo `createFirstUser` completa con successo THEN il sistema SHALL restituire un DTO di risposta che non espone la password (né in chiaro né encodata) e contenga solo le informazioni sicure dell'utente creato (id, email, firstName, lastName, ruoli)

2.4 WHEN si esamina l'interfaccia `AuthController.java` THEN il metodo `createFirstUser` SHALL essere dichiarato nell'interfaccia con la firma corretta e le annotazioni appropriate (`@PostMapping`)

2.5 WHEN si esamina `Endpoints.java` THEN il path `/createFirstUser` SHALL essere definito come costante `CREATE_FIRST_USER` e utilizzato nel controller

2.6 WHEN il service `AuthServiceImpl.createFirstUser` viene invocato THEN la firma del metodo SHALL accettare un DTO tipizzato come parametro e restituire un `ResponseEntity` con un DTO di risposta tipizzato

### Unchanged Behavior (Regression Prevention)

3.1 WHEN esiste già almeno un utente nel database e si invoca `createFirstUser` THEN il sistema SHALL CONTINUE TO rifiutare la richiesta con un errore, impedendo la creazione di utenti admin duplicati

3.2 WHEN i ruoli `ROLE_ADMIN` e `ROLE_USER` non esistono nel database THEN il sistema SHALL CONTINUE TO crearli automaticamente prima di assegnare i ruoli all'utente

3.3 WHEN i ruoli `ROLE_ADMIN` e `ROLE_USER` esistono già nel database THEN il sistema SHALL CONTINUE TO riutilizzarli senza crearne di duplicati

3.4 WHEN l'endpoint `/api/auth/createFirstUser` viene invocato THEN il sistema SHALL CONTINUE TO essere accessibile senza autenticazione (endpoint pubblico sotto `/api/auth/**`)

3.5 WHEN l'utente admin viene creato con successo THEN il sistema SHALL CONTINUE TO assegnare entrambi i ruoli `ROLE_ADMIN` e `ROLE_USER` al primo utente

3.6 WHEN gli altri endpoint di `AuthController` (`/login`, `/signup`, `/logout`, `/refreshToken`, ecc.) vengono invocati THEN il sistema SHALL CONTINUE TO funzionare esattamente come prima senza alcuna regressione
