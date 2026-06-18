Il goal dello use case è creare un server MCP locale che deve girare su docker, scritto in python , implementato in modo semplice , che deve permettere queries in NL su DB interno. Accettabile in contesto datathon H2 — richiede isolation strategy documentata in guardrails.md e approvazione esplicita per ogni query.
L'utente deve essere in grado di interagire con il server mcp da kiro e ottenere le risposte direttamente nella chat.
L'accesso alla documentazione è read-only

 
Partendo da questo obiettivo, crea uno spec file con i requisiti sotto allo use case .kiro/specs/UC-MCP-004
