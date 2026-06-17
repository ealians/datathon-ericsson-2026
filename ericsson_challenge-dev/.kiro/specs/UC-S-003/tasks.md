# Implementation Plan

## Overview

Bugfix per l'endpoint `POST /api/auth/createFirstUser` (UC-S-003): rimozione credenziali hardcoded, introduzione DTO con Jakarta Validation, rimozione password dalla risposta, dichiarazione nell'interfaccia del controller, centralizzazione del path come costante in `Endpoints.java`, e creazione automatica del primo utente admin al primo avvio dell'applicazione.

## Tasks

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - createFirstUser ignora credenziali client e usa valori hardcoded
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists (hardcoded credentials, no validation, password exposure)
  - **Scoped PBT Approach**: Scope the property to concrete failing cases:
    - Invoke `POST /api/auth/createFirstUser` with custom DTO `{"email": "custom@test.it", "password": "MyP@ss123", "firstName": "Test", "lastName": "User"}` and assert the created user has email `custom@test.it` (not `admin@elis.org`)
    - Invoke with empty/invalid body `{}` and assert response status 400 (not 200)
    - Invoke with valid data and assert response JSON does NOT contain `password` field
  - Test that `createFirstUser` with valid `CreateFirstUserRequestDto` uses client-provided credentials
  - Test that invalid input produces HTTP 400
  - Test that response does not expose password
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists)
  - Document counterexamples found to understand root cause
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Comportamento invariato per prevenzione duplicati, gestione ruoli e altri endpoint
  - **IMPORTANT**: Follow observation-first methodology
  - **Observe on UNFIXED code**:
    - Observe: `createFirstUser` when DB already has a user → throws Exception "First user already present!"
    - Observe: `createFirstUser` when `ROLE_ADMIN` does not exist → creates it automatically
    - Observe: `createFirstUser` when `ROLE_ADMIN` already exists → reuses it without duplication
    - Observe: `/api/auth/login` with valid credentials → returns AuthResponseDTO with JWT token
  - Write property-based tests capturing observed behavior:
    - For all states where `userProfileRepository.count() > 0`, `createFirstUser` rejects with error
    - For all role configurations (existing/missing), the system ensures both `ROLE_ADMIN` and `ROLE_USER` are present after execution
    - For all valid login requests, the login endpoint returns the same response structure pre and post fix
  - Verify tests pass on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 3. Fix for createFirstUser endpoint - rimozione credenziali hardcoded e conformità architetturale

  - [x] 3.1 Create `CreateFirstUserRequestDto.java` con Jakarta Validation
    - Create file at `src/main/java/org/elis/ericsson/datathon/user_management/model/dto/request/CreateFirstUserRequestDto.java`
    - Add fields: `email` (`@NotBlank @Email`), `password` (`@NotBlank @Size(min=8, max=128)`), `firstName` (`@NotBlank`), `lastName` (`@NotBlank`)
    - Use `@Data` Lombok annotation consistent with existing DTOs (e.g., `SignUpRequestDto`)
    - _Requirements: 2.2_

  - [x] 3.2 Create `CreateFirstUserResponseDto.java` senza campo password
    - Create file at `src/main/java/org/elis/ericsson/datathon/user_management/model/dto/response/CreateFirstUserResponseDto.java`
    - Add fields: `id` (Long), `email` (String), `firstName` (String), `lastName` (String), `roles` (Collection<Role>)
    - Use `@Data @Builder @AllArgsConstructor @NoArgsConstructor` Lombok annotations
    - Do NOT include `password` field
    - _Requirements: 2.3_

  - [x] 3.3 Add `CREATE_FIRST_USER` constant to `Endpoints.java`
    - Add `public static final String CREATE_FIRST_USER = "/createFirstUser";` to `src/main/java/org/elis/ericsson/datathon/user_management/constants/Endpoints.java`
    - _Requirements: 2.5_

  - [x] 3.4 Declare `createFirstUser` method in `AuthController.java` interface
    - Add method declaration: `@PostMapping(Endpoints.CREATE_FIRST_USER) ResponseEntity<CreateFirstUserResponseDto> createFirstUser(@RequestBody @Valid CreateFirstUserRequestDto requestDto) throws Exception;`
    - Add necessary imports for `CreateFirstUserRequestDto`, `CreateFirstUserResponseDto`, `Endpoints`
    - _Requirements: 2.4_

  - [x] 3.5 Update `AuthControllerImpl.java` to use DTO and constant
    - Replace current `createFirstUser(HttpServletRequest req)` with `createFirstUser(@RequestBody @Valid CreateFirstUserRequestDto requestDto)`
    - Change return type from `ResponseEntity<?>` to `ResponseEntity<CreateFirstUserResponseDto>`
    - Add `@Override` annotation
    - Replace `"/createFirstUser"` string literal with `CREATE_FIRST_USER` constant (static import from Endpoints)
    - Delegate to `authService.createFirstUser(requestDto)`
    - _Requirements: 2.4, 2.5_

  - [x] 3.6 Update `AuthService.java` interface signature
    - Change `ResponseEntity<?> createFirstUser(HttpServletRequest req) throws Exception;` to `ResponseEntity<CreateFirstUserResponseDto> createFirstUser(CreateFirstUserRequestDto requestDto) throws Exception;`
    - Add necessary imports
    - _Requirements: 2.6_

  - [x] 3.7 Update `AuthServiceImpl.java` - remove hardcoded credentials and use DTOs
    - Replace `user.setEmail("admin@elis.org")` with `user.setEmail(requestDto.getEmail().toLowerCase())`
    - Replace `user.setFirstName("firstName_admin")` with `user.setFirstName(requestDto.getFirstName())`
    - Replace `user.setLastName("lastName_admin")` with `user.setLastName(requestDto.getLastName())`
    - Replace `user.setPassword(passwordEncoder.encode("password"))` with `user.setPassword(passwordEncoder.encode(requestDto.getPassword()))`
    - Change method signature to accept `CreateFirstUserRequestDto requestDto` and return `ResponseEntity<CreateFirstUserResponseDto>`
    - Build `CreateFirstUserResponseDto` with builder pattern (id, email, firstName, lastName, roles) — NO password
    - Return `ResponseEntity.ok(responseDto)` instead of `ResponseEntity.ok(user)`
    - Preserve: duplicate prevention (`userProfileRepository.count() > 0` check), role auto-creation logic, role reuse logic, both roles assigned
    - _Requirements: 2.1, 2.2, 2.3, 2.6, 3.1, 3.2, 3.3, 3.5_

  - [x] 3.8 Verify bug condition exploration test now passes
    - Re-run the SAME test from task 1 - do NOT write a new test
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 3.9 Verify preservation tests still pass
    - Re-run the SAME tests from task 2 - do NOT write new tests
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 4. Implement automatic first user creation at application startup
  - Create a `DataInitializer` component that invokes `createFirstUser` at application startup to seed the default admin user
  - Create class `src/main/java/org/elis/ericsson/datathon/user_management/configuration/DataInitializer.java`
  - The class SHALL implement `CommandLineRunner` (or use `@EventListener(ApplicationReadyEvent.class)`)
  - At startup, check if the database is empty (`userProfileRepository.count() == 0`)
  - IF empty, invoke `authService.createFirstUser(requestDto)` with a `CreateFirstUserRequestDto` containing:
    - `email = "admin@elis.org"`
    - `password = "password"`
    - `firstName = "firstName_admin"`
    - `lastName = "lastName_admin"`
  - IF the database already has users, skip silently (log info message "First user already present, skipping initialization")
  - Use constructor injection for `AuthService` dependency
  - Wrap the call in try/catch and log any errors without crashing the application
  - This ensures backward compatibility: the system still creates `admin@elis.org` / `password` at first boot, but now through the validated DTO-based endpoint rather than hardcoded values in the service layer
  - _Requirements: 3.4 (endpoint pubblico accessibile), Preservation: utente seed `admin@elis.org` / `password` creato al primo avvio_

