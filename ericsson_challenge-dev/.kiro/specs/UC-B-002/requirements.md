# UC-B-002 – Bugfix: Rimozione @EnableAutoConfiguration dalle classi Entity

## Problema

L'annotazione `@EnableAutoConfiguration` (da `org.springframework.boot.autoconfigure.EnableAutoConfiguration`) è stata erroneamente posizionata sulle classi JPA Entity `Role` e `RefreshToken`. Questa annotazione è un'annotazione di configurazione Spring Boot che deve essere presente esclusivamente su classi `@Configuration` o sulla classe principale dell'applicazione (`@SpringBootApplication`). La sua presenza su un'entità può causare comportamenti inattesi di auto-configurazione, conflitti tra bean e viola le best practice di Spring Boot.

Inoltre, la classe `Role` presenta l'annotazione `@EntityListeners(AuditingEntityListener.class)` ridondante, poiché la superclasse `DateAudit` la dichiara già tramite `@MappedSuperclass`.

## Root Cause

| Componente | File | Stato |
|---|---|---|
| `@EnableAutoConfiguration` su `Role` | `model/entity/Role.java` | ✗ **Errato** – deve essere rimossa |
| `@EnableAutoConfiguration` su `RefreshToken` | `model/entity/RefreshToken.java` | ✗ **Errato** – deve essere rimossa |
| `@EntityListeners(AuditingEntityListener.class)` su `Role` | `model/entity/Role.java` | ✗ **Ridondante** – ereditato da `DateAudit` |
| `@EntityListeners(AuditingEntityListener.class)` su `DateAudit` | `model/modelbase/DateAudit.java` | ✓ Presente e corretto |
| `@EnableAutoConfiguration` su altre entità | `UserProfile`, `PasswordResetToken`, `EggUpInfo`, `EggUpScore`, `EggUpTrait` | ✓ Assente (corretto) |

## Entità impattate

- `Role` → rimuovere `@EnableAutoConfiguration` + `@EntityListeners(AuditingEntityListener.class)` ridondante + import inutilizzati
- `RefreshToken` → rimuovere `@EnableAutoConfiguration` + import inutilizzato

---

## Modifiche richieste

### 1. Rimuovere `@EnableAutoConfiguration` e `@EntityListeners` ridondante da `Role.java`

**Path**: `src/main/java/org/elis/ericsson/datathon/user_management/model/entity/Role.java`

**Prima (codice errato):**
```java
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EnableAutoConfiguration
@EntityListeners(AuditingEntityListener.class)
public class Role extends DateAudit {
```

**Dopo (codice corretto):**
```java
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Role extends DateAudit {
```

**Motivazione**: 
- `@EnableAutoConfiguration` è un'annotazione di configurazione Spring Boot, non appartiene a un'entità JPA
- `@EntityListeners(AuditingEntityListener.class)` è già dichiarata sulla superclasse `DateAudit` con `@MappedSuperclass`, quindi viene ereditata automaticamente
- Gli import `org.springframework.boot.autoconfigure.EnableAutoConfiguration` e `org.springframework.data.jpa.domain.support.AuditingEntityListener` diventano inutilizzati e devono essere rimossi

### 2. Rimuovere `@EnableAutoConfiguration` da `RefreshToken.java`

**Path**: `src/main/java/org/elis/ericsson/datathon/user_management/model/entity/RefreshToken.java`

**Prima (codice errato):**
```java
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

@EnableAutoConfiguration
@Getter
@Setter
@Entity
@Table(name = "refresh_token")
public class RefreshToken extends DateAudit {
```

**Dopo (codice corretto):**
```java
@Getter
@Setter
@Entity
@Table(name = "refresh_token")
public class RefreshToken extends DateAudit {
```

**Motivazione**: `@EnableAutoConfiguration` non ha alcun significato su una classe `@Entity` e deve essere rimossa insieme al relativo import.

---

## Specifiche di test

### Test 1: Avvio del contesto applicativo (regressione)

**Classe**: `EricssonDatathonProjectApplicationTests` (esistente)  
**Metodo**: `contextLoads()`  
**Scopo**: Verificare che il contesto Spring si avvii senza errori dopo la rimozione delle annotazioni.  
**Comportamento atteso**: Il test passa senza eccezioni.

### Test 2: Persistenza entità Role dopo la fix

**Classe**: `src/test/java/org/elis/ericsson/datathon/user_management/model/entity/RoleEntityTest.java`

