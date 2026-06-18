# UC-MCP-004 — Tasks

## Fase 1: Preparazione Infrastruttura

- [ ] 1. Scaricare `h2-2.2.224.jar` nella directory `mcp-nl/` (necessario per jaydebeapi JDBC)
- [ ] 2. Creare `mcp-nl/requirements.txt` con `mcp[cli]==1.9.2` e `jaydebeapi==1.2.3`
- [ ] 3. Creare `mcp-nl/Dockerfile` (python:3.11-slim + JRE headless, install deps, copy h2.jar e server.py)

## Fase 2: Implementazione Server MCP

- [ ] 4. Creare `mcp-nl/server.py` con:
  - FastMCP server "datathon-nl" (stdio transport)
  - Configurazione connessione H2 da env vars (`H2_JDBC_URL`, `H2_USER`, `H2_PASSWORD`, `H2_JAR_PATH`)
  - `FORBIDDEN_PATTERN` regex per bloccare scritture
  - Helper `_get_connection()` via jaydebeapi
  - Helper `_get_schema_text()` per ottenere schema formattato
  - Tool `get_schema()`: restituisce schema DB completo
  - Tool `nl_query(question: str)`: traduce NL in SQL proposta usando pattern matching + schema context
  - Tool `execute_approved_query(sql: str)`: valida SQL (solo SELECT), esegue con timeout 5s, restituisce risultati
  - Gestione errori: connessione fallita, query invalida, timeout

## Fase 3: Integrazione Docker

- [ ] 5. Aggiungere servizio `mcp-nl` in `docker-compose.yml` (read_only, volume ./data:ro, tmpfs /tmp, env vars)
- [ ] 6. Aggiornare `.kiro/settings/mcp.json` con entry `datathon-nl`

## Fase 4: Documentazione Isolation

- [ ] 7. Aggiornare `.kiro/steering/guardrails.md` con sezione "NL Query Isolation" che documenta la strategia di isolamento

## Fase 5: Test e Verifica

- [ ] 8. Rebuild containers (`docker compose build mcp-nl`)
- [ ] 9. Verificare AC-01: `get_schema` restituisce tabelle e colonne
- [ ] 10. Verificare AC-02: `nl_query` con "Quanti utenti ci sono?" → propone SQL senza eseguire
- [ ] 11. Verificare AC-03: `execute_approved_query` con SQL approvata → risultati
- [ ] 12. Verificare AC-04: `execute_approved_query` con INSERT → errore
- [ ] 13. Verificare AC-05: `nl_query` con domanda complessa → SQL con JOIN
- [ ] 14. Verificare AC-06: `execute_approved_query` con SQL invalida → errore leggibile
- [ ] 15. Verificare AC-07: con DB non raggiungibile → messaggio chiaro
- [ ] 16. Verificare AC-08: tool disponibili in Kiro da mcp.json
- [ ] 17. Verificare AC-09: sezione isolation documentata in guardrails.md
