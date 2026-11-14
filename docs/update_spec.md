# Build spec: multi-agent customer support system

> Create a **new repository** for this project. Copy this file into it as `docs/UPGRADE_SPEC.md`, and `CONTEXT.md` into its root, then commit both as the first commit. Claude Code reads them at the start of every phase. Progress is tracked in `docs/UPGRADE_PROGRESS.md`.

---

## 1. Goal and principle

Build, in a new repository, a multi-agent customer support system for an online shop. It succeeds the `agentic-system` prototype (github.com/SAR-BAR/agentic-system), which stays unchanged as its own finished project. It handles **order and payment problems**, fixes the safe ones automatically, and hands hard ones to a human.

> **Use AI where the input is human language. Use code where the output is money.**

AI agents understand, investigate, propose and challenge. Deterministic Java does all lookups and maths, makes every decision, and is the only code that writes to the database or moves money.

**Target roles:** backend engineer first, forward-deployed engineer second. Lean on correctness, consistency and failure handling; the AI is a practical, well-guarded integration, not model research.

Use the vocabulary in `CONTEXT.md` (Conversation, Case, Problem, Case board, Post, …) everywhere: code, docs, commit messages.

## 2. The prototype this builds on (`agentic-system`; verify in Phase 0, never modify it)

- Java 21, Spring Boot 4.1.x, LangChain4j 1.2.x, Ollama (`qwen3:8b`), Spring Data JPA + MySQL, Maven. Package root `com.demo.myfirstagent`.
- **Agents-as-tools:** `CoordinationAssistant` (LLM via `AiServices`) picks between `CoordinatorTools.verifyCustomer` and `CoordinatorTools.refundProcess`, which call sub-agent LLMs (`VerifierAssistant`, `RefundProcessAssistant`) with one string each. Agents never exchange messages.
- **State:** `SupportCase` → `AgentSession` + `CoordinatorContext`, bound by a `ThreadLocal` (`SupportCaseHolder`), in memory only.
- **Likely bug:** one `MessageWindowChatMemory.withMaxMessages(20)` built once per coordinator bean, so probably shared across all customers.
- **Simulated side effects:** `processRefund` writes nothing; `STATUS_REFUNDED` is never set; `escalateTohuman` prints to stdout.
- Probable dead code: `SupportAgent` / `SupportAssitant`. Tests: `ConcurrentRefundTest`, `RefundGuardrailTest`, context test.
- Carry over as ideas (re-implemented in the new structure, not copied file by file): `PreToolGuard`, the refund limit, ownership checks, treating an agent's claim as unverified until code re-checks it, and the concurrent-refund test.

## 3. Ground rules

1. Phase 0 is read-only. Every phase starts with a plan and **waits for my approval** before editing.
2. Always work on the currently checked-out branch.
3. Never create a new branch. 
4. Never commit, push, or open a PR without explicit approval.
5. **Invariant:** every agent tool is read-only. No agent can trigger a write or a money movement.
6. **Build without AI first:** the full flow must run end-to-end with scripted fake agents before any real model is plugged in.
7. Before adding an agent, write down why it needs an LLM. If there's no good answer, it's Java.
8. Every new behaviour ships with tests (unit tests; Testcontainers for MySQL).
9. Before adding a dependency, check it supports Spring Boot 4 / Spring Framework 7. If not, say so and propose an alternative.
10. Ask before writing anything outside the repo (e.g. `~/.claude/`).
11. Work the scope tiers in order (section 6): finish **Must** completely before **Should**, and **Should** before **Could**.
12. At the end of each phase, update `docs/UPGRADE_PROGRESS.md`: what was done, decisions made, anything left over.
13. Never build anything in section 7 (out of scope).
14. Never modify the `agentic-system` prototype repository; read it only.
---

## 4. Target architecture

```
customer (signed in with Google)
  → Triage agent                → problems, orders, mood, threats
  → Java router: about an order?
       no  → General handler (Java, no case) → approved answer / settings link / human queue / refusal
       yes → Java opens one Case per order and posts FACTS on its Case board
             → Resolution and/or Billing investigate, read each other's posts, propose / revise / withdraw
             → Risk challenges money proposals ("prove it", max 2 rounds) → verdict
             → Java referee decides who acts next and enforces limits
             → Decision engine (Java): rules, all maths, no double refunds
                  EXECUTE  → Saga (refund last)
                  ESCALATE → Escalation queue → human decides → precedent
                  ASK      → ask the customer
  → Communicator agent          → one polite reply covering every problem
```

