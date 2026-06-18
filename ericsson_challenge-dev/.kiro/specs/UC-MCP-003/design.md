# UC-MCP-003 — Design Document

## Architettura

```
┌─────────────┐       stdio        ┌──────────────────┐      HTTP GET       ┌──────────────────┐
│    Kiro      │◄──────────────────►│  mcp-metrics     │────────────────────►│  Spring Boot App │
│  (IDE/CLI)   │   JSON-RPC/stdio   │  (Python 3.11)   │  http://app:8080   │  /actuator/*     │
└─────────────┘                     └──────────────────┘                     └──────────────────┘
                                           Docker container                       Docker container
```

Il server MCP `mcp-metrics` è un container Docker che:
1. Riceve comandi via stdin (JSON-RPC, MCP protocol)
2. Effettua richieste HTTP GET agli endpoint Actuator dell'app Spring Boot
3. Restituisce i risultati formattati via stdout

## Tool Design

| Tool | Descrizione | Parametri | Endpoint Actuator |
|------|-------------|-----------|-------------------|
| `health` | Stato di salute dell'app | nessuno | `GET /actuator/health` |
| `list_metrics` | Elenco metriche disponibili | nessuno | `GET /actuator/metrics` |
| `get_metric` | Dettaglio di una metrica | `name: str` | `GET /actuator/metrics/{name}` |
| `prometheus` | Metriche formato Prometheus | nessuno | `GET /actuator/prometheus` |

## Sicurezza

| Misura | Implementazione |
|--------|-----------------|
| Read-only | Solo richieste HTTP GET, nessuna mutazione |
| Input validation | Il parametro `name` di `get_metric` è validato con regex `^[a-zA-Z0-9._-]+$` |
| Path traversal prevention | Rifiuto di nomi contenenti `/`, `..`, o caratteri speciali |
| Container read-only | `read_only: true` in docker-compose |
| Timeout | Timeout di 5 secondi su tutte le richieste HTTP |
| No secrets in code | URL base via env var `ACTUATOR_BASE_URL` |

## Modifiche all'App Spring Boot

Per abilitare Actuator e Prometheus:

1. **pom.xml** — aggiungere:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   <dependency>
       <groupId>io.micrometer</groupId>
       <artifactId>micrometer-registry-prometheus</artifactId>
   </dependency>
   ```

2. **application-docker.properties** — aggiungere:
   ```properties
   # Actuator
   management.endpoints.web.exposure.include=health,info,metrics,prometheus
   management.endpoint.health.show-details=always
   ```

## Docker Integration

### Container `mcp-metrics`

```yaml
mcp-metrics:
  build:
    context: ./mcp-metrics
  container_name: datathon-mcp-metrics
  restart: unless-stopped
  read_only: true
  environment:
    ACTUATOR_BASE_URL: http://app:8080/actuator
  depends_on:
    app:
      condition: service_started
  stdin_open: true
  tty: true
  tmpfs:
    - /tmp
```

### MCP Config (`.kiro/settings/mcp.json`)

```json
{
  "datathon-metrics": {
    "command": "docker",
    "args": ["exec", "-i", "datathon-mcp-metrics", "python", "server.py"],
    "env": {}
  }
}
```

## Dipendenze Python

```
mcp[cli]==1.9.2
httpx==0.28.1
```

- `mcp[cli]` — framework FastMCP per server stdio
- `httpx` — client HTTP async/sync leggero per chiamare Actuator

## Struttura File

```
mcp-metrics/
├── Dockerfile
├── requirements.txt
└── server.py
```
