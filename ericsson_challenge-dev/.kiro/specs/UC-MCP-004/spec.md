# UC-MCP-004 — MCP Server: Natural Language Queries su DB H2

## Obiettivo

Server MCP (Model Context Protocol) locale in Python che gira su Docker, capace di accettare query in linguaggio naturale dall'utente via Kiro, tradurle in SQL, e eseguirle sul database H2 dell'applicazione Spring Boot in profilo dev. Ogni query generata richiede approvazione esplicita dell'utente prima dell'esecuzione.

---

## Requisiti Funzionali

| ID | Requisito |
|----|-----------|
| RF-01 | Il server MCP deve esporre un tool `nl_query` che accetta una domanda in linguaggio naturale e restituisce la query SQL proposta (senza eseguirla) |
| RF-02 | Il server MCP deve esporre un tool `execute_approved_query` che esegue una query SQL precedentemente proposta e approvata dall'utente |
| RF-03 | Il server MCP deve esporre un tool `get_schema` che restituisce lo schema del database H2 per contesto |
| RF-04 | Tutte le operazioni devono essere read-only (solo SELECT) |
| RF-05 | Il server comunica via stdio transport |
| RF-06 | Il tool `nl_query` deve generare SQL basandosi sullo schema del database e sulla domanda NL ricevuta |
| RF-07 | Il flusso richiede approvazione esplicita: nl_query propone → utente approva → execute_approved_query esegue |

## Requisiti Non-Funzionali

| ID | Requisito |
|----|-----------|
| RNF-01 | Container Docker read-only con Python 3.11-slim |
| RNF-02 | Connessione al database H2 via TCP (modalità server) esposta dall'app Spring Boot |
| RNF-03 | Nessun segreto hardcoded nel codice |
| RNF-04 | Isolation strategy documentata in guardrails.md |
| RNF-05 | Dipendenze minime: `mcp[cli]`, `jaydebeapi` (JDBC driver per H2) |
| RNF-06 | Timeout sulle query (max 5s) |

---

## Isolation Strategy

Il server MCP per query NL opera con le seguenti garanzie di isolamento:

1. **Read-only enforcement**: Regex blocca qualsiasi keyword SQL non-SELECT prima dell'esecuzione
2. **Two-step approval**: La traduzione NL→SQL e l'esecuzione sono tool separati; l'utente vede e approva la query prima che venga eseguita
3. **Connection isolation**: Connessione JDBC in modalità read-only (`SET SESSION CHARACTERISTICS AS TRANSACTION READ ONLY`)
4. **Scope limitato**: Il server accede solo al database H2 dev, non al PostgreSQL di produzione
5. **No data mutation**: Nessun tool di seed/clean — ambiente dev pre-esistente, zero scritture
6. **Container read-only**: Filesystem del container in sola lettura

---

## Criteri di Accettazione

### AC-01: Schema disponibile

```gherkin
Given il server MCP NL è in esecuzione
  And il database H2 dell'app è attivo in modalità server TCP
When l'utente invoca il tool "get_schema"
Then viene restituito l'elenco completo delle tabelle con colonne e tipi
```

### AC-02: Traduzione NL → SQL

```gherkin
Given il server MCP NL è in esecuzione
When l'utente invoca "nl_query" con question="Quanti utenti ci sono?"
Then il server restituisce una query SQL proposta (es. "SELECT COUNT(*) FROM users")
  And la query NON viene eseguita
```

### AC-03: Approvazione esplicita richiesta

```gherkin
Given il tool "nl_query" ha proposto una query SQL
When l'utente invoca "execute_approved_query" con la query proposta
Then la query viene eseguita e i risultati restituiti
```

### AC-04: Blocco operazioni di scrittura

```gherkin
Given il server MCP NL è in esecuzione
When l'utente invoca "execute_approved_query" con SQL contenente INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE o CREATE
Then viene restituito un messaggio di errore e la query non viene eseguita
```

### AC-05: NL complesso

```gherkin
Given il server MCP NL è in esecuzione
  And il database contiene utenti con ruoli
When l'utente invoca "nl_query" con question="Mostrami gli utenti admin con la loro email"
Then il server propone una query SQL con JOIN tra users, users_roles e roles
```

### AC-06: Errore query invalida

```gherkin
Given il server MCP NL è in esecuzione
When l'utente invoca "execute_approved_query" con SQL sintatticamente errato
Then viene restituito un messaggio di errore leggibile (non uno stack trace)
```

### AC-07: Database non raggiungibile

```gherkin
Given il server MCP NL è in esecuzione
  And il database H2 non è raggiungibile
When l'utente invoca qualsiasi tool
Then viene restituito un messaggio di errore chiaro ("Database non raggiungibile")
```

### AC-08: Configurazione MCP per Kiro

```gherkin
Given il file `.kiro/settings/mcp.json` è aggiornato con il server `datathon-nl`
When Kiro viene avviato nel progetto
Then i tool del server sono riconosciuti e disponibili
```

### AC-09: Isolation strategy documentata

```gherkin
Given il file guardrails.md esiste nel progetto
When si verifica la sezione "NL Query Isolation"
Then è presente la documentazione della strategia di isolamento per UC-MCP-004
```

---

## Schema Database di Riferimento (H2)

| Tabella | Colonne principali |
|---|---|
| `users` | id, username, email, first_name, last_name, phone_number, password, created_at, updated_at |
| `roles` | id, name, created_at, updated_at |
| `users_roles` | user_id, role_id |
| `refresh_token` | id, token, user_id, expiry_date |
| `password_reset_token` | id, token, user_id, expiry_date |
| `eggup_user` | id, eggup_user_guid, username, password, assessment_url, authentication_token, score_id, eni_user_id |
| `eggup_score` | id, test_name, coverage_index, duration, date |
| `eggup_trait` | id, trait_id, trait_name, score, macro_name, macro_score, macro_weight, count, score_id |

## Struttura File Attesa

```
ericsson_challenge-dev/
├── mcp-nl/
│   ├── Dockerfile
│   ├── requirements.txt
│   └── server.py
├── docker-compose.yml              # aggiornato con servizio mcp-nl
├── .kiro/settings/mcp.json         # aggiornato con server datathon-nl
└── .kiro/steering/guardrails.md    # aggiornato con sezione NL Query Isolation
```
