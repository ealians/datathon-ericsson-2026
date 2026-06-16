# UC-B-001 – Bugfix: Abilitazione JPA Auditing mancante

## Problema

L'applicazione non dispone dell'annotazione `@EnableJpaAuditing` necessaria per attivare l'infrastruttura di auditing di Spring Data JPA. Tutte le entità del progetto estendono `DateAudit`, che utilizza `@EntityListeners(AuditingEntityListener.class)` con i campi `@CreatedDate` e `@LastModifiedDate`. Senza l'abilitazione esplicita dell'auditing, il contesto applicativo non si avvia correttamente e i test JPA falliscono.

## Root Cause

| Componente | Stato |
|---|---|
| `DateAudit` → `@EntityListeners(AuditingEntityListener.class)` | ✓ Presente |
| `@CreatedDate` / `@LastModifiedDate` sui campi audit | ✓ Presente |
| `@EnableJpaAuditing` in una classe `@Configuration` o `@SpringBootApplication` | ✗ **Mancante** |

## Entità impattate

- `UserProfile`
- `Role`
- `RefreshToken`
- `PasswordResetToken`
- `EggUpInfo`
- `EggUpScore`
- `EggUpTrait`

---

## Modifiche richieste

### 1. Creare la classe di configurazione `JpaAuditingConfig`

**Path**: `src/main/java/org/elis/ericsson/datathon/user_management/configuration/JpaAuditingConfig.java`

```java
package org.elis.ericsson.datathon.user_management.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

**Motivazione**: Separare la configurazione di auditing in una classe dedicata (come da convenzione architetturale del progetto: le configurazioni risiedono nel package `configuration/`). Questo evita di sovraccaricare la classe principale e rende la configurazione più testabile in isolamento.

---

## Specifiche di test

### Test 1: Avvio del contesto applicativo (regressione)

**Classe**: `EricssonDatathonProjectApplicationTests` (esistente)  
**Metodo**: `contextLoads()`  
**Scopo**: Verificare che il contesto Spring si avvii senza errori dopo l'aggiunta della configurazione.  
**Comportamento atteso**: Il test passa senza eccezioni.

### Test 2: Popolamento automatico campi audit su persistenza

**Classe**: `src/test/java/org/elis/ericsson/datathon/user_management/configuration/JpaAuditingIntegrationTest.java`

```java
package org.elis.ericsson.datathon.user_management.configuration;

import org.elis.ericsson.datathon.user_management.model.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class JpaAuditingIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void whenEntityPersisted_thenCreatedAtIsPopulated() {
        Role role = new Role("ROLE_TEST");
        Role saved = entityManager.persistAndFlush(role);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void whenEntityUpdated_thenUpdatedAtChanges() {
        Role role = new Role("ROLE_TEST");
        Role saved = entityManager.persistAndFlush(role);

        saved.setName("ROLE_UPDATED");
        entityManager.persistAndFlush(saved);
        entityManager.clear();

        Role reloaded = entityManager.find(Role.class, saved.getId());
        assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(reloaded.getCreatedAt());
    }
}
```

### Test 3: Verifica audit su entità UserProfile

**Classe**: `src/test/java/org/elis/ericsson/datathon/user_management/configuration/UserProfileAuditTest.java`

```java
package org.elis.ericsson.datathon.user_management.configuration;

import org.elis.ericsson.datathon.user_management.model.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserProfileAuditTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void whenUserProfilePersisted_thenAuditFieldsAreSet() {
        UserProfile user = new UserProfile();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPassword("encoded_password");

        UserProfile saved = entityManager.persistAndFlush(user);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
```

---

## Criteri di accettazione

| # | Criterio | Verifica |
|---|---|---|
| AC-1 | Il test `contextLoads()` esistente passa senza errori | `mvn test -Dtest=EricssonDatathonProjectApplicationTests` → BUILD SUCCESS |
| AC-2 | Alla persistenza di qualsiasi entità che estende `DateAudit`, il campo `created_at` viene popolato automaticamente con il timestamp corrente | Test `whenEntityPersisted_thenCreatedAtIsPopulated` → PASS |
| AC-3 | Alla persistenza di qualsiasi entità che estende `DateAudit`, il campo `updated_at` viene popolato automaticamente | Test `whenEntityPersisted_thenCreatedAtIsPopulated` → PASS |
| AC-4 | All'aggiornamento di un'entità, il campo `updated_at` viene aggiornato con un valore ≥ `created_at` | Test `whenEntityUpdated_thenUpdatedAtChanges` → PASS |
| AC-5 | Nessuna regressione sui test esistenti | `mvn test` → tutti i test pre-esistenti passano |
| AC-6 | La classe `JpaAuditingConfig` risiede nel package `configuration/` secondo le guardrails architetturali | Verifica path file |
| AC-7 | Nessuna modifica alla classe `EricssonDatathonProjectApplication` | La classe principale rimane invariata |
| AC-8 | Nessuna nuova dipendenza Maven aggiunta | `pom.xml` invariato |

---

## Note implementative

- La fix è minimale e non invasiva: un solo file nuovo (`JpaAuditingConfig.java`)
- Nessuna modifica alle entità esistenti
- Nessuna modifica al `pom.xml`
- Compatibile con entrambi i profili (H2 dev, PostgreSQL docker)
- Segue il pattern architetturale del progetto (configurazioni nel package `configuration/`)