**Pattern:** a blackboard (one Case board per order) with routing (Triage + router) and debate (Risk vs specialists), run by a deterministic Java orchestrator (the referee). Explicitly rejected: an LLM supervisor/hierarchy, group chat, direct agent-to-agent messages.

### 4.1 Login and identity

- Customers sign in with **Google OAuth** (Spring Security OAuth2 client). There is no Verifier agent.
- Java checks that the signed-in customer **owns** every order referenced. Login proves identity; ownership is a separate check.
- Account changes (email, phone, address, deletion) are **never** done in chat.

### 4.2 Models

- All agents run on **local Ollama `qwen3:8b`** (via `langchain4j-ollama`). One model for everything, so a 16 GB machine never swaps models in and out.
- Each agent's model is still a config setting, so any agent can later move to another provider with a config change:

```yaml
agents:
  triage:       { provider: ollama, model: qwen3:8b }
  resolution:   { provider: ollama, model: qwen3:8b }
  billing:      { provider: ollama, model: qwen3:8b }
  risk:         { provider: ollama, model: qwen3:8b }
  communicator: { provider: ollama, model: qwen3:8b }
```

- **Prompt layout rule** (applies now, pays off later): fixed content first (tool definitions → agent instructions → policy), changing content last (case posts → customer message).
- Model failure: one retry → escalate to a human. No fallback model.

### 4.3 Agents

Each agent is an `AiServices` interface (`*Assistant`) plus a Spring wrapper (`*Agent`). It returns **records, never strings**. Malformed output → one retry with the validation error → escalate. Each agent reads only its **scoped view** of the case board.

**Triage**
- Job: read the message; find the problems, the orders they're about, the mood and any threats; label non-order messages.
- Reads: current message + recent conversation turns. Tools: none.
- Output: `TriageFinding(List<ProblemRef> problems, List<String> orderIds, GeneralType generalType, double frustration, boolean legalOrSocialThreat, boolean asksForHuman, List<String> missingFields, boolean suspiciousContent)`
    - `ProblemType`: `ORDER_STATUS, LATE_DELIVERY, WRONG_ITEM, NOT_RECEIVED, COUPON_NOT_APPLIED, REFUND_REQUEST, DOUBLE_CHARGE, WRONG_FEE, DEBITED_ORDER_FAILED, INVOICE_COPY, OTHER`
    - `GeneralType` (no order): `FAQ, ACCOUNT_CHANGE, PRE_SALES, FEEDBACK, OFF_TOPIC, UNCLEAR, NONE`

**Resolution** (order problems)
- Handles `LATE_DELIVERY, WRONG_ITEM, NOT_RECEIVED, COUPON_NOT_APPLIED, REFUND_REQUEST`.
- Reads: FACTS, problem posts, other specialists' proposals and withdrawals, challenges addressed to it, current policy text + version, precedents (Should tier).
- Tools (read-only): `getShipment`, `getCouponRules`, `getRefundPolicy`, `findPrecedents` (Should tier).
- Decides its own investigation steps (max 8 tool calls per turn).
- Posts: `PROPOSAL`, `WITHDRAWAL`, `HANDOFF`, `EVIDENCE_REPLY`.

**Billing** (payment problems)
- Handles `DOUBLE_CHARGE, WRONG_FEE, DEBITED_ORDER_FAILED, INVOICE_COPY`.
- Reads: same as Resolution.
- Tools (read-only): `getInvoiceLines`, `getPayments`, `getMembership`, `getCreditNotes`, `findPrecedents` (Should tier).
- Interprets facts Java has already computed; never computes money itself.
- Posts: `PROPOSAL`, `WITHDRAWAL`, `HANDOFF`, `EVIDENCE_REPLY`.