```java
package org.elis.ericsson.datathon.user_management.model.entity;

import org.elis.ericsson.datathon.user_management.configuration.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class RoleEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void roleClassShouldNotHaveEnableAutoConfiguration() {
        assertThat(Role.class.getAnnotation(EnableAutoConfiguration.class)).isNull();
    }

    @Test
    void whenRolePersisted_thenAuditFieldsArePopulated() {
        Role role = new Role("ROLE_TEST");
        Role saved = entityManager.persistAndFlush(role);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
```

### Test 3: Persistenza entità RefreshToken dopo la fix

**Classe**: `src/test/java/org/elis/ericsson/datathon/user_management/model/entity/RefreshTokenEntityTest.java`

```java
package org.elis.ericsson.datathon.user_management.model.entity;

import org.elis.ericsson.datathon.user_management.configuration.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class RefreshTokenEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void refreshTokenClassShouldNotHaveEnableAutoConfiguration() {
        assertThat(RefreshToken.class.getAnnotation(EnableAutoConfiguration.class)).isNull();
    }

    @Test
    void whenRefreshTokenPersisted_thenAuditFieldsArePopulated() {
        UserProfile user = new UserProfile();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPassword("encoded_password");
        UserProfile savedUser = entityManager.persistAndFlush(user);

        RefreshToken token = new RefreshToken();
        token.setUser(savedUser);
        token.setToken("test-refresh-token-123");
        token.setExpiryDate(Instant.now().plusSeconds(3600));
        RefreshToken saved = entityManager.persistAndFlush(token);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
```

### Test 4: Verifica assenza annotazione su tutte le entità (property-based)

**Classe**: `src/test/java/org/elis/ericsson/datathon/user_management/model/entity/NoEnableAutoConfigOnEntitiesTest.java`

```java
package org.elis.ericsson.datathon.user_management.model.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class NoEnableAutoConfigOnEntitiesTest {

    @ParameterizedTest(name = "Entity {0} should not have @EnableAutoConfiguration")
    @ValueSource(classes = {
            Role.class,
            RefreshToken.class,
            UserProfile.class,
            PasswordResetToken.class
    })
    void entityShouldNotHaveEnableAutoConfiguration(Class<?> entityClass) {
        assertThat(entityClass.getAnnotation(EnableAutoConfiguration.class))
                .as("@EnableAutoConfiguration should not be present on %s", entityClass.getSimpleName())
                .isNull();
    }
}
```

---

## Criteri di accettazione

| # | Criterio | Verifica |
|---|---|---|
| AC-1 | L'annotazione `@EnableAutoConfiguration` è rimossa da `Role.java` | Ispezione codice + test `roleClassShouldNotHaveEnableAutoConfiguration` |
| AC-2 | L'annotazione `@EnableAutoConfiguration` è rimossa da `RefreshToken.java` | Ispezione codice + test `refreshTokenClassShouldNotHaveEnableAutoConfiguration` |
| AC-3 | L'annotazione `@EntityListeners(AuditingEntityListener.class)` ridondante è rimossa da `Role.java` | Ispezione codice |
| AC-4 | Gli import inutilizzati (`EnableAutoConfiguration`, `AuditingEntityListener`) sono rimossi | Ispezione codice |
| AC-5 | L'auditing JPA continua a funzionare (campi `createdAt`/`updatedAt` popolati) su `Role` | Test `whenRolePersisted_thenAuditFieldsArePopulated` → PASS |
| AC-6 | L'auditing JPA continua a funzionare su `RefreshToken` | Test `whenRefreshTokenPersisted_thenAuditFieldsArePopulated` → PASS |
| AC-7 | Il test `contextLoads()` esistente passa senza errori | `mvn test -Dtest=EricssonDatathonProjectApplicationTests` → BUILD SUCCESS |
| AC-8 | Nessuna regressione sui test esistenti | `mvn test` → tutti i test pre-esistenti passano |
| AC-9 | Nessuna nuova dipendenza Maven aggiunta | `pom.xml` invariato |
| AC-10 | Nessuna entità del progetto contiene `@EnableAutoConfiguration` | Test parametrizzato `NoEnableAutoConfigOnEntitiesTest` → PASS |

---

## Note implementative

- La fix è minimale e non invasiva: solo rimozione di annotazioni e import errati
- Nessun file nuovo di produzione (solo test nuovi)
- Nessuna modifica alle entità non affette (`UserProfile`, `PasswordResetToken`, `EggUpInfo`, `EggUpScore`, `EggUpTrait`)
- Nessuna modifica al `pom.xml`
- Nessuna modifica alla logica di business
- L'auditing JPA è garantito dalla superclasse `DateAudit` (annotata con `@EntityListeners(AuditingEntityListener.class)` e `@MappedSuperclass`)
- Compatibile con entrambi i profili (H2 dev, PostgreSQL docker)
