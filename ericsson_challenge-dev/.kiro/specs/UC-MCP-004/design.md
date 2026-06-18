# UC-MCP-004 — Design Document

## Architettura

```
┌─────────────┐       stdio        ┌──────────────────┐      JDBC/TCP       ┌──────────────────┐
│    Kiro      │◄──────────────────►│  mcp-nl          │────────────────────►│  Spring Boot App │
│  (IDE/CLI)   │   JSON-RPC/stdio   │  (Python 3.11)   │  jdbc:h2:tcp://    │  H2 DB (TCP mode)│
└─────────────┘                     └──────────────────┘                     └──────────────────┘
                                           Docker container                       Docker container
```

Il server MCP `mcp-nl` è un container Docker che:
1. Riceve domande NL via stdin (JSON-RPC, MCP protocol)
2. Traduce NL → SQL usando lo schema DB come contesto e pattern matching
3. Propone la query all'utente senza eseguirla
4. Su approvazione, esegue la query via JDBC verso H2 TCP e restituisce i risultati

## Tecnologie

| Componente | Scelta | Motivazione |
|---|---|---|
| Framework MCP | `mcp[cli]` (FastMCP) | SDK ufficiale Python, supporto stdio nativo |
| Driver DB | `jaydebeapi` + H2 JDBC jar | Unico modo per connettere Python a H2 via TCP |
| Java Runtime | JRE incluso nel container | Necessario per jaydebeapi (usa JNI/JVM per JDBC) |
| Runtime | Python 3.11-slim + JRE | Supporto jaydebeapi |
| Transport | stdio | Richiesto dal protocollo MCP per integrazione con Kiro |

## Approccio NL → SQL

Il server usa un approccio basato su template e pattern matching per tradurre le domande NL in SQL:

1. Il tool `get_schema` fornisce il contesto dello schema al client MCP (Kiro)
2. Il tool `nl_query` riceve la domanda NL e genera SQL attraverso:
   - Mappatura keyword → tabelle/colonne (es. "utenti" → `users`, "ruoli" → `roles`)
   - Pattern comuni (es. "quanti" → `COUNT(*)`, "mostrami" → `SELECT`)
   - JOIN detection basato sulle relazioni FK note
3. Il client MCP (Kiro LLM) può anche generare direttamente la SQL e usare `execute_approved_query`

**Nota**: In contesto datathon, il client LLM (Kiro) è il vero motore di traduzione NL→SQL. Il tool `nl_query` fornisce lo schema come contesto e può fare una traduzione baseline, ma il flusso principale è: Kiro legge schema → genera SQL → propone all'utente → esegue via `execute_approved_query`.

## Tool Design

| Tool | Descrizione | Parametri | Ritorna |
|------|-------------|-----------|---------|
| `get_schema` | Schema completo del DB H2 | nessuno | Tabelle, colonne, tipi |
| `nl_query` | Traduce NL in SQL proposta | `question: str` | SQL proposta (non eseguita) |
| `execute_approved_query` | Esegue SQL approvata | `sql: str` | Risultati formattati |

## Sicurezza & Isolation

| Misura | Implementazione |
|--------|-----------------|
| Read-only enforcement | Regex blocca INSERT/UPDATE/DELETE/DROP/ALTER/TRUNCATE/CREATE |
| Two-step approval | `nl_query` propone, `execute_approved_query` esegue — tool separati |
| Input validation | SQL validata con regex prima dell'esecuzione |
| Container read-only | `read_only: true` in docker-compose |
| Timeout | Query timeout 5 secondi |
| Scope limitato | Solo accesso a H2 dev, non PostgreSQL prod |
| No secrets in code | Credenziali via env var |

## Configurazione H2 TCP Mode

L'app Spring Boot deve esporre H2 in modalità server TCP per permettere connessioni esterne. Aggiunta in `application.properties`:

```properties
spring.h2.console.settings.web-allow-others=true
```

E configurazione programmatica per TCP server (già inclusa se si usa `DB_CLOSE_ON_EXIT=FALSE` con file-based H2). In alternativa, la connessione JDBC può puntare al file H2 montato come volume condiviso.

**Approccio scelto**: Volume condiviso del file H2 — il container mcp-nl monta lo stesso path `./data/` dell'app e si connette con `jdbc:h2:file:/data/datathon_user_db;ACCESS_MODE_DATA=r`.

## Docker Integration

### Dockerfile (`mcp-nl/Dockerfile`)

```dockerfile
FROM python:3.11-slim
RUN apt-get update && apt-get install -y --no-install-recommends default-jre-headless && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY h2-2.2.224.jar /app/h2.jar
COPY server.py .
CMD ["python", "server.py"]
```

### docker-compose.yml (servizio aggiuntivo)

```yaml
mcp-nl:
  build:
    context: ./mcp-nl
  container_name: datathon-mcp-nl
  restart: unless-stopped
  read_only: true
  environment:
    H2_JDBC_URL: jdbc:h2:file:/data/datathon_user_db;ACCESS_MODE_DATA=r;DB_CLOSE_ON_EXIT=FALSE
    H2_USER: ${DB_USERNAME:-admin}
    H2_PASSWORD: ${DB_PASSWORD:-dev_password}
    H2_JAR_PATH: /app/h2.jar
  volumes:
    - ./data:/data:ro
  tmpfs:
    - /tmp
  stdin_open: true
  tty: true
```

### MCP Config (`.kiro/settings/mcp.json`)

```json
{
  "datathon-nl": {
    "command": "docker",
    "args": ["exec", "-i", "datathon-mcp-nl", "python", "server.py"],
    "env": {}
  }
}
```

## Struttura server.py

```python
# 1. Costanti e config da env vars
# 2. FORBIDDEN_PATTERN regex
# 3. NL_PATTERNS per traduzione base NL→SQL
# 4. Helper _get_connection() via jaydebeapi
# 5. Helper _get_schema_text() → schema formattato
# 6. Tool: get_schema()
# 7. Tool: nl_query(question: str) → SQL proposta
# 8. Tool: execute_approved_query(sql: str) → risultati
# 9. Entrypoint: mcp.run(transport="stdio")
```

## Dipendenze Python

```
mcp[cli]==1.9.2
jaydebeapi==1.2.3
```

## Struttura File

```
mcp-nl/
├── Dockerfile
├── requirements.txt
├── h2-2.2.224.jar
└── server.py
```
