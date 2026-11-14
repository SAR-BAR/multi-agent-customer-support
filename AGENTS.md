# AGENTS.md

## Project Overview & Commands

**Goal:** Multi-agent customer support system for an online shop. AI agents understand, investigate, propose and challenge; deterministic Java does all lookups, maths, decisions, and is the only code that writes to the database or moves money.

**Tech stack:**
- Java 26, Maven
- Spring Boot 4.x (target), Spring Security OAuth2, Spring Data JPA
- LangChain4j + Ollama (`qwen3:8b`) for all agents
- MySQL (Flyway migrations), Testcontainers for integration tests
- Plain HTML + JS for the UI (no front-end framework)

**Build commands:**
```sh
mvn compile                    # compile
mvn test                       # unit tests
mvn verify                     # full test suite (unit + integration)
mvn spring-boot:run            # run the app
mvn spotless:apply             # format code
docker compose up -d           # start MySQL + Ollama
docker compose down            # stop services
```

**Package root:** `com.sarbar.support`

**Naming conventions:**
- `*Agent` = Spring wrapper class
- `*Assistant` = LangChain4j `AiServices` interface

---

## Context & Documentation

Before planning or writing code, **always read these files first:**

1. **`docs/context.md`** — Domain glossary (Conversation, Case, Problem, Case board, Post, Proposal, Verdict, Decision, etc.). Use this vocabulary everywhere: code, docs, commits, issues.
2. **`docs/update_spec.md`** — Full build spec with target architecture, phases, scope tiers, and ground rules. This is the source of truth for what to build and in what order.
3. **`docs/agents/domain.md`** — How to consume domain docs (`CONTEXT.md`, ADRs). Proceed silently if files don't exist yet.
4. **`docs/agents/issue-tracker.md`** — Issues are tracked in GitHub Issues via the `gh` CLI.
5. **`docs/agents/triage-labels.md`** — Five canonical triage labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`.

Also check `docs/adr/` for architectural decision records when working in a specific area.

---

## Workflow Rules

### Spec-Driven Development

Specs live in `.specify/` (created as needed). Each feature starts as a spec before implementation. Phases in `docs/update_spec.md` define the build order — finish **Must** before **Should**, **Should** before **Could**.


### Key Invariants

- Every agent tool is **read-only**. No agent can trigger a write or money movement.
- Build without AI first: the full flow must work end-to-end with scripted fake agents before any real model is plugged in.
- Every new behaviour ships with tests.
- Always work on the currently checked-out branch. Never create a feature branch.
- After completing a milestone, commit and raise a PR automatically.

---

## Agent Skills

### Issue tracker

Issues are tracked in GitHub Issues (via the `gh` CLI). See `docs/agents/issue-tracker.md`.

### Triage labels

Default label vocabulary (five canonical roles). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout (one `CONTEXT.md` + `docs/adr/` at the repo root). See `docs/agents/domain.md`.