**Specialist coordination (the blackboard part)**
- Specialists communicate **only through posts**. No direct messages.
- Before proposing, a specialist reads the other specialist's proposals. If another proposal already covers its fix (e.g. a full refund already includes the wrong fee), it posts a `WITHDRAWAL` instead.
- At most **1 `HANDOFF`** per case (Triage put a problem on the wrong desk).

**Risk** (independent checker)
- Wakes for every money `PROPOSAL`.
- Reads: FACTS, Java-computed account signals (refunds in the last 90 days, account age, refund-to-order ratio, contacts about this order), the customer's own words, and the proposal **with its evidence but without the specialist's reasoning**.
- Tools (read-only): `getRefundHistory`, `getAccountSignals`.
- Posts: `CHALLENGE` (asks for specific evidence) or `VERDICT(ALLOW | REVIEW | HIGH_RISK, signalsCited)`.
- Max **2 challenge rounds** per proposal; unresolved → `REVIEW`. Risk never denies on its own; `REVIEW`/`HIGH_RISK` escalate.

**Communicator**
- Wakes on `DECISION`, `ESCALATED`, or a general-handler result.
- Reads: final decision + saga result + a **redacted** view (no risk signals, scores, rule names or raw personal data). Tools: none.
- Posts: `REPLY` (one reply covering every problem in the case); `HANDOFF_BRIEF` when escalating (what we checked, what we found, flags, suggested action).
- Java checks that amounts, dates and outcome in the reply match the decision, and that no internal terms leaked.

### 4.4 Case board

- One `case_posts` table, **append-only**. A row: `caseId, postId, author, type, content (typed record as JSON), evidenceIds, replyTo, createdAt`.
- Post types: `PROBLEM, MOOD, FACTS, PROPOSAL, WITHDRAWAL, HANDOFF, CHALLENGE, EVIDENCE_REPLY, VERDICT, DECISION, SAGA_STEP, ESCALATED, HUMAN_DECISION, REPLY, HANDOFF_BRIEF`.
- **Java posts `FACTS` first**: payments and their status, invoice lines and totals, delivery dates and delay, membership status, refundable amount. Agents never fetch these core facts themselves.
- **Evidence check:** every `evidenceId` in a post must exist and support the claim; Java verifies before the post is accepted.
- Current case state lives in normal tables (`cases`); the board is history, shared state and resume point.

### 4.5 Referee (Java orchestrator)

A small class with fixed `switch` rules. After every post it picks the next actor:

| Latest post | Next |
|---|---|
| `PROBLEM`, all of type `ORDER_STATUS` | Decision engine directly (answered from FACTS, no specialist) |
| `PROBLEM` | the matching specialist(s), in parallel |
| `PROPOSAL` with money | Risk |
| `PROPOSAL` without money, `WITHDRAWAL` | check whether all problems are covered |
| `HANDOFF` | the target specialist |
| `CHALLENGE` | the challenged specialist |
| `EVIDENCE_REPLY` | Risk |
| all problems covered, every money proposal has a `VERDICT` | Decision engine |
| `DECISION` / `ESCALATED` | Communicator |

Limits: 8 tool calls and 60 s per agent turn; max 2 challenge rounds per proposal; max 1 handoff per case; max 20 agent turns and 3 minutes per case. **Quiet board** (no eligible actor, no decision) → run the decision engine; if it can't decide → escalate. One referee per case at a time (per-case lock); different cases run in parallel.

### 4.6 Decision engine

Pure function from the board to `Decision(EXECUTE | ESCALATE | ASK | DENY)`. No I/O inside; inputs are loaded fresh from MySQL with row locks before it runs.

| Situation | Decision |
|---|---|
| Missing information | ASK |
| Not eligible, ownership mismatch, already refunded | DENY, with explanation |
| No money involved (order status, explanation, invoice download link) | EXECUTE |
| Total money ≤ `support.autoLimitPaise` (default ₹5,000) and every money proposal has `ALLOW` | EXECUTE |
| Over the limit, any `REVIEW`/`HIGH_RISK` | ESCALATE |
| Proposals overlap (the same rupee would be refunded twice) | combine into the larger one; if it can't be resolved cleanly → ESCALATE |

Escalation triggers override everything: legal or social-media threat; customer asks for a human; high frustration on the 3rd+ contact about the same order; budget exceeded; agent failure.

