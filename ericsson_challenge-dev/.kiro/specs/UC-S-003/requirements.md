# UC-S-003 – Bugfix: Fallimento endpoint `createFirstUser`

## Problema

L'endpoint pubblico `POST /api/auth/createFirstUser` non riesce a creare il primo utente admin con successo. L'endpoint è progettato per inizializzare il sistema con un utente amministratore hardcoded (`admin@elis.org` / `password`) alla prima esecuzione dell'applicazione, ma la chiamata fallisce o ritorna errori non strutturati.

## Root Cause

L'analisi del codice rivela **tre problemi distinti** che concorrono al fallimento:

| # | Componente | Problema | Impatto |
|---|---|---|---|
| 1 | `AuthServiceImpl.passwordEncoder` | Istanziato come `new BCryptPasswordEncoder()` anziché iniettato come bean Spring | Non usa il bean definito in `SecurityConfig`; funziona ma viola il principio DI e potrebbe causare inconsistenze se il bean viene modificato |
| 2 | Serializzazione `UserProfile` → JSON | Relazione bidirezionale `UserProfile.eggUpInfo ↔ EggUpInfo.creationUser` senza `@JsonIgnore` su `eggUpInfo` in `UserProfile` | Jackson entra in loop infinito di serializzazione quando si ritorna `ResponseEntity.ok(user)` → `StackOverflowError` o `HttpMessageNotWritableException` |
| 3 | Gestione eccezioni | Nessun `@ControllerAdvice` / `GlobalExceptionHandler` nel progetto | Le eccezioni (`"First user already present!"`, errori JPA, errori di serializzazione) vengono propagate come errori 500 senza body JSON strutturato, oppure redirect a `/login` via `LoginUrlAuthenticationEntryPoint` — **out of scope per questo bugfix** |

### Flusso di esecuzione attuale

```
POST /api/auth/createFirstUser
  → SecurityFilterChain: permitAll (/api/auth/**) ✅
  → JwtAuthenticationFilter: nessun token → continua senza auth ✅
  → AuthControllerImpl.createFirstUser()
  → AuthServiceImpl.createFirstUser()
    1. count() == 0 → procede ✅
    2. Crea ruoli ROLE_ADMIN, ROLE_USER → save OK ✅
    3. Crea UserProfile con passwordEncoder.encode() → save OK ✅
    4. ResponseEntity.ok(user) → Jackson serializza UserProfile
       → UserProfile.eggUpInfo (null ma campo presente)
       → Se non null: loop EggUpInfo → UserProfile → EggUpInfo...
       → Anche se null: il campo espone la struttura interna ❌
    5. La password encodata viene ritornata nel JSON ❌ (security issue)
```

---

## Modifiche richieste

### Fix 1: Iniezione corretta del `PasswordEncoder` in `AuthServiceImpl` e `CustomAuthenticationManager`

**Path 1**: `src/main/java/org/elis/ericsson/datathon/user_management/service/impl/AuthServiceImpl.java`

**Stato attuale**:
```java
final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
```

**Stato corretto**:
```java
private final PasswordEncoder passwordEncoder;
```

Il `PasswordEncoder` deve essere iniettato tramite costruttore, usando il bean `@Bean` definito in `SecurityConfig`. L'istanza diretta va rimossa e il campo aggiunto ai parametri del costruttore.

**Path 2**: `src/main/java/org/elis/ericsson/datathon/user_management/security/CustomAuthenticationManager.java`

**Stato attuale**:
```java
final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
```

**Stato corretto**:
```java
private final PasswordEncoder passwordEncoder;
```

