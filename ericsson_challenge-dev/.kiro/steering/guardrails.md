---
inclusion: always
---

# Project Guardrails

## Scope of Changes

- Only modify code explicitly requested by the user. Do not refactor, rename, or reorganize files outside the scope of the current task.
- Do not delete or move existing files unless instructed.
- Preserve existing coding patterns and naming conventions found in the codebase.

## Dependencies and External Libraries

- Do not add new Maven dependencies or external libraries unless the user explicitly asks for them.
- If a new dependency is genuinely required to complete a task, ask for approval before adding it to `pom.xml`.
- Do not upgrade existing dependency versions without explicit instruction.

## Architecture Conventions

- Follow the existing layered architecture: `controller` → `service` → `repository`.
- REST controllers go in `controller/impl/`; interfaces go in `controller/`.
- Web (Thymeleaf) controllers go in `controller/web/`.
- Service interfaces go in `service/`; implementations go in `service/impl/`.
- DTOs go in `model/dto/`; entities go in `model/entity/`; exceptions go in `model/exception/`.
- Constants and configuration classes live in their respective packages (`constants/`, `configuration/`).
- New code must reside within `org.elis.ericsson.datathon.user_management` and its sub-packages.

## Code Style

- Use Lombok annotations (`@Data`, `@Builder`, `@AllArgsConstructor`, etc.) consistent with existing entities and DTOs.
- Use constructor injection (not field injection) for Spring beans.
- Annotate validation constraints on DTOs using Jakarta Validation (`@NotNull`, `@Email`, `@Size`, etc.).
- Keep controller methods thin — business logic belongs in service implementations.
- Use the constants defined in `Endpoints.java`, `SecurityConstants.java`, and `ExceptionMessages.java` rather than inline strings.

## Security

- Never expose credentials, secrets, API keys, or tokens in source code, logs, or comments.
- Store sensitive values in environment variables or Spring configuration files (`application.properties` / `application-docker.properties`), never hardcoded in Java classes.
- Do not disable or weaken the existing Spring Security filter chain without explicit instruction.
- Maintain stateless session management (no server-side sessions).
- Do not execute arbitrary shell scripts. Only use build-related commands: `mvn`, `javac`, `docker`, `git`.
- If a non-standard command is needed, ask the user before running it.

## Database

- Do not perform destructive DDL operations (`ALTER TABLE`, `DROP TABLE`, `TRUNCATE`) unless the task specification explicitly requires it.
- Use JPA entity mappings and Spring Data repositories for all data access. Do not write raw SQL unless specifically asked.
- Respect the existing JPA auditing pattern (`DateAudit` superclass with `createdAt` / `updatedAt`).

## Output Confinement

- All generated output (files, resources, configurations) for a given use case must remain within the corresponding project folder.
- Do not write files outside the workspace directory.

## Testing

- Do not delete or disable existing tests.
- When adding new functionality, include or suggest corresponding unit tests in `src/test/`.
- Use the existing test framework and patterns already present in the project.

## Test environment
- If JAVA_HOME is not avaialable, use docker to run test cases

