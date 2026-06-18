# UC-MCP-002 – MCP Server: Documentazione Interna come Risorsa

## Obiettivo

Server MCP (Model Context Protocol) locale in Python che gira su Docker, che espone la documentazione interna del progetto come risorsa consultabile in sola lettura. L'utente interagisce con Kiro per ottenere informazioni sulla documentazione direttamente nella chat.

## Requisiti Funzionali

### RF-1: Server MCP Python (stdio)
- Il server deve implementare il protocollo MCP (stdio transport) in Python.
- Deve esporre **risorse** (resources) e/o **tool** per consultare la documentazione interna del progetto.
- Implementazione semplice e minimale (singolo file Python).

### RF-2: Documentazione esposta
- Il server deve rendere disponibili come risorse MCP i seguenti file di documentazione:
  - `README.md` – guida principale del progetto
  - `project.md` – analisi architetturale e struttura completa
- I file sono montati nel container Docker come volume read-only.

### RF-3: Tool Esposti

- `list_docs`: restituisce l'elenco dei documenti disponibili con nome, percorso e breve descrizione.
- `read_doc`: accetta il nome/percorso di un documento e restituisce il suo contenuto integrale.
- `search_docs`: accetta una parola chiave o frase e restituisce le sezioni dei documenti che contengono il termine cercato, con contesto (righe prima/dopo il match).

### RF-4: Accesso Read-Only
- Tutti i tool sono in sola lettura: nessuna operazione di scrittura, modifica o cancellazione è permessa.
- Il filesystem nel container è montato in read-only per la directory dei documenti.

### RF-5: Integrazione Docker
- Il server MCP deve girare in un container Docker dedicato (separato dal server MCP esistente per il database).
- Deve essere aggiunto come servizio nel `docker-compose.yml` esistente.
- I documenti del progetto devono essere montati via volume bind-mount read-only.

### RF-6: Configurazione MCP per Kiro
- Il server deve essere registrato in `.kiro/settings/mcp.json` affinché Kiro possa riconoscerlo e utilizzarne i tool.

## Requisiti Non Funzionali

### RNF-1: Sicurezza
- Accesso esclusivamente in lettura ai file di documentazione.
- Nessun accesso al filesystem del container al di fuori della directory montata (`/docs`).
- Il tool `read_doc` deve validare il percorso richiesto (path traversal prevention): accetta solo file all'interno di `/docs`.
- Nessuna credenziale o segreto deve essere esposta.

### RNF-2: Semplicità
- Dipendenze minime: `mcp[cli]`.
- Un singolo file Python per il server.
- Nessun database necessario.

### RNF-3: Performance
- I documenti vengono letti dal filesystem ad ogni richiesta (no caching necessario dato il volume ridotto).

## Acceptance Criteria

### AC-1: Container avviabile
- **Given** il docker-compose è avviato con `docker compose up --build`
- **When** il servizio `mcp-docs` parte
- **Then** il container è in stato running e il server MCP è pronto ad accettare comandi via stdio

### AC-2: list_docs restituisce l'elenco documenti
- **Given** il server MCP docs è in esecuzione
- **When** viene invocato il tool `list_docs`
- **Then** viene restituito un elenco contenente almeno `README.md` e `project.md` con le rispettive descrizioni

### AC-3: read_doc restituisce il contenuto di un documento
- **Given** il server MCP docs è in esecuzione
- **When** viene invocato `read_doc` con argomento `README.md`
- **Then** viene restituito il contenuto integrale del file README.md del progetto

### AC-4: read_doc blocca path traversal
- **Given** il server MCP docs è in esecuzione
- **When** viene invocato `read_doc` con un percorso che tenta path traversal (es. `../../etc/passwd`, `../docker-compose.yml`)
- **Then** viene restituito un messaggio di errore e il file non viene letto

### AC-5: search_docs trova contenuto pertinente
- **Given** il server MCP docs è in esecuzione e i documenti sono montati
- **When** viene invocato `search_docs` con termine `"Spring Boot"`
- **Then** vengono restituite le sezioni dei documenti contenenti "Spring Boot" con contesto circostante

### AC-6: search_docs gestisce nessun risultato
- **Given** il server MCP docs è in esecuzione
- **When** viene invocato `search_docs` con un termine che non compare in nessun documento (es. `"xyznonexistent123"`)
- **Then** viene restituito un messaggio indicante che nessun risultato è stato trovato

### AC-7: Configurazione MCP per Kiro
- **Given** il file `.kiro/settings/mcp.json` è aggiornato con il server `mcp-docs`
- **When** Kiro viene avviato nel progetto
- **Then** i tool del server `mcp-docs` sono riconosciuti e disponibili per l'uso

### AC-8: Volume read-only
- **Given** il container `mcp-docs` è in esecuzione
- **When** si tenta di scrivere un file nella directory `/docs` del container
- **Then** l'operazione fallisce (filesystem read-only)

## Documentazione di Riferimento

| File | Descrizione |
|---|---|
| `README.md` | Guida utente: quick start, credenziali, configurazione, API, troubleshooting |
| `project.md` | Analisi tecnica: architettura, stack, entità JPA, sicurezza, deployment |

## Struttura File Attesa

```
ericsson_challenge-dev/
├── mcp-docs/
│   ├── Dockerfile
│   ├── requirements.txt
│   └── server.py
├── docker-compose.yml          # aggiornato con servizio mcp-docs
└── .kiro/settings/mcp.json     # aggiornato con server datathon-docs
```

## Esempio di Configurazione MCP

```json
{
  "mcpServers": {
    "datathon-db": { ... },
    "datathon-docs": {
      "command": "docker",
      "args": ["exec", "-i", "datathon-mcp-docs", "python", "server.py"],
      "env": {}
    }
  }
}
```
