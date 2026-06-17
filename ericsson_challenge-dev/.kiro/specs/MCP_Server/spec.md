# MCP Server - Specifica Requisiti

## Obiettivo

Server MCP (Model Context Protocol) locale in Python che gira su Docker, capace di interpretare query in linguaggio naturale e tradurle in interrogazioni SQL verso il database PostgreSQL del progetto.

## Requisiti Funzionali

### RF-1: Server MCP Python
- Il server deve implementare il protocollo MCP (stdio transport) in Python.
- Deve esporre tool per interrogare il database PostgreSQL del progetto.
- Implementazione semplice e minimale.

### RF-2: Tool Esposti - Query
- `get_schema`: restituisce lo schema delle tabelle del database (nomi tabelle, colonne, tipi) per permettere al client MCP di costruire query corrette.
- `query_database`: accetta una query SQL SELECT e la esegue sul database, restituendo i risultati.

### RF-3: Popolamento e Pulizia Database (Test)
- `seed_database`: popola il database con dati di test realistici (utenti, ruoli, associazioni, dati EggUp). Utile per verificare il funzionamento dell'applicazione senza dati reali.
- `clean_database`: rimuove tutti i dati di test dal database, riportandolo allo stato iniziale (mantenendo solo l'utente admin di default e i ruoli base).
- Entrambi i tool devono operare in modo idempotente: chiamate ripetute non devono causare errori o duplicati.
- I dati di seed devono coprire tutte le tabelle principali: `users`, `roles`, `users_roles`, `eggup_user`, `eggup_score`, `eggup_trait`.

### RF-4: Integrazione Docker
- Il server MCP deve girare in un container Docker dedicato.
- Deve connettersi al database PostgreSQL già definito nel `docker-compose.yml` del progetto.
- Deve essere aggiunto come servizio nel `docker-compose.yml` esistente.

### RF-5: Configurazione
- Credenziali database passate tramite variabili d'ambiente (riutilizzando `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`).
- Il server deve essere configurabile in `.kiro/settings/mcp.json`.

## Requisiti Non Funzionali

### RNF-1: Sicurezza
- Il tool `query_database` accetta solo query SELECT (bloccare INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE, CREATE).
- I tool `seed_database` e `clean_database` sono le uniche eccezioni alla regola read-only: eseguono operazioni di scrittura predefinite e controllate (non accettano SQL arbitrario dall'utente).
- Credenziali database via environment variables, mai hardcoded.

### RNF-2: Semplicità
- Dipendenze minime: `mcp`, `psycopg2-binary`.
- Un singolo file Python per il server.

## Acceptance Criteria

### AC-1: Server MCP avviabile
- **Given** il docker-compose è avviato con `docker compose up --build`
- **When** il servizio `mcp-server` parte
- **Then** il container è in stato running e il server MCP è pronto ad accettare comandi via stdio

### AC-2: get_schema restituisce lo schema
- **Given** il server MCP è in esecuzione e il database PostgreSQL è attivo con le tabelle create dall'app Spring Boot
- **When** viene invocato il tool `get_schema`
- **Then** viene restituito l'elenco completo delle tabelle con colonne e tipi di dato

### AC-3: query_database esegue SELECT
- **Given** il database contiene dati (almeno l'utente admin di default)
- **When** viene invocato `query_database` con `SELECT * FROM users`
- **Then** viene restituito almeno un record con i campi dell'utente admin

### AC-4: query_database blocca operazioni di scrittura
- **Given** il server MCP è in esecuzione
- **When** viene invocato `query_database` con una query contenente INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE o CREATE
- **Then** viene restituito un messaggio di errore e la query non viene eseguita

### AC-5: seed_database popola il database
- **Given** il database è nello stato iniziale (solo admin e ruoli base)
- **When** viene invocato il tool `seed_database`
- **Then** vengono inseriti dati di test in tutte le tabelle principali (`users`, `roles`, `users_roles`, `eggup_user`, `eggup_score`, `eggup_trait`) e viene restituito un messaggio di conferma

### AC-6: seed_database è idempotente
- **Given** `seed_database` è già stato invocato con successo
- **When** viene invocato nuovamente `seed_database`
- **Then** non vengono generati errori né duplicati

### AC-7: clean_database ripristina lo stato iniziale
- **Given** il database contiene dati di test inseriti da `seed_database`
- **When** viene invocato il tool `clean_database`
- **Then** tutti i dati di test vengono rimossi, mantenendo solo l'utente admin (`admin@elis.org`) e i ruoli base (`ROLE_ADMIN`, `ROLE_USER`)

### AC-8: Configurazione MCP per Kiro
- **Given** il file `.kiro/settings/mcp.json` è presente e configurato
- **When** Kiro viene avviato nel progetto
- **Then** il server MCP è riconosciuto e i suoi tool sono disponibili per l'uso

### AC-9: Connessione database via variabili d'ambiente
- **Given** le variabili `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` sono definite nel docker-compose
- **When** il server MCP tenta di connettersi al database
- **Then** la connessione avviene con successo utilizzando le credenziali dalle variabili d'ambiente


## Schema Database di Riferimento

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
├── mcp-server/
│   ├── Dockerfile
│   ├── requirements.txt
│   └── server.py
├── docker-compose.yml          # aggiornato con servizio mcp-server
└── .kiro/settings/mcp.json     # configurazione MCP per Kiro
```
