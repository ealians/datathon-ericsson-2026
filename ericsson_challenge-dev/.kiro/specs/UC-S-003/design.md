# UC-S-003 createFirstUser Endpoint Bugfix Design

## Overview

L'endpoint `POST /api/auth/createFirstUser` presenta molteplici difetti di sicurezza e conformità architetturale: credenziali hardcoded, assenza di validazione input tramite DTO, esposizione della password encodata nella risposta, metodo non dichiarato nell'interfaccia del controller, path non definito come costante, e tipo di ritorno generico. Il fix ristruttura l'endpoint seguendo le convenzioni del progetto: introduce un DTO di richiesta con Jakarta Validation, un DTO di risposta sicuro, dichiara il metodo nell'interfaccia `AuthController`, definisce la costante in `Endpoints.java`, e rimuove ogni credenziale hardcoded dal codice sorgente.

## Glossary

- **Bug_Condition (C)**: L'insieme di condizioni che causano i difetti — invocazione di `createFirstUser` con credenziali hardcoded, senza validazione input, con risposta che espone dati sensibili, fuori dal contratto dell'interfaccia
- **Property (P)**: Il comportamento corretto — il primo utente admin viene creato con credenziali fornite dal client, validate, e la risposta non espone la password
- **Preservation**: Il comportamento esistente che non deve cambiare — prevenzione duplicati, creazione automatica ruoli, accessibilità pubblica dell'endpoint, assegnazione ruoli ADMIN+USER
- **createFirstUser**: Metodo in `AuthServiceImpl.java` che crea il primo utente admin nel database se non ne esistono altri
- **Endpoints.java**: Classe in `constants/` che centralizza tutti i path come costanti `public static final String`
- **AuthController**: Interfaccia in `controller/` che definisce il contratto del controller di autenticazione
- **CreateFirstUserRequestDto**: Nuovo DTO di input con validazione Jakarta per email, password, firstName, lastName
- **CreateFirstUserResponseDto**: Nuovo DTO di output che espone solo id, email, firstName, lastName e ruoli (senza password)

## Bug Details

### Bug Condition

Il bug si manifesta quando il metodo `createFirstUser` viene invocato: il sistema ignora qualsiasi dato fornito dal client e crea sempre un utente con credenziali fisse (`admin@elis.org` / `password`). Inoltre, il metodo opera fuori dal contratto dell'interfaccia, non valida l'input, e restituisce l'entità completa con password encodata.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type HttpRequest to POST /api/auth/createFirstUser
  OUTPUT: boolean

  RETURN (serviceMethod.usesHardcodedCredentials("admin@elis.org", "password")
         OR serviceMethod.acceptsRawHttpServletRequest()
         OR response.containsEncodedPassword()
         OR NOT authControllerInterface.declaresMethod("createFirstUser")
         OR NOT endpointsClass.definesConstant("CREATE_FIRST_USER")
         OR serviceMethod.returnType == "ResponseEntity<?>")
