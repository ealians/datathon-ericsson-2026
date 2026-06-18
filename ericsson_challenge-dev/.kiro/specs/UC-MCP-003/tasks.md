# UC-MCP-003 — Tasks

## Fase 1: Preparazione App Spring Boot

- [x] 1. Aggiungere `spring-boot-starter-actuator` e `micrometer-registry-prometheus` al `pom.xml`
- [x] 2. Aggiungere configurazione Actuator in `application-docker.properties` (exposure: health, info, metrics, prometheus; show-details=always)

## Fase 2: Implementazione Server MCP

- [x] 3. Creare directory `mcp-metrics/`
- [x] 4. Creare `mcp-metrics/requirements.txt` con `mcp[cli]==1.9.2` e `httpx==0.28.1`
- [x] 5. Creare `mcp-metrics/Dockerfile` (python:3.11-slim, install deps, copy server.py)
- [x] 6. Creare `mcp-metrics/server.py` con:
  - FastMCP server "datathon-metrics" (stdio transport)
  - Variabile env `ACTUATOR_BASE_URL` (default `http://app:8080/actuator`)
  - Tool `health`: GET /actuator/health, restituisce JSON formattato
  - Tool `list_metrics`: GET /actuator/metrics, restituisce lista nomi
  - Tool `get_metric(name)`: valida input con regex, GET /actuator/metrics/{name}
  - Tool `prometheus`: GET /actuator/prometheus, restituisce testo raw
  - Gestione errori: timeout 5s, connection error → messaggio user-friendly
  - Validazione input: regex `^[a-zA-Z0-9._-]+$` per nome metrica

## Fase 3: Integrazione Docker

- [x] 7. Aggiungere servizio `mcp-metrics` in `docker-compose.yml`
- [x] 8. Aggiungere entry `datathon-metrics` in `.kiro/settings/mcp.json`

## Fase 4: Test

- [x] 9. Rebuild containers (`docker compose build`)
- [x] 10. Verificare AC-01: tool `health` restituisce stato UP ✓
- [x] 11. Verificare AC-02: tool `list_metrics` restituisce elenco metriche (95 metriche) ✓
- [x] 12. Verificare AC-03: tool `get_metric` con "jvm.memory.used" restituisce valore ✓
- [x] 13. Verificare AC-04: tool `prometheus` restituisce testo Prometheus (384 righe) ✓
- [x] 14. Verificare AC-05: tool `get_metric` con input malevolo restituisce errore ✓
- [x] 15. Verificare AC-06: con app spenta, tools restituiscono errore chiaro ✓
