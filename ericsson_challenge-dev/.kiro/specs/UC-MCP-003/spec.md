# UC-MCP-003 — MCP Metrics Server (Actuator/Prometheus)

## Obiettivo

Fornire a Kiro un server MCP in Python (Docker, stdio) che interroga gli endpoint Spring Boot Actuator dell'applicazione per esporre metriche live in modalità read-only.

---

## Requisiti Funzionali

| ID | Requisito |
|----|-----------|
| RF-01 | L'applicazione Spring Boot deve esporre gli endpoint Actuator (health, info, metrics, prometheus) |
| RF-02 | Il server MCP deve esporre un tool per verificare lo stato di salute dell'app (`health`) |
| RF-03 | Il server MCP deve esporre un tool per elencare le metriche disponibili (`list_metrics`) |
| RF-04 | Il server MCP deve esporre un tool per ottenere il dettaglio di una specifica metrica (`get_metric`) |
| RF-05 | Il server MCP deve esporre un tool per ottenere le metriche in formato Prometheus (`prometheus`) |
| RF-06 | Tutte le operazioni devono essere read-only (solo GET HTTP) |
| RF-07 | Il server MCP comunica via stdio transport |

## Requisiti Non-Funzionali

| ID | Requisito |
|----|-----------|
| RNF-01 | Container Docker read-only con Python 3.11-slim |
| RNF-02 | URL base Actuator configurabile via variabile d'ambiente |
| RNF-03 | Timeout sulle richieste HTTP (max 5s) |
| RNF-04 | Nessun segreto hardcoded nel codice |

---

## Criteri di Accettazione

### AC-01: Health check

```gherkin
Given il server MCP metrics è in esecuzione
  And l'applicazione Spring Boot è attiva
When l'utente invoca il tool "health"
Then il server restituisce lo stato di salute (UP/DOWN) con i dettagli dei componenti
```

### AC-02: Lista metriche

```gherkin
Given il server MCP metrics è in esecuzione
  And Actuator è configurato sull'app
When l'utente invoca il tool "list_metrics"
Then il server restituisce l'elenco dei nomi delle metriche disponibili
```

### AC-03: Dettaglio metrica

```gherkin
Given il server MCP metrics è in esecuzione
  And la metrica "jvm.memory.used" esiste
When l'utente invoca il tool "get_metric" con name="jvm.memory.used"
Then il server restituisce il valore corrente, l'unità e le misurazioni disponibili
```

### AC-04: Metriche Prometheus

```gherkin
Given il server MCP metrics è in esecuzione
  And l'endpoint /actuator/prometheus è abilitato
When l'utente invoca il tool "prometheus"
Then il server restituisce il testo in formato Prometheus exposition
```

### AC-05: Input non valido

```gherkin
Given il server MCP metrics è in esecuzione
When l'utente invoca "get_metric" con name="../../etc/passwd"
Then il server restituisce un messaggio di errore senza effettuare la richiesta
```

### AC-06: App non raggiungibile

```gherkin
Given il server MCP metrics è in esecuzione
  And l'applicazione Spring Boot è spenta
When l'utente invoca qualsiasi tool
Then il server restituisce un messaggio di errore chiaro (es. "App non raggiungibile")
```
