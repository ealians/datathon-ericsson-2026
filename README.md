# Datathon Ericsson 2026

**Academy Generative AI - Ericsson | ELIS Innovation Hub**

Repository ufficiale del Datathon Ericsson 2026: progetto base, materiali per i team, documentazione di design e use case su cui i partecipanti lavoreranno con **Amazon Q CLI** e **Amazon Q IDE Plugin**.

---

## Cos'è il Datathon

Una sfida pratica in cui squadre miste si affrontano su una codebase reale (un sistema di gestione profili utente Spring Boot) con l'obiettivo di **risolvere bug, chiudere vulnerabilità di sicurezza, aggiungere funzionalità e introdurre testing/DevOps** usando strumenti di GenAI assistita.

| Parametro | Valore |
|---|---|
| Format | 2 sessioni (identiche) × 3 giornate + onboarding |
| Partecipanti | 30 per sessione (60 totali) |
| Tavoli di lavoro | 5 per sessione, 6 partecipanti ciascuno |
| Ore di lavoro effettivo | ~18h (DAY1 ~6h, DAY2 ~7h, DAY3 ~5h) |
| Strumenti | Amazon Q CLI + Amazon Q IDE Plugin |

---

## Struttura della Repository

```
datathon/
├── README.md                                  # Questo file
├── DATATHON_ERICSSON_DESIGN.md                # Design document completo (architettura, gap analysis, UC, rubrica)
├── Datathon_Ericsson_2026_Onboarding_Team.docx
├── Datathon_Ericsson_2026_Guida_UC_Pilota.docx
│
├── ericsson_challenge-dev/                    # Codebase di partenza (Spring Boot 3.3.5)
│   ├── README.md                              # Quick start del progetto base
│   ├── pom.xml
│   ├── docker-compose.yml
│   └── src/
│
├── analisi/                                   # Versioni del progetto base e UC scoring
│   └── Datathon_Ericsson_2026_*.docx
│
└── docs/                                      # Materiali di presentazione
    ├── Academy GenAI_Datathon Ericsson_v6.pptx
    ├── Datathon_Ericsson_2026_v3.pdf
    ├── Datathon_Ericsson_Base_Project_2026_v3.pdf
    ├── Linee Guida.pdf
    └── Onboarding TL .pdf
```

---

## Il Progetto Base

La codebase di partenza è in `ericsson_challenge-dev/`: un'applicazione Spring Boot che implementa un **User Profile Management System** con autenticazione JWT, ruoli ADMIN/USER e UI Thymeleaf/Bootstrap.

| Componente | Tecnologia |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security 6 + JWT (jjwt 0.11.5) |
| ORM | Spring Data JPA / Hibernate |
| Database | H2 (dev) / PostgreSQL (Docker) |
| Template Engine | Thymeleaf 3.1.0 + Bootstrap 5.3.3 |
| Build | Maven |

Per istruzioni di avvio, configurazione, endpoint API e troubleshooting → vedi [`ericsson_challenge-dev/README.md`](./ericsson_challenge-dev/README.md).

```bash
cd ericsson_challenge-dev
docker compose up --build       # http://localhost:8080/login
```

Credenziali di default: `admin@elis.org` / `password`.

---

## Use Case del Datathon

Il design document elenca **45 Use Case** divisi per categoria:

| Categoria | UC | % |
|---|---|---|
| BUGFIX | 7 | 16% |
| SECURITY | 10 | 22% |
| FEATURE | 12 | 27% |
| TESTING | 9 | 20% |
| DEVOPS | 7 | 16% |

Distribuiti per difficoltà: 14 EASY, 18 MEDIUM, 9 HARD, 4 EXPERT.

Dettaglio completo (titolo, ore stimate, dipendenze, tag CLI/IDE) → sezione 5 di [`DATATHON_ERICSSON_DESIGN.md`](./DATATHON_ERICSSON_DESIGN.md).

---

## Per i Team Partecipanti

1. **Onboarding** → `Datathon_Ericsson_2026_Onboarding_Team.docx` e `docs/Onboarding TL .pdf`
2. **UC Pilota** → `Datathon_Ericsson_2026_Guida_UC_Pilota.docx` (suggerito come primo UC per familiarizzare con gli strumenti)
3. **Linee guida valutazione** → `docs/Linee Guida.pdf` e sezione 7 del design document (rubrica)
4. **Codebase** → fork/clone di `ericsson_challenge-dev/`, lavorare su branch dedicati per UC

---

## Per Tutor / Lead

- **Architettura target e gap analysis** → sezioni 2–4 di `DATATHON_ERICSSON_DESIGN.md`
- **Organizzazione squadre** → sezione 6 del design document
- **Rubrica di valutazione** → sezione 7 del design document
- **Materiali presentazione kick-off** → `docs/Academy GenAI_Datathon Ericsson_v6.pptx`

---

## Contatti

ELIS Innovation Hub - Academy GenAI
Repository: <https://gitlab.elis.org/eih/ericson-datathon-2026/datathon>