END FUNCTION
```

### Examples

- **Credenziali hardcoded**: Client invia `{"email": "custom@org.it", "password": "S3cure!"}` → il sistema ignora i dati e crea `admin@elis.org` con password `password`
- **Nessuna validazione**: Client invia `{}` (body vuoto) → il sistema non restituisce errore 400, procede con credenziali hardcoded
- **Password esposta**: Risposta HTTP contiene `{"password": "$2a$10$...hash..."}` → dato sensibile visibile al client
- **Interfaccia violata**: `AuthController.java` non dichiara `createFirstUser` → il metodo esiste solo in `AuthControllerImpl` fuori contratto
- **Path inline**: `@PostMapping("/createFirstUser")` usa stringa letterale → viola la convenzione di centralizzare i path in `Endpoints.java`
- **Tipo generico**: `ResponseEntity<?>` → il compilatore non può garantire la sicurezza del tipo di risposta

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Se esiste già almeno un utente nel database, la richiesta viene rifiutata con errore (prevenzione duplicati)
- I ruoli `ROLE_ADMIN` e `ROLE_USER` vengono creati automaticamente se non esistono
- I ruoli esistenti vengono riutilizzati senza crearne di duplicati
- L'endpoint rimane accessibile senza autenticazione (sotto `/api/auth/**`)
- Al primo utente vengono assegnati entrambi i ruoli `ROLE_ADMIN` e `ROLE_USER`
- Tutti gli altri endpoint di `AuthController` (`/login`, `/signup`, `/logout`, `/refreshToken`, ecc.) funzionano identicamente

**Scope:**
Tutti gli input che NON coinvolgono l'endpoint `createFirstUser` non devono essere impattati dal fix. Questo include:
- Login, signup, logout, refresh token
- Gestione profili utente
- Pagine web Thymeleaf
- Qualsiasi altra operazione del sistema

## Hypothesized Root Cause

Based on the code analysis, the root causes are clearly identifiable:

1. **Credenziali Hardcoded nel Service**: In `AuthServiceImpl.createFirstUser()`, le righe `user.setEmail("admin@elis.org")` e `user.setPassword(passwordEncoder.encode("password"))` impostano valori fissi ignorando qualsiasi input dal client

2. **Parametro HttpServletRequest non utilizzato**: Il metodo accetta `HttpServletRequest req` ma non lo usa per estrarre dati — è un artefatto di design scorretto. Il pattern corretto prevede un DTO con `@RequestBody @Valid`

3. **Restituzione entità JPA diretta**: `return ResponseEntity.ok(user)` restituisce l'oggetto `UserProfile` completo, inclusa la password encodata nel campo `password`, perché non viene usato un DTO di risposta

4. **Assenza dal contratto dell'interfaccia**: `createFirstUser` è definito solo in `AuthControllerImpl` con `@PostMapping("/createFirstUser")` ma non è dichiarato in `AuthController` interface

5. **Path non centralizzato**: La stringa `"/createFirstUser"` è inline nell'annotation `@PostMapping` anziché referenziare una costante da `Endpoints.java`

6. **Tipo di ritorno generico**: Sia nell'interfaccia `AuthService` che nel controller, il tipo è `ResponseEntity<?>` anziché un DTO tipizzato

## Correctness Properties

Property 1: Bug Condition - createFirstUser accetta credenziali dal client e non espone dati sensibili

_For any_ richiesta HTTP valida all'endpoint `createFirstUser` con email, password, firstName e lastName forniti nel body, la funzione corretta SHALL creare l'utente con le credenziali fornite dal client (non hardcoded), validare l'input con Jakarta Validation, e restituire un DTO di risposta tipizzato che NON contiene la password (né in chiaro né encodata).

**Validates: Requirements 2.1, 2.2, 2.3, 2.6**

Property 2: Preservation - Comportamento invariato per input non-bug

_For any_ input che NON coinvolge l'endpoint `createFirstUser` (login, signup, logout, refresh token, gestione profili), oppure invocazioni di `createFirstUser` quando esiste già un utente nel database, la funzione corretta SHALL produrre esattamente lo stesso risultato della funzione originale, preservando la prevenzione duplicati, la creazione automatica ruoli, e l'accessibilità pubblica dell'endpoint.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `src/main/java/org/elis/ericsson/datathon/user_management/model/dto/request/CreateFirstUserRequestDto.java`

**Action**: Creare nuovo DTO di richiesta

```java
package org.elis.ericsson.datathon.user_management.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFirstUserRequestDto {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
}
```

---

**File**: `src/main/java/org/elis/ericsson/datathon/user_management/model/dto/response/CreateFirstUserResponseDto.java`

**Action**: Creare nuovo DTO di risposta (senza password)

```java
package org.elis.ericsson.datathon.user_management.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elis.ericsson.datathon.user_management.model.entity.Role;

import java.util.Collection;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateFirstUserResponseDto {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Collection<Role> roles;
}
```

---

**File**: `src/main/java/org/elis/ericsson/datathon/user_management/constants/Endpoints.java`

**Action**: Aggiungere costante per il path

```java
public static final String CREATE_FIRST_USER = "/createFirstUser";
```

---

**File**: `src/main/java/org/elis/ericsson/datathon/user_management/controller/AuthController.java`

**Action**: Dichiarare il metodo nell'interfaccia

```java
@PostMapping(Endpoints.CREATE_FIRST_USER)
ResponseEntity<CreateFirstUserResponseDto> createFirstUser(
        @RequestBody @Valid CreateFirstUserRequestDto requestDto) throws Exception;
```

---

**File**: `src/main/java/org/elis/ericsson/datathon/user_management/controller/impl/AuthControllerImpl.java`

**Action**: Aggiornare l'implementazione per usare il DTO e la costante

```java
@Override
@PostMapping(CREATE_FIRST_USER)
public ResponseEntity<CreateFirstUserResponseDto> createFirstUser(
        @RequestBody @Valid CreateFirstUserRequestDto requestDto) throws Exception {
    return authService.createFirstUser(requestDto);
}
```

---

**File**: `src/main/java/org/elis/ericsson/datathon/user_management/service/AuthService.java`

**Action**: Aggiornare la firma del metodo nell'interfaccia del service

```java
ResponseEntity<CreateFirstUserResponseDto> createFirstUser(
        CreateFirstUserRequestDto requestDto) throws Exception;
```

---

**File**: `src/main/java/org/elis/ericsson/datathon/user_management/service/impl/AuthServiceImpl.java`

**Action**: Rimuovere credenziali hardcoded, usare DTO in input/output

```java
@Override
public ResponseEntity<CreateFirstUserResponseDto> createFirstUser(
        CreateFirstUserRequestDto requestDto) throws Exception {
    try {
        logger.debug("Enter into AuthService.createFirstUser");
        // Check if the first user is already present.
        if (userProfileRepository.count() > 0)
            throw new Exception("First user already present!");

        // Get ADMIN and USER role.
        Optional<Role> ruoloAdmin = roleRepository.findByName("ROLE_ADMIN");
        Optional<Role> ruoloUser = roleRepository.findByName("ROLE_USER");
        ArrayList<Role> roles = new ArrayList<>();

        // If the roles are not present, create them.
        if (ruoloAdmin.isEmpty()) {
            Role role = new Role();
            role.setName("ROLE_ADMIN");
            roles.add(roleRepository.save(role));
        } else {
            roles.add(ruoloAdmin.get());
        }
        if (ruoloUser.isEmpty()) {
            Role role = new Role();
            role.setName("ROLE_USER");
            roles.add(roleRepository.save(role));
        } else {
            roles.add(ruoloUser.get());
        }

        // Use credentials from the request DTO (no hardcoded values)
        UserProfile user = new UserProfile();
        user.setEmail(requestDto.getEmail().toLowerCase());
        user.setFirstName(requestDto.getFirstName());
        user.setLastName(requestDto.getLastName());
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setRoles(roles);

        // Save the user in the database.
        try {
            userProfileRepository.save(user);
        } catch (Exception e) {
            throw new InvalidCredentialsException("User already present!");
        }

        // Build response DTO without password
        CreateFirstUserResponseDto responseDto = CreateFirstUserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles())
                .build();

        return ResponseEntity.ok(responseDto);
    } catch (Exception e) {
        logger.error("Error in AuthServiceImpl.createFirstUser " + e.getMessage());
        throw e;
    }
}
```

## Testing Strategy

### Validation Approach

La strategia di testing segue un approccio a due fasi: prima, evidenziare counterexample che dimostrano i bug sul codice non fixato, poi verificare che il fix funzioni correttamente e preservi il comportamento esistente.

### Exploratory Bug Condition Checking

**Goal**: Evidenziare counterexample che dimostrano i bug PRIMA di implementare il fix. Confermare o confutare l'analisi delle root cause. Se confutiamo, dovremo riformulare le ipotesi.

**Test Plan**: Scrivere test che invocano `createFirstUser` con dati custom e verificano che il sistema li utilizzi (anziché le credenziali hardcoded). Eseguire questi test sul codice NON fixato per osservare i fallimenti.

**Test Cases**:
1. **Hardcoded Credentials Test**: Invocare `createFirstUser` con `{"email": "custom@test.it", "password": "MyP@ss123"}` e asserire che l'utente creato abbia email `custom@test.it` (fallirà sul codice non fixato — creerà `admin@elis.org`)
2. **Validation Test**: Invocare `createFirstUser` con body vuoto `{}` e asserire risposta 400 (fallirà sul codice non fixato — accetterà qualsiasi input)
3. **Password Exposure Test**: Invocare `createFirstUser` con dati validi e asserire che la risposta JSON NON contenga il campo `password` (fallirà sul codice non fixato — esporrà la password encodata)
4. **Invalid Email Test**: Invocare con `{"email": "not-an-email", "password": "short"}` e asserire risposta 400 (fallirà sul codice non fixato — nessuna validazione)

**Expected Counterexamples**:
- L'utente creato ha sempre email `admin@elis.org` indipendentemente dall'input
- Richieste con body vuoto o invalido non producono errore 400
- La risposta contiene il campo `password` con hash BCrypt

### Fix Checking

**Goal**: Verificare che per tutti gli input dove la bug condition è vera, la funzione fixata produce il comportamento atteso.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := createFirstUser_fixed(input)
  ASSERT result.email == input.email
  ASSERT result.firstName == input.firstName
  ASSERT result.lastName == input.lastName
  ASSERT result.password == null OR NOT present in response
  ASSERT result.roles contains ROLE_ADMIN AND ROLE_USER
  ASSERT response.statusCode == 200
END FOR
```

### Preservation Checking

**Goal**: Verificare che per tutti gli input dove la bug condition NON è vera, la funzione fixata produce lo stesso risultato della funzione originale.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT createFirstUser_original(input) = createFirstUser_fixed(input)
END FOR
```

**Testing Approach**: Property-based testing è raccomandato per il preservation checking perché:
- Genera automaticamente molti casi di test su tutto il dominio degli input
- Cattura edge case che i test manuali potrebbero mancare
- Fornisce garanzie forti che il comportamento è invariato per tutti gli input non-buggy

**Test Plan**: Osservare il comportamento sul codice NON fixato per login, signup, e altri endpoint, poi scrivere property-based test che catturano quel comportamento.

**Test Cases**:
1. **Duplicate Prevention Preservation**: Verificare che se esiste già un utente nel DB, `createFirstUser` continui a rifiutare la richiesta con errore
2. **Role Auto-Creation Preservation**: Verificare che i ruoli vengano creati automaticamente se non esistono
3. **Role Reuse Preservation**: Verificare che ruoli già esistenti vengano riutilizzati senza duplicazione
4. **Public Access Preservation**: Verificare che l'endpoint rimanga accessibile senza autenticazione
5. **Other Endpoints Preservation**: Verificare che login, signup, logout funzionino identicamente dopo il fix

### Unit Tests

- Test che `CreateFirstUserRequestDto` con campi validi passa la validazione Jakarta
- Test che `CreateFirstUserRequestDto` con email invalida fallisce validazione
- Test che `CreateFirstUserRequestDto` con password troppo corta fallisce validazione
- Test che `CreateFirstUserRequestDto` con campi blank fallisce validazione
- Test che `CreateFirstUserResponseDto` non ha campo password
- Test che `createFirstUser` con utente già presente lancia eccezione
- Test che `createFirstUser` crea ruoli se non esistono
- Test che `createFirstUser` riutilizza ruoli esistenti

### Property-Based Tests

- Generare DTO di richiesta random con email valide e password di lunghezza variabile (8-128 chars) → verificare che l'utente creato usi sempre le credenziali dal DTO
- Generare configurazioni random di ruoli preesistenti → verificare che il risultato contenga sempre entrambi i ruoli senza duplicati
- Generare sequenze di chiamate con DB già popolato → verificare che la prevenzione duplicati funzioni sempre
- Generare input random per altri endpoint (login, signup) → verificare che il comportamento sia identico pre e post fix

### Integration Tests

- Test end-to-end: POST `/api/auth/createFirstUser` con DTO valido su DB vuoto → verifica creazione utente con credenziali custom
- Test end-to-end: POST `/api/auth/createFirstUser` con DTO valido su DB con utente esistente → verifica rifiuto con errore
- Test end-to-end: POST `/api/auth/createFirstUser` con DTO invalido → verifica risposta 400
- Test end-to-end: POST `/api/auth/login` dopo fix → verifica che il login funzioni normalmente
- Test end-to-end: verifica che `/api/auth/createFirstUser` sia accessibile senza token JWT
