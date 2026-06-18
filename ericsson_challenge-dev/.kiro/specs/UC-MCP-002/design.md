# UC-MCP-002 – Design Specifications

## Architettura

```
┌─────────────┐       stdio        ┌─────────────────┐       read-only      ┌────────────────┐
│   Kiro CLI  │ ◄──────────────► │  mcp-docs       │ ◄──────────────────► │  /docs (volume) │
│  (MCP client)│                   │  (Python/FastMCP)│                      │  README.md      │
└─────────────┘                    └─────────────────┘                      │  project.md     │
                                                                            └────────────────┘
```

Il server MCP viene eseguito via `docker exec` dal client Kiro, comunicando tramite stdio. Il server legge i documenti dalla directory `/docs` montata come volume read-only dal host.

## Tecnologie

| Componente | Scelta | Motivazione |
|---|---|---|
| Framework MCP | `mcp[cli]` (FastMCP) | SDK ufficiale Python, supporto stdio nativo |
| Runtime | Python 3.11-slim | Immagine leggera, nessuna dipendenza aggiuntiva necessaria |
| Transport | stdio | Richiesto dal protocollo MCP per integrazione con Kiro |
| Filesystem | Volume bind-mount read-only | Accesso sicuro e controllato ai documenti |

## Struttura server.py

```python
# Singolo file con:
# 1. Costante DOCS_DIR = "/docs"
# 2. Mappa DOCS_METADATA con descrizione per ogni documento
# 3. Helper _safe_path(name) per validazione path traversal
# 4. Tool: list_docs()
# 5. Tool: read_doc(name: str)
# 6. Tool: search_docs(query: str, context_lines: int = 3)
# 7. Entrypoint: mcp.run(transport="stdio")
```

## Design dei Tool

### list_docs()
- Scansiona la directory `/docs` per file `.md`
- Restituisce elenco formattato: nome file + descrizione (da mappa statica)
- Nessun parametro in input

### read_doc(name: str)
- Valida il parametro `name` con `_safe_path()`:
  - Risolve il percorso canonico e verifica che sia dentro `/docs`
  - Blocca `..`, path assoluti, symlink fuori dalla directory
- Legge e restituisce il contenuto integrale del file
- Se il file non esiste, restituisce errore

### search_docs(query: str, context_lines: int = 3)
- Itera su tutti i file `.md` in `/docs`
- Per ogni file, cerca righe contenenti `query` (case-insensitive)
- Per ogni match, restituisce `context_lines` righe prima e dopo
- Formatta output con nome file, numero riga e contesto
- Se nessun risultato, restituisce messaggio "Nessun risultato trovato"

## Validazione Path (_safe_path)

```python
def _safe_path(name: str) -> str:
    """Risolve e valida il percorso. Ritorna path assoluto sicuro o solleva ValueError."""
    candidate = os.path.realpath(os.path.join(DOCS_DIR, name))
    if not candidate.startswith(os.path.realpath(DOCS_DIR) + os.sep) and candidate != os.path.realpath(DOCS_DIR):
        raise ValueError("Access denied: path outside docs directory")
    if not os.path.isfile(candidate):
        raise ValueError(f"File not found: {name}")
    return candidate
```

Previene path traversal risolvendo symlink e verificando il prefisso.

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
mcp-docs:
  build:
    context: ./mcp-docs
  container_name: datathon-mcp-docs
  restart: unless-stopped
  volumes:
    - ./README.md:/docs/README.md:ro
    - ./project.md:/docs/project.md:ro
  depends_on:
    postgres:
      condition: service_healthy
  stdin_open: true
  tty: true
```

- Volume bind-mount `:ro` per ogni file di documentazione
- `stdin_open: true` per comunicazione stdio via `docker exec`
- `depends_on` postgres per garantire che l'infrastruttura sia pronta (anche se questo server non usa il DB)

## Configurazione MCP (.kiro/settings/mcp.json)

```json
{
  "mcpServers": {
    "datathon-db": {
      "command": "docker",
      "args": ["exec", "-i", "datathon-mcp-server", "python", "server.py"],
      "env": {}
    },
    "datathon-docs": {
      "command": "docker",
      "args": ["exec", "-i", "datathon-mcp-docs", "python", "server.py"],
      "env": {}
    }
  }
}
```

## Sicurezza

- **Path traversal prevention**: validazione con `os.path.realpath()` + controllo prefisso
- **Volume read-only**: il mount Docker impedisce qualsiasi scrittura nella directory `/docs`
- **Nessun accesso al filesystem host**: il container vede solo i file esplicitamente montati
- **Nessuna credenziale necessaria**: il server non accede a database o servizi esterni
- **Nessuna porta esposta**: comunicazione esclusivamente via stdio
