# UC-SEC-001 — Tasks

## Fase 1: CRITICAL — Secrets & Access Control

- [x] 1. **V-001** — `SecurityConstants.java`: JWT_SECRET letto da `System.getenv("JWT_SECRET")` con fallback 64+ bytes ✓
- [x] 2. **V-002** — `application.properties`: Password sostituita con `${DB_PASSWORD:dev_password}` ✓
- [x] 3. **V-003** — `AuthServiceImpl.createFirstUser()`: Ritorna 403 se DB popolato, password da env var `ADMIN_INITIAL_PASSWORD` ✓
- [x] 4. **docker-compose.yml**: Aggiunti `JWT_SECRET`, `ADMIN_INITIAL_PASSWORD`, `CORS_ALLOWED_ORIGINS` ✓

## Fase 2: HIGH — Input Validation & IDOR

- [x] 5. **V-004** — `LoginDto.java` e `SignUpRequestDto.java`: Password rimossa da `toString()` ✓
- [x] 6. **V-006** — `LoginDto.java`: Aggiunti `@NotBlank` e `@Email` ✓
- [x] 7. **V-010** — `SignUpRequestDto.java`: Aggiunto `@Size(min = 8)` su password ✓
- [x] 8. **V-007** — `UserProfileWebController.editProfile()`: Aggiunto check IDOR (id == principal oppure ADMIN) ✓
- [x] 9. **V-005** — `CorsConfig.java`: Origin da env var `CORS_ALLOWED_ORIGINS`, default `http://localhost:8080` ✓

## Fase 3: MEDIUM — Hardening

- [x] 10. **V-008** — `JwtUtility.java`: Usa `Jwts.parserBuilder().setSigningKey(key).build()` ✓
- [x] 11. **V-009** — `SecurityConfig.java`: `frameOptions().deny()`, `contentTypeOptions` abilitato ✓
- [x] 12. **V-012** — `SecurityConstants.java`: `TOKEN_EXPIRATION = 15 * 60 * 1000` (15 min) ✓

## Fase 4: LOW — Cleanup

- [x] 13. **V-011** — `AuthServiceImpl.registerUser()`: Ritorna entity sanitizzata (solo id, email, name) con HTTP 201 ✓

## Fase 5: Verifica

- [x] 14. Rebuild container app: `docker compose build app` ✓
- [x] 15. AC-01: JWT_SECRET letto da env, fallback ≥64 bytes ✓
- [x] 16. AC-02: POST /api/auth/createFirstUser con DB popolato → 403 ✓
- [x] 17. AC-03: Login loggato senza password visibile nei log ✓
- [x] 18. AC-04: CORS configurato con origin restrittivi (env var) ✓
- [x] 19. AC-05: Login con body vuoto → MethodArgumentNotValidException (validazione attiva) ✓
- [x] 20. AC-06: Edit profilo altrui protetto con check IDOR ✓
- [x] 21. AC-07: Registrazione con password "abc" → MethodArgumentNotValidException (min 8 char) ✓
- [x] 22. AC-08: X-Frame-Options: DENY, X-Content-Type-Options: nosniff presenti ✓
