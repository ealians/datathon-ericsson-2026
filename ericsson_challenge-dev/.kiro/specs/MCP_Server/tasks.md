# MCP Server - Task List

## Fase 1: Setup struttura e dipendenze
- [x] 1. Creare `mcp-server/requirements.txt` con `mcp[cli]` e `psycopg2-binary`
- [x] 2. Creare `mcp-server/Dockerfile` (Python slim, install dipendenze, entrypoint server.py)

## Fase 2: Implementazione server MCP
- [x] 3. Creare `mcp-server/server.py` con connessione PostgreSQL via env vars
- [x] 4. Implementare tool `get_schema` (query information_schema.columns)
- [x] 5. Implementare tool `query_database` (esecuzione SELECT + validazione anti-scrittura)
- [x] 6. Implementare tool `seed_database` (INSERT idempotenti su tutte le tabelle principali)
- [x] 7. Implementare tool `clean_database` (DELETE dati test, preserva admin e ruoli base)

## Fase 3: Integrazione Docker
- [x] 8. Aggiornare `docker-compose.yml` con servizio `mcp-server` (depends_on postgres, env vars DB)

## Fase 4: Configurazione MCP
- [x] 9. Creare `.kiro/settings/mcp.json` con configurazione server MCP via docker exec + stdio

## Fase 5: Test
- [x] 10. Verificare avvio container: `docker compose up --build` (AC-1, AC-9) ✓
- [x] 11. Verificare `get_schema` restituisce tutte le tabelle (AC-2) ✓
- [x] 12. Verificare `query_database` con SELECT restituisce risultati (AC-3) ✓
- [x] 13. Verificare `query_database` blocca query di scrittura (AC-4) ✓
- [x] 14. Verificare `seed_database` popola tutte le tabelle (AC-5) ✓
- [x] 15. Verificare `seed_database` è idempotente (AC-6) ✓
- [x] 16. Verificare `clean_database` ripristina stato iniziale (AC-7) ✓
- [x] 17. Verificare tool MCP disponibili in Kiro (AC-8) ✓
