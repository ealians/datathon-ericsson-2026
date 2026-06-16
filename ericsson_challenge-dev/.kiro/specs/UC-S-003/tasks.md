# Implementation Plan

## Overview

Verify and validate the bugfix for `createFirstUser` endpoint failure. Both fixes (DI injection of PasswordEncoder and @JsonIgnore annotations) are already applied. Tasks 1-4 focus on writing tests to confirm the bug conditions are resolved. Tasks 5-7 add a "Create First Admin" button to the login page with visibility control via a new `GET /api/auth/adminExists` endpoint.

## Tasks

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - createFirstUser fails due to missing DI and serialization issues
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: Since fixes are already applied, this test encodes the expected behavior and verifies it passes on the current (fixed) code
  - **GOAL**: Demonstrate that the bug conditions would cause failure
  - **Scoped PBT Approach**: Scope the property to the concrete failing cases:
    - C(X): `UserProfile` serialized via Jackson without `@JsonIgnore` on `password` → response contains password field
    - C(X): `UserProfile` serialized via Jackson without `@JsonIgnore` on `eggUpInfo` → StackOverflowError or infinite recursion
    - C(X): `AuthServiceImpl` or `CustomAuthenticationManager` uses `new BCryptPasswordEncoder()` instead of injected bean
  - Write integration test: POST `/api/auth/createFirstUser` on empty DB → verify HTTP 200, response JSON does NOT contain `password` field, no serialization error occurs
  - Write unit test: verify `AuthServiceImpl` and `CustomAuthenticationManager` use injected `PasswordEncoder` (not local instantiation)
  - Test file: `src/test/java/org/elis/ericsson/datathon/user_management/bugfix/CreateFirstUserBugConditionTest.java`
  - Run test on current (fixed) code — expect PASS (confirms bug is resolved)
  - Document: if `@JsonIgnore` were removed, serialization would expose password or cause StackOverflowError
  - _Requirements: AC-1, AC-2, AC-3, AC-5_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Existing auth and user behavior preserved
  - **IMPORTANT**: Follow observation-first methodology
  - **Observe on current code (fixes applied)**:
    - `registerUser` still creates users correctly with encoded password
    - `login` with valid credentials returns JWT (AuthResponseDTO, not UserProfile — not affected by serialization fix)
    - `createFirstUser` called twice → second call throws exception with "First user already present!"
    - User created by `createFirstUser` has roles ROLE_ADMIN and ROLE_USER
  - Write property-based tests capturing preservation guarantees:
    - For all valid login attempts after user creation: login returns HTTP 200 with JWT
    - For all `createFirstUser` calls on non-empty DB: exception is thrown (idempotency guard preserved)
    - For all serialized `UserProfile` responses: `roles` field is present and non-empty
    - BCrypt encoding/matching still works end-to-end (password encoded at creation, verified at login)
  - Test file: `src/test/java/org/elis/ericsson/datathon/user_management/bugfix/CreateFirstUserPreservationTest.java`
  - Run tests on current code — expect PASS (confirms baseline behavior is intact)
  - _Requirements: AC-4, AC-7, AC-8, AC-9_

- [x] 3. Verify fixes for createFirstUser endpoint

  - [x] 3.1 Verify Fix 1: PasswordEncoder DI injection is correct
    - Confirm `AuthServiceImpl` declares `private final PasswordEncoder passwordEncoder` (no `new BCryptPasswordEncoder()`)
    - Confirm `CustomAuthenticationManager` declares `private final PasswordEncoder passwordEncoder` (no `new BCryptPasswordEncoder()`)
    - Confirm both receive the bean via constructor injection (`@Autowired` or implicit single-constructor injection)
    - Confirm the bean is the same one defined in `SecurityConfig`
    - _Bug_Condition: isBugCondition(class) where class uses `new BCryptPasswordEncoder()` instead of injected bean_
    - _Expected_Behavior: PasswordEncoder is injected via Spring DI, same bean instance across all consumers_
    - _Preservation: BCrypt encode/matches behavior unchanged; login still works_
    - _Requirements: AC-5, AC-9_

  - [x] 3.2 Verify Fix 2: @JsonIgnore annotations on UserProfile
    - Confirm `password` field has `@JsonIgnore` annotation
    - Confirm `eggUpInfo` field has `@JsonIgnore` annotation
    - Confirm serialization of `UserProfile` produces JSON without `password` and without `eggUpInfo`
    - _Bug_Condition: isBugCondition(entity) where entity lacks @JsonIgnore on password/eggUpInfo_
    - _Expected_Behavior: JSON response excludes password and eggUpInfo fields; no StackOverflowError_
    - _Preservation: Other UserProfile fields (id, email, firstName, lastName, roles) still serialized correctly_
    - _Requirements: AC-1, AC-2, AC-3, AC-8_

  - [x] 3.3 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - createFirstUser returns clean JSON without password exposure
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: AC-1, AC-2, AC-3, AC-5_

  - [x] 3.4 Verify preservation tests still pass
    - **Property 2: Preservation** - Existing auth and user behavior preserved
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after fix (no regressions)
    - _Requirements: AC-4, AC-7, AC-8, AC-9_

