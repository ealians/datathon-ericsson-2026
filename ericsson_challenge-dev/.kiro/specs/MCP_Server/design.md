# MCP Server - Design Specifications

## Architettura

```
┌─────────────┐       stdio        ┌─────────────────┐       TCP/5432       ┌────────────┐
│   Kiro CLI  │ ◄──────────────► │  mcp-server     │ ◄──────────────────► │  PostgreSQL │
│  (MCP client)│                   │  (Python/FastMCP)│                      │  (container)│
└─────────────┘                    └─────────────────┘                      └────────────┘
```

Il server MCP viene eseguito via `docker exec` dal client Kiro, comunicando tramite stdio (stdin/stdout). Il server si connette al database PostgreSQL sulla rete interna Docker.

## Tecnologie

| Componente | Scelta | Motivazione |
|---|---|---|
| Framework MCP | `mcp[cli]` (FastMCP) | SDK ufficiale Python, supporto stdio nativo |
| Driver DB | `psycopg2-binary` | Driver PostgreSQL standard, zero dipendenze di compilazione |
| Runtime | Python 3.11-slim | Immagine leggera, compatibile con le dipendenze |
| Transport | stdio | Richiesto dal protocollo MCP per integrazione con Kiro |

## Struttura server.py

```python
# Singolo file con:
# 1. Configurazione connessione DB (da env vars)
# 2. Helper _get_connection()
# 3. Regex FORBIDDEN_PATTERN per bloccare scritture
# 4. Tool: get_schema()
# 5. Tool: query_database(sql: str)
# 6. Tool: seed_database()
# 7. Tool: clean_database()
# 8. Entrypoint: mcp.run(transport="stdio")
```

## Design dei Tool

### get_schema()
- Interroga `information_schema.columns` filtrando `table_schema = 'public'`
- Restituisce output testuale formattato: tabella → lista colonne con tipo e nullabilità
- Nessun parametro in input

### query_database(sql: str)
- Valida l'input con regex: se contiene keyword di scrittura (INSERT, UPDATE, DELETE, DROP, ALTER, TRUNCATE, CREATE, GRANT, REVOKE), rifiuta con messaggio di errore
- Esegue la query e restituisce risultati formattati (header + righe)
- Limita output a 100 righe per evitare risposte troppo grandi

### seed_database()
- Inserisce dati di test predefiniti con `INSERT ... ON CONFLICT DO NOTHING` per idempotenza
- Ordine di inserimento rispetta le foreign key:
  1. `roles` (se non esistono già oltre ai base)
  2. `users` (utenti di test)
  3. `users_roles` (associazioni)
  4. `eggup_score` (punteggi)
  5. `eggup_user` (info EggUp, referenzia users e score)
  6. `eggup_trait` (tratti, referenzia score)
- Usa transaction unica con COMMIT alla fine
- Restituisce conteggio record inseriti per tabella

### clean_database()
- Elimina dati in ordine inverso rispetto alle FK:
  1. `eggup_trait`
  2. `eggup_user`
  3. `eggup_score`
  4. `users_roles` (WHERE user_id non è admin)
  5. `users` (WHERE email != 'admin@elis.org')
- Non tocca: `roles`, utente admin, `refresh_token`, `password_reset_token`
- Usa transaction unica con COMMIT alla fine
- Restituisce conteggio record eliminati per tabella

## Dockerfile

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY server.py .
CMD ["python", "server.py"]
```

Immagine minimale, nessun build stage necessario.

## Integrazione docker-compose.yml

```yaml
mcp-server:
  build:
    context: ./mcp-server
  container_name: datathon-mcp-server
  environment:
    DB_HOST: postgres
    DB_PORT: 5432
    DB_NAME: ${DB_NAME:-datathon_db}
    DB_USERNAME: ${DB_USERNAME:-datathon_user}
    DB_PASSWORD: ${DB_PASSWORD:-datathon_pass}
  depends_on:
    postgres:
      condition: service_healthy
  stdin_open: true
```

Il container resta attivo con `stdin_open: true` per permettere la comunicazione stdio via `docker exec`.

## Configurazione MCP (.kiro/settings/mcp.json)

```json
{
  "mcpServers": {
    "datathon-db": {
      "command": "docker",
      "args": ["exec", "-i", "datathon-mcp-server", "python", "server.py"],
      "env": {}
    }
  }
}
```

Kiro avvia il server MCP eseguendo `python server.py` dentro il container già in esecuzione.

## Sicurezza

- **Validazione input**: regex che blocca qualsiasi keyword SQL di scrittura prima dell'esecuzione
- **Operazioni controllate**: seed e clean usano statement SQL hardcoded, non accettano SQL dall'utente
- **Credenziali**: passate esclusivamente via environment variables Docker
- **Rete**: il server MCP non espone porte esterne, comunica solo sulla rete Docker interna verso postgres