- [x] 5. Write integration test for automatic startup initialization
  - Write an integration test verifying that on an empty database, the application startup creates an admin user with email `admin@elis.org`
  - Verify the user has both `ROLE_ADMIN` and `ROLE_USER` roles assigned
  - Verify that if a user already exists, the startup does NOT create a duplicate
  - Verify the admin user can successfully login via `POST /api/auth/login` with `{"email": "admin@elis.org", "password": "password"}`
  - _Requirements: 3.4, 3.5, Preservation_

- [x] 6. Checkpoint - Ensure all tests pass
  - Run full test suite via Docker (`docker-compose`)
  - Ensure exploration test (Property 1) passes after fix
  - Ensure preservation tests (Property 2) pass after fix
  - Ensure startup initialization test passes
  - Ensure all existing tests continue to pass (no regressions)
  - Ask the user if questions arise


## Task Dependency Graph

```json
{
  "waves": [
    {
      "wave": 1,
      "tasks": ["1", "2"],
      "description": "Write exploration and preservation tests BEFORE implementing the fix"
    },
    {
      "wave": 2,
      "tasks": ["3.1", "3.2", "3.3"],
      "description": "Create DTOs and endpoint constant"
    },
    {
      "wave": 3,
      "tasks": ["3.4", "3.5", "3.6", "3.7"],
      "description": "Update interfaces and implementation to use new DTOs"
    },
    {
      "wave": 4,
      "tasks": ["3.8", "3.9"],
      "description": "Verify exploration and preservation tests pass after fix"
    },
    {
      "wave": 5,
      "tasks": ["4"],
      "description": "Implement automatic first user creation at application startup (DataInitializer)"
    },
    {
      "wave": 6,
      "tasks": ["5"],
      "description": "Write integration test for startup initialization"
    },
    {
      "wave": 7,
      "tasks": ["6"],
      "description": "Final checkpoint - ensure all tests pass"
    }
  ]
}
```

## Notes

- Tasks 1 and 2 MUST be completed BEFORE task 3 (implementation)
- Task 1 is expected to FAIL on unfixed code — this is correct behavior confirming the bug
- Task 2 is expected to PASS on unfixed code — this captures baseline behavior
- Task 4 creates a `DataInitializer` that automatically seeds `admin@elis.org` / `password` at first boot via the new DTO-based endpoint
- All tests are run via Docker (`docker-compose`) as per project conventions
- The fix preserves backward compatibility: the default admin user is still created at first boot, but now through the validated, properly typed endpoint