- [x] 4. Checkpoint - Ensure all tests pass
  - Run `mvn test` (or via Docker if JAVA_HOME unavailable)
  - Ensure all pre-existing tests pass (no regressions): `EricssonDatathonProjectApplicationTests`, `JpaAuditingIntegrationTest`, `UserProfileAuditTest`
  - Ensure all new bugfix tests pass: `CreateFirstUserBugConditionTest`, `CreateFirstUserPreservationTest`
  - Verify no compilation errors
  - Ask the user if questions arise

- [x] 5. Add backend endpoint GET /api/auth/adminExists
  - [x] 5.1 Add ADMIN_EXISTS constant and adminExists method to service interface
    - Add `public static final String ADMIN_EXISTS = "/adminExists";` to `Endpoints.java`
    - Add `ResponseEntity<Boolean> adminExists();` to `AuthService.java`
    - _Requirements: AC-F11, AC-F12_

  - [x] 5.2 Implement adminExists in AuthServiceImpl
    - Add override method that returns `ResponseEntity.ok(userProfileRepository.count() > 0)`
    - Follows existing pattern of delegating to repository
    - _Requirements: AC-F11_

  - [x] 5.3 Add adminExists to AuthController interface and implement in AuthControllerImpl
    - Add `@GetMapping("/adminExists") ResponseEntity<Boolean> adminExists();` to interface
    - Add override implementation in `AuthControllerImpl` that delegates to `authService.adminExists()`
    - _Requirements: AC-F11, AC-F12_

- [x] 6. Implement frontend button and JavaScript logic on login.html
  - [x] 6.1 Add the Admin Creation Button and feedback alert HTML to login.html
    - Place `<button id="createAdminBtn">` after the `</form>` tag, inside `<div class="card-body">`
    - Use `btn btn-success w-100 mt-3` classes, `style="display: none;"` by default
    - Add `<div id="adminFeedback" class="mt-3" style="display: none;"></div>` below the button
    - _Requirements: AC-F1, AC-F2_

  - [x] 6.2 Add JavaScript functions for checkAdminExists, createFirstAdmin, and escapeHtml
    - `checkAdminExists()`: fetches `GET /api/auth/adminExists`, shows/hides button based on response
    - `createFirstAdmin()`: disables button, calls `POST /api/auth/createFirstUser`, shows success/error alert
    - `escapeHtml()`: sanitizes error text before DOM insertion (XSS protection)
    - Update `window.onload` to call `checkAdminExists()` alongside existing `checkAuthToken()`
    - _Requirements: AC-F3, AC-F4, AC-F5, AC-F6, AC-F7, AC-F8, AC-F9, AC-F10_

- [x] 7. Checkpoint - Verify admin creation button integration
  - Run `mvn test` to ensure no regressions from new code
  - Verify no compilation errors
  - Ask the user if questions arise

## Task Dependencies

- Task 1: No dependencies
- Task 2: No dependencies
- Task 3.1: Depends on 1, 2
- Task 3.2: Depends on 1, 2
- Task 3.3: Depends on 3.1, 3.2
- Task 3.4: Depends on 3.1, 3.2
- Task 4: Depends on 3.3, 3.4
- Task 5.1: Depends on 4
- Task 5.2: Depends on 5.1
- Task 5.3: Depends on 5.2
- Task 6.1: Depends on 5.3
- Task 6.2: Depends on 6.1
- Task 7: Depends on 6.2

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1", "2"] },
    { "id": 1, "tasks": ["3.1", "3.2"] },
    { "id": 2, "tasks": ["3.3", "3.4"] },
    { "id": 3, "tasks": ["4"] },
    { "id": 4, "tasks": ["5.1"] },
    { "id": 5, "tasks": ["5.2"] },
    { "id": 6, "tasks": ["5.3", "6.1"] },
    { "id": 7, "tasks": ["6.2"] },
    { "id": 8, "tasks": ["7"] }
  ]
}
```

## Notes

- Both Fix 1 (DI PasswordEncoder) and Fix 2 (@JsonIgnore) are already applied in the codebase
- Tasks 1-4 focus on verification and test coverage rather than code changes
- Tasks 5-7 implement the new "Create First Admin" button on the login page
- GlobalExceptionHandler is explicitly out of scope
- Tests use Spring Boot Test + MockMvc (integration) and JUnit 5 + Mockito (unit)
- Use JAVA_HOME from WSL, if not present use Docker to run tests
- No SecurityConfig changes needed for the new endpoint (covered by `/api/auth/**` permitAll)
- No new Maven dependencies required
