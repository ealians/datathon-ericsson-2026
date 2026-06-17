# Strategy

## Obiettivo

Evolvere l'applicazione User Profile Management per la Datathon Ericsson 2026, mantenendo la stabilità del sistema esistente e aggiungendo nuove funzionalità richieste dalla challenge.

## Principi Guida

1. **Incrementale**: ogni modifica deve essere autocontenuta e testabile indipendentemente
2. **Backward-compatible**: le API esistenti non devono rompere i client attuali
3. **Sicurezza first**: ogni nuovo endpoint deve rispettare il security baseline
4. **Documentazione**: ogni use case deve avere spec completa (requirements → design → tasks)

## Workflow di Sviluppo

1. Definire i requisiti in `.kiro/specs/<uc-id>/requirements.md`
2. Opzionale su richiesta: Progettare la soluzione in `.kiro/specs/<uc-id>/design.md`
3. Opzionale su richiesta: Scomporre in task implementativi in `.kiro/specs/<uc-id>/tasks.md`
4. Implementare seguendo le guardrails
5. Testare (unit + integration)
6. Review e merge


## Way of working
Il team predilige la generazione di file di specifica meno  prolissi


## Avvio Environment 
- Avviare sempre il progetto e i test utilizzando Docker (`docker-compose`) su WSL