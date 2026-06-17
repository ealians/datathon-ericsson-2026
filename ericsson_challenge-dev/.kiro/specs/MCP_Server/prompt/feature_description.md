Il goal dello use case è creare un server MCP locale che deve girare su docker, scritto in python , implementato in modo semplice , che deve interpretare le query in linguaggio naturale di kiro e tradurle in richieste al database postgres del progetto. 
L'utente deve essere in grado di interagire con il server mcp da kiro e ottenere le risposte direttamente nella chat.

Per security, le query devono essere solo in lettura.

Un log di tutte le operazioni eseguite verso il database e dei relativi risultati viene collezionato e salvato all'interno del container.
 
Partendo da questo obiettivo, crea uno spec file con i requisiti sotto allo use case .kiro/specs/MCP_Server