Approved proposals for one case are combined into **one saga**.

### 4.7 Saga

- Orchestrated by Java. Saga log tables `saga_instances` and `saga_steps` (`PENDING, DONE, FAILED, COMPENSATING, COMPENSATED`).
- **Irreversible step last:** reversible steps first, the refund is the pivot, steps after it are retry-only.
- Every step has an idempotency key `(sagaId, stepName)` and a compensation (except the pivot and retry-only steps).
- External systems (payment gateway, coupon service, notifier) are **adapter interfaces** with fake implementations that can be told to fail.
- Crash mid-saga → on restart, continue or compensate from the log.

| Saga | Steps (pivot in bold) | Tier |
|---|---|---|
| `REFUND` | reserve credit-note number → create credit note → **refund** → notify | Must |
| `COUPON_CREDIT` | issue coupon → notify | Must |
| `REFUND_WITH_COUPON` | reserve credit-note number → issue coupon → create credit note → **refund** → notify | Could |
| `INVOICE_RESEND` | regenerate invoice → send | Could |

### 4.8 Non-order messages (general handler)

No case and no board. Java picks the response from Triage's `GeneralType`:

| GeneralType | Handling |
|---|---|
| `FAQ` | approved answer from `approved_answers` (written by humans; the AI never invents policy) |
| `ACCOUNT_CHANGE` | link to the secure settings page; nothing changes in chat |
| `PRE_SALES` | polite "out of scope for this assistant" |
| `FEEDBACK` | low-priority item in the human queue |
| `OFF_TOPIC` / suspicious | polite refusal, logged |
| `UNCLEAR` | one clarifying question, then the human queue |

The Communicator only rephrases the chosen reply politely.

### 4.9 Memory and caching

| Memory | Scope | Implementation | Tier |
|---|---|---|---|
| Chat memory | one conversation | `chatMemoryProvider` + `@MemoryId conversationId`, persisted in MySQL | Must |
| Case board | one case, all agents | `case_posts` | Must |
| Precedents | across cases | `precedents`, written only by human decisions; `findPrecedents` by plain SQL | Should |

| Cache | What | Tier |
|---|---|---|
| Step memo | on retry/resume, reuse stored agent posts instead of calling the model again | Should |
| Answer cache | cache rephrased FAQ replies keyed by `(GeneralType, topic, policyVersion)` | Could |
| Tool cache | per-case read-through cache for extra tool calls, cleared on any saga write | Could |
| Prompt cache | provider-side prompt caching, once a provider that supports it is configured | Deferred |

**Rule:** cache for thinking, never for acting. The decision engine and saga never read a cache; an **ArchUnit** test enforces it once any cache exists.

### 4.10 Tables (Flyway migrations)

Domain: `customers`, `orders` (`@Version`), `order_items`, `shipments`, `invoices`, `invoice_lines`, `payments`, `memberships`, `refunds` (unique idempotency key), `credit_notes`, `coupons`, `refund_policies` (versioned), `approved_answers`.
Platform: `conversations`, `chat_memory`, `cases`, `case_posts` (append-only), `escalations`, `precedents`, `saga_instances`, `saga_steps`, `outbox`.

### 4.11 UI and interfaces

- Plain HTML + a little JavaScript served by Spring. No front-end framework.
- **Customer chat page** with Google sign-in (Must).
- **Board viewer:** a case's posts appearing live over SSE; also replays a recorded case step by step (Must).
- **Human console:** escalated cases with the handoff brief, approve/reject (Should).
- REST + OpenAPI: `POST /conversations/{id}/messages`, `GET /cases/{id}` (board + state), `GET /cases/{id}/stream` (SSE), `POST /escalations/{id}/resolve` (Must, except the escalation endpoint which comes with the console).
- **Product MCP server** exposing `get_case`, `list_escalations`, `resolve_escalation` through the same services and guards (Could).

---

## 5. Phases