Anche `CustomAuthenticationManager` istanzia un `BCryptPasswordEncoder` locale anziché usare il bean Spring. Questo non blocca il login (BCrypt è cross-istanza compatibile: qualsiasi istanza può verificare un hash creato da un'altra), ma viola il principio di Dependency Injection e potrebbe causare inconsistenze se il bean in `SecurityConfig` venisse sostituito con un encoder diverso. Il `PasswordEncoder` va iniettato tramite costruttore come per `AuthServiceImpl`.

> **Nota**: il login con `admin@elis.org` / `password` funziona correttamente dopo tutti i fix proposti perché:
> 1. La password viene encodata con BCrypt in `createFirstUser` (Fix 1 non cambia l'algoritmo)
> 2. Il `CustomAuthenticationManager.authenticate()` verifica la password con `passwordEncoder.matches()` — compatibile cross-istanza
> 3. La risposta di login ritorna un `AuthResponseDTO` (DTO piatto), non `UserProfile`, quindi non è soggetta ai problemi di serializzazione del Fix 2

---

### Fix 2: Prevenire serializzazione circolare e esposizione password

**Path**: `src/main/java/org/elis/ericsson/datathon/user_management/model/entity/UserProfile.java`

| Campo | Annotazione da aggiungere | Motivazione |
|---|---|---|
| `password` | `@JsonIgnore` | La password (anche encodata) non deve mai apparire nelle risposte JSON |
| `eggUpInfo` | `@JsonIgnore` | Previene loop di serializzazione bidirezionale e nasconde dettagli interni |

---

## Criteri di accettazione

| # | Criterio | Verifica |
|---|---|---|
| AC-1 | La chiamata `POST /api/auth/createFirstUser` su un database vuoto ritorna HTTP 200 con il profilo utente creato (senza password nel body) | Test di integrazione |
| AC-2 | La risposta JSON non contiene il campo `password` | Assenza di `"password"` nel body di risposta |
| AC-3 | La risposta JSON non causa loop di serializzazione (campo `eggUpInfo` escluso) | Nessun `StackOverflowError` o timeout |
| AC-4 | Una seconda chiamata a `createFirstUser` ritorna un errore (exception propagata) con messaggio `"First user already present!"` | Test ripetibilità |
| AC-5 | Il `PasswordEncoder` in `AuthServiceImpl` e `CustomAuthenticationManager` è iniettato come bean Spring (stesso bean di `SecurityConfig`) | Verifica codice / test di contesto |
| AC-9 | Dopo `createFirstUser`, il login con `admin@elis.org` / `password` ritorna HTTP 200 con JWT valido | Test di integrazione end-to-end |
| AC-7 | Nessuna regressione sui test esistenti | `mvn test` → tutti i test pre-esistenti passano |
| AC-8 | L'utente creato ha ruoli `ROLE_ADMIN` e `ROLE_USER` assegnati | Verifica nel body di risposta |

---

## Priorità dei fix

1. **Fix 2** (serializzazione) — causa diretta del fallimento visibile (errore HTTP 500)
2. **Fix 1** (DI PasswordEncoder) — correttezza architetturale, previene inconsistenze future

---

## Note implementative

- Fix 1 e Fix 2 sono minimali e non invasivi (annotazioni + modifica costruttore)
- Nessuna nuova dipendenza Maven necessaria
- Compatibile con entrambi i profili (H2 dev, PostgreSQL docker)
- Segue il pattern architetturale del progetto

---

## Feature Aggiuntiva: Pulsante "Crea Primo Admin" nella pagina di login

### Descrizione

Aggiungere un pulsante nella pagina di login che permetta di creare il primo utente admin con un solo click, senza dover utilizzare strumenti esterni (curl, Postman, ecc.). Il pulsante chiama l'endpoint `POST /api/auth/createFirstUser` già esistente.

### Requisiti

#### Requisito F-1: Posizionamento del pulsante

**User Story:** Come amministratore di sistema, voglio un pulsante visibile nella pagina di login per creare il primo utente admin, così da poter inizializzare il sistema senza strumenti API esterni.

**Criteri di accettazione:**

| # | Criterio | Verifica |
|---|---|---|
| AC-F1 | La pagina di login mostra il pulsante "Crea Primo Admin" sotto il pulsante Login, nella stessa card Bootstrap | Verifica visiva |
| AC-F2 | Il pulsante usa stile Bootstrap `btn-success` con aspetto distinto dal pulsante Login | Verifica visiva |

#### Requisito F-2: Chiamata API dal pulsante

**User Story:** Come amministratore di sistema, voglio che il pulsante crei automaticamente l'utente admin con credenziali predefinite, così da poter configurare il sistema con un solo click.

**Criteri di accettazione:**

| # | Criterio | Verifica |
|---|---|---|
| AC-F3 | Cliccando il pulsante, viene inviata una POST a `/api/auth/createFirstUser` via `fetch()` | Test E2E / verifica manuale |
| AC-F4 | Il pulsante viene disabilitato durante la chiamata API | Verifica comportamento |
| AC-F5 | Se la risposta è HTTP 200, viene mostrato un alert Bootstrap verde con messaggio di conferma | Verifica visiva |
| AC-F6 | Se la risposta è non-200, viene mostrato un alert Bootstrap rosso con l'errore | Verifica visiva |

#### Requisito F-3: Controllo visibilità del pulsante

**User Story:** Come amministratore di sistema, voglio che il pulsante sia nascosto quando un admin esiste già, così da non confondermi con un'azione che fallirebbe.

**Criteri di accettazione:**

| # | Criterio | Verifica |
|---|---|---|
| AC-F7 | Al caricamento della pagina, viene verificata l'esistenza dell'admin tramite `GET /api/auth/adminExists` | Test di integrazione |
| AC-F8 | Se un admin esiste, il pulsante è nascosto | Verifica comportamento |
| AC-F9 | Se nessun admin esiste, il pulsante è visibile | Verifica comportamento |
| AC-F10 | Dopo creazione riuscita, il pulsante scompare | Verifica comportamento |

#### Requisito F-4: Endpoint `GET /api/auth/adminExists`

**User Story:** Come sistema, ho bisogno di un endpoint che indichi se un utente admin esiste, per controllare la visibilità del pulsante.

**Criteri di accettazione:**

| # | Criterio | Verifica |
|---|---|---|
| AC-F11 | L'endpoint `GET /api/auth/adminExists` ritorna `true` se `userProfileRepository.count() > 0`, `false` altrimenti | Test di integrazione |
| AC-F12 | L'endpoint è accessibile pubblicamente (coperto da `/api/auth/**` permitAll) | Verifica SecurityConfig |
