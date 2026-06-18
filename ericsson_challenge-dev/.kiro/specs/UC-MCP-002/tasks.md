# UC-MCP-002 – Task List

## Fase 1: Setup struttura e dipendenze
- [x] 1. Creare `mcp-docs/requirements.txt` con `mcp[cli]>=1.0.0`
- [x] 2. Creare `mcp-docs/Dockerfile` (Python 3.11-slim, install dipendenze, entrypoint server.py)

## Fase 2: Implementazione server MCP
- [x] 3. Creare `mcp-docs/server.py` con costanti e helper `_safe_path()`
- [x] 4. Implementare tool `list_docs` (scansione /docs, restituzione elenco con descrizioni)
- [x] 5. Implementare tool `read_doc` (lettura file con validazione path traversal)
- [x] 6. Implementare tool `search_docs` (ricerca keyword con contesto righe)

## Fase 3: Integrazione Docker
- [x] 7. Aggiornare `docker-compose.yml` con servizio `mcp-docs` (build, volumes ro, read_only, stdin_open)

## Fase 4: Configurazione MCP
- [x] 8. Aggiornare `.kiro/settings/mcp.json` con server `datathon-docs`

## Fase 5: Test
- [x] 9. Verificare avvio container: `docker compose up --build` → container running (AC-1) ✓
- [x] 10. Verificare `list_docs` restituisce README.md e project.md (AC-2) ✓
- [x] 11. Verificare `read_doc` con "README.md" restituisce contenuto corretto (AC-3) ✓
- [x] 12. Verificare `read_doc` blocca path traversal (`../../etc/passwd`) (AC-4) ✓
- [x] 13. Verificare `search_docs` con "Spring Boot" restituisce risultati pertinenti (AC-5) ✓
- [x] 14. Verificare `search_docs` con termine inesistente restituisce messaggio vuoto (AC-6) ✓
- [x] 15. Verificare tool disponibili in Kiro da mcp.json (AC-7) ✓
- [x] 16. Verificare volume read-only: tentativo di scrittura in /docs fallisce (AC-8) ✓