### Phase 0: Carry-over audit of the prototype (read-only)
- Clone `agentic-system` outside the new repo and read it; change nothing there.
- Confirm or correct every point in section 2.
- Confirm or disprove the shared chat-memory bug by reading the code (and, if useful, a throwaway test run in the clone that is never committed).
- In the new repo, write `docs/audit.md`: the prototype's architecture, bugs found (including the memory bug), and a **carry-over list**: which ideas, guards and tests to re-implement, and where each lands in the new structure. Create `docs/UPGRADE_PROGRESS.md`.
- **Done when:** I've reviewed and approved the audit.

### Phase 1: Claude Code setup
- **`CLAUDE.md`** (root, under ~150 lines): goal and principle; "use `CONTEXT.md` vocabulary"; package map; agent roster; invariants (no LangChain4j in `domain/`; agent tools read-only; money only through the saga; the decision engine is pure; never cache the money path; every tool has a guard + tests); build/test/run commands; naming (`*Agent` = Spring wrapper, `*Assistant` = AiServices interface); "read `docs/UPGRADE_SPEC.md` and `docs/UPGRADE_PROGRESS.md` before starting work".
- **Nested `CLAUDE.md`** in `agent/` (records only, prompt layout rule, read-only tools), `board/` (append-only, evidence checks), `decision/` (pure, every rule tested), `saga/` (idempotent steps, pivot last).
- **`.claude/settings.json`**: allow `mvn` build/test, `docker compose` up/down/logs, read-only git; deny reading `.env*` and `**/application-prod*`, `git push --force`, `rm -rf`. Hooks in `.claude/hooks/`:
    - PreToolUse `Edit|Write`: block secrets/prod config and already-committed Flyway migrations
    - PostToolUse `Edit|Write` on `*.java`: Spotless on the file
    - Stop: run the fast test suite and report failures back
    - SessionStart: inject the branch, uncommitted changes and the latest `UPGRADE_PROGRESS.md` entry
- `.claude/settings.local.json` in `.gitignore`.
- **`.claude/agents/`**: `red-teamer.md` (adversarial tests under `src/test/.../adversarial/`, cannot edit `src/main/`), `architecture-reviewer.md` (read-only; checks a diff against the invariants and ADRs), `test-writer.md` (tests only).
- **`.claude/skills/`**: `add-agent`, `add-post-type`, `add-saga`, `write-adr`.
- **`.mcp.json`**: a read-only MySQL MCP server for the local Docker DB (development only).
- **Global (propose only, ask first, never commit)**: `~/.claude/CLAUDE.md`, `~/.claude/settings.json`.
- **Done when:** each hook demonstrably fires and `CLAUDE.md` is accurate.

### Phase 2: Core without AI
- Scaffold the project: Maven, Java 21, Spring Boot 4, Flyway, Testcontainers, Spotless, a real group id and package root. Hexagonal layering from the start (domain has no framework/LangChain4j imports; agents behind interfaces).
- Re-implement the carry-over items from `docs/audit.md` in the new structure.
- Flyway migrations from 4.10 with seed data: customers, orders, a late delivery, a double charge, a wrong delivery fee with an active membership, a debited-but-failed order, policy v1, approved FAQ answers.
- Records from 4.3; `case_posts`, scoped views, evidence checks; FACTS computation.
- Referee (4.5), decision engine (4.6), general handler (4.8). Every referee rule, decision rule and limit has a unit test.
- **Scripted fake agents** that read the board and post fixed sequences, including withdrawals, handoffs and challenges.
- **Done when:** these run end-to-end with no model calls: an FAQ question; an account change; a single late delivery; a double charge; late delivery + wrong fee on one order where Billing withdraws; a Risk challenge answered with evidence; an over-limit escalation; a legal-threat escalation; a message about two orders (two cases).

### Phase 3: Real agents
- Implement the five agents on Ollama `qwen3:8b` with per-agent config (4.2) and the prompt layout rule.
- Per-conversation persisted chat memory, with a test proving two customers never share memory (the bug found in the prototype).
- Injection posture: customer text and tool output wrapped as data; guards and evidence checks re-validate everything.
- Red-teamer adds adversarial tests: injection, social engineering, made-up evidence ids, someone else's order, amount tampering.
- **Done when:** the Phase 2 scenarios pass with real agents, and the architecture-reviewer confirms no agent tool can write.

### Phase 4: Execution and durability
- Saga orchestrator, saga log, `REFUND` and `COUPON_CREDIT` sagas, adapter interfaces with failure-injectable fakes.
- Tests: failure at every step compensates; failure after the pivot only retries; crash mid-saga then restart never refunds twice; N concurrent identical requests produce exactly one refund (the prototype's concurrent-refund test, rebuilt); overlapping proposals never refund the same amount twice.
- Persisted escalation queue; outbox events for refunds and escalations; problem-details error model; Spring profiles `local`, `test`, `prod`.
- **Done when:** all of the above is covered by tests.

### Phase 5: Login, interfaces and UI
- Google OAuth sign-in; ownership checks on every order reference.
- REST + OpenAPI and SSE endpoints (4.11).
- Customer chat page and board viewer (live and replay).
- **Done when:** a signed-in customer can raise a case in the browser and watch its board fill in live.

### Phase 6: Proof and packaging
- **Small eval:** about 50 scripted conversations with expected decisions, covering every Phase 2 scenario plus adversarial ones. Report % correct decisions and the number of unsafe actions (target 0).
- **Recorded replays:** record real local runs and ship them with the app; the board viewer replays them.
- **Deployed demo** (e.g. Render or Fly) running in **replay mode**: no model needed, free to host.
- `docker compose up` starts the app, MySQL and Ollama with seed data. GitHub Actions: build → unit tests → Testcontainers tests.
- `docs/adr/`, at least 7 ADRs:
    1. blackboard per order instead of a pipeline or group chat
    2. Java referee instead of an LLM supervisor
    3. pure decision engine
    4. orchestrated saga with the refund last
    5. Google login + Java ownership checks instead of an LLM verifier
    6. local single model with per-agent config
    7. non-order messages handled without a case
- README: problem → architecture diagram → agent roster → case board and referee → money path → eval results → trade-offs and limits → development workflow with Claude Code → demo link → "where this started": a link to the `agentic-system` prototype and what changed.
- **Done when:** a clean clone runs by following only the README, and the demo link works.

### Phase 7: Should tier
- Human console page and `POST /escalations/{id}/resolve`.
- Precedents written on human decisions; `findPrecedents` available to the specialists.
- Step memo on resume.
- **Done when:** a human resolution resumes its case and the next similar case cites the precedent.

### Phase 8: Could tier (only if time allows; any order)
- Product MCP server (4.11). Answer cache and tool cache, with the ArchUnit rule. Risk self-consistency (3 runs above ₹1,000; disagreement → `REVIEW`). Frustration trend (moving average across turns) in the escalation triggers. `REFUND_WITH_COUPON` and `INVOICE_RESEND` sagas. A second model provider for any agent once API access exists.

---

## 6. Scope tiers

| Tier | Contents | Phases |
|---|---|---|
| **Must** | five agents, case board + referee, decision engine, general handler, `REFUND` + `COUPON_CREDIT` sagas, escalation queue, Google login, chat page + board viewer, fake-agents-first testing, small eval, recorded replays + deployed demo, Claude Code setup | 0–6 |
| **Should** | human console, precedents, step memo | 7 |
| **Could** | product MCP server, answer cache, tool cache, Risk voting, frustration trend, extra sagas, second model provider | 8 |

## 7. Out of scope (do not build)

LLM supervisor or hierarchy, group chat, direct agent-to-agent messages, dynamic model routing or cascades, what-if simulator, photo evidence, vector DB/RAG, fine-tuning, React or any front-end framework, Kubernetes, microservices split, a large eval suite, metrics/tracing dashboards, cost tracking.

## 8. Definition of done (v1 = Must tier)

- [ ] Phases 0–6 merged; all tests green, adversarial tests included.
- [ ] The full flow runs with fake agents (no model calls) and with real local agents.
- [ ] No agent tool writes or moves money (architecture-reviewer confirms).
- [ ] No amount is ever refunded twice: concurrency, crash and overlapping-proposal tests pass.
- [ ] Eval reported in the README (X/50 correct, 0 unsafe actions).
- [ ] The demo link replays recorded cases.
- [ ] Every Claude Code file from Phase 1 exists and is explained.
- [ ] `docker compose up` works from a clean clone.
