# Prepr — AI Interview Coaching Platform

An AI-powered platform that helps students and job seekers prepare for technical interviews:
realistic AI-conducted interview simulations, answer evaluation, coaching plans, skill tracking,
resume analysis, and a knowledge-base assistant with Retrieval-Augmented Generation (RAG).

- **Stack:** Java 26 · Spring Boot 4.0.6 · Spring MVC · Spring Data JPA · Spring AI 2.0.0-M8
  (OpenAI-compatible client) · PostgreSQL + PGVector · Redis · Apache Tika
- **Frontend:** framework-free HTML/CSS/vanilla-JS SPA (ES modules, hash router, AG Grid), served by Spring at `/`
- **Architecture:** a single-module **modular monolith** organized by business domain

---

## Key Features

- **Adaptive AI interviewer** — runs a conversational mock interview, asks context-aware follow-ups and decides when to move to the next question. Optional voice mode uses the browser Web Speech API.
- **Answer evaluation and reports** — answers are scored asynchronously against a rubric (technical accuracy, completeness, practical understanding, communication clarity); finishing an interview triggers a report with strengths, weaknesses, recommendations and an overall score.
- **Coaching plans and skill tracking** — plans derived from interview history, plus long-term skill progression per user.
- **Resume analysis** — upload a resume and get an AI analysis.
- **RAG knowledge assistant** — upload documents, then ask questions with metadata-filtered vector search over PGVector, optional LLM re-ranking and source citations.
- **Conversation memory** — recent turns cached in Redis, periodic summaries, and long-term semantic memory stored in PGVector.
- **Security** — stateless JWT authentication, Google/GitHub OAuth2 login, role-based access control and ownership checks.

---

## Table of Contents
1. [Architecture](#architecture)
2. [Modules](#modules)
3. [AI & RAG Workflow](#ai--rag-workflow)
4. [Prerequisites](#prerequisites)
5. [Setup (Docker infra + LM Studio)](#setup)
6. [Configuration](#configuration)
7. [Running the app](#running-the-app)
8. [Development workflow](#development-workflow)
9. [API overview](#api-overview)
10. [Frontend](#frontend)
11. [Deployment & production readiness](#deployment--production-readiness)
12. [Troubleshooting](#troubleshooting)

---

## Architecture

A synchronous Spring MVC monolith on embedded Tomcat. Each business domain owns its full vertical slice —
`web` (controllers) → `service` (interfaces) → `service/impl` → `repository` → `domain` (entities) →
`dto`. Cross-cutting infrastructure lives in `common`, `config`, and `security`.

```
Browser SPA (static/) ──HTTP/JSON──► Spring MVC controllers
                                          │
                                     service layer
                          ┌───────────────┼───────────────────────────┐
                          ▼               ▼                           ▼
                   Spring AI ChatClient   JPA/Hibernate          Redis (short-term
                   → OpenAI-compatible    → PostgreSQL            conversation memory)
                     endpoint (LM Studio)   + PGVector
```

Key design decisions:
- **LLM calls never run inside a DB transaction.** Services load inputs, drop the JPA session, call the
  model, then persist results in a short transaction (see `common/tx/TransactionRunner`). This avoids
  holding a connection open across multi-second inference.
- **Non-critical AI work runs off-request** on one bounded executor (`config/AsyncConfig`): answer
  evaluation, report generation, conversation-summary refresh, and semantic-memory extraction. Bursts
  are absorbed by a queue with `CallerRunsPolicy` backpressure.
- **Interview completion is decoupled via an in-process event** (`InterviewCompletedEvent`): the report
  module listens, generates the report, then triggers skill aggregation — no compile-time dependency
  from `interview` → `report`/`skill`.
- **`open-in-view: false`** — no lazy loading in the web layer; services map to DTOs explicitly.

---

## Modules

| Package | Responsibility |
|---|---|
| `security` | Stateless JWT auth (OAuth2 Resource Server), login/register, Google/GitHub social login, RBAC + ownership guards |
| `user` | User accounts, roles, profile |
| `interview` | Interview sessions, questions, answers, the conversational AI interviewer, adaptive question flow, answer evaluation, question bank |
| `report` | Post-interview report generation (strengths / weaknesses / recommendations / summary / overall score) |
| `coaching` | Coaching plans derived from interview history and skill trends |
| `skill` | Long-term skill tracking / progression |
| `resume` | Resume upload + AI analysis |
| `knowledgebase` | Knowledge bases, document upload, Tika parsing, token chunking, embedding into PGVector |
| `retrieval` | Vector search + RAG question-answering (`/api/search`, `/api/ask`) with LLM reranking and citations |
| `conversation` | Conversations, messages, Redis-backed recent-message cache |
| `memory` | Conversation summaries, long-term semantic memory (PGVector), context assembly |
| `chat` | General-purpose memory-aware assistant |
| `common` | Base entity, exceptions, global error handling, transaction helper, shared AI presets |
| `config` | Async executor, CORS, memory tunables, custom LM Studio health indicator |

---

## AI & RAG Workflow

**Interview turn** (`POST /api/voice/interviews/{id}/respond`): load interview state + recent history →
prompt the model as a natural interviewer (structured `InterviewerTurn{reply, moveToNextQuestion}`) →
persist candidate + interviewer messages → when advancing, record and asynchronously **evaluate** the
answer → on completion, publish an event that generates the report and updates skills.

**Answer evaluation** (async): the LLM scores an answer against a rubric
(technical accuracy / completeness / practical understanding / communication clarity) and stores
score + strengths/weaknesses/suggestions.

**RAG / Ask** (`POST /api/ask`): embed query → vector search (top-K) over one knowledge base
(metadata-filtered by `knowledgeBaseId`) → optional LLM **rerank** of the candidate pool → answer from
only the top passages, with source citations. Conversation memory is reused when a `conversationId`
is supplied.

**Document ingestion**: upload → Tika parse → `TokenTextSplitter` (≈300-token chunks) → embed with the
`search_document:` prefix → store in PGVector; a relational copy is kept in `document_chunks` for browsing.

**Memory**: short-term recent turns in Redis (capped list, TTL-refreshed, PostgreSQL as source of truth);
periodic LLM **summary** once enough new messages accumulate; long-term **semantic memory** extracted by
the LLM into categories (PERSONAL / PREFERENCE / FACT / PROJECT / GOAL) and stored in PGVector.

Embeddings use the Nomic prefix convention: `search_document:` for stored chunks, `search_query:` for
queries. The query is embedded exactly once (by the vector store).

---

## Prerequisites

- **JDK 26**
- **Docker** (for PostgreSQL+PGVector and Redis)
- **LM Studio** (or any OpenAI-compatible endpoint) serving one chat model and one embedding model.
  The Maven wrapper (`./mvnw`) is included — no separate Maven install needed.

---

## Setup

Built-in defaults target local infra, so if you match them you can run with **no env vars**.
Defaults: Postgres `localhost:5433` db `ragdb` user `root`/`root`; Redis `localhost:6379`;
LM Studio `http://127.0.0.1:1234/v1`; embeddings **768 dims**.

### 1. PostgreSQL + PGVector
Use an image that ships the `vector` extension (the plain `postgres` image will fail vector-store init).
```bash
docker run -d --name prepr-postgres -p 5433:5432 \
  -e POSTGRES_USER=root -e POSTGRES_PASSWORD=root -e POSTGRES_DB=ragdb \
  pgvector/pgvector:pg16
docker exec -it prepr-postgres psql -U root -d ragdb -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### 2. Redis
```bash
docker run -d --name prepr-redis -p 6379:6379 redis:7
docker exec -it prepr-redis redis-cli ping   # expect PONG
```

### 3. LM Studio (or any OpenAI-compatible endpoint)
Load **two** models and start the local server on port **1234**:
- a **chat** model (default id `google/gemma-4-e4b`)
- an **embedding** model (default id `text-embedding-nomic-embed-text-v2-moe`, **must output 768 dims**)

Verify:
```bash
curl http://127.0.0.1:1234/v1/models                       # ids must match config
curl http://127.0.0.1:1234/v1/embeddings \
  -H "Content-Type: application/json" \
  -d '{"model":"text-embedding-nomic-embed-text-v2-moe","input":"hello"}'   # embedding length must be 768
```
> If the embedding model's dimension differs from **768**, update `spring.ai.vectorstore.pgvector.dimensions`
> and drop any existing `vector_store` table before restarting.

---

## Configuration

All settings live in `src/main/resources/application.yml` and are env-overridable. Override only what
differs from the defaults above.

| Var | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/ragdb` | |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | |
| `AI_BASE_URL` | `http://127.0.0.1:1234/v1` | OpenAI-compatible endpoint |
| `AI_API_KEY` | `lm-studio` | any non-empty value for LM Studio |
| `AI_CHAT_MODEL` | `google/gemma-4-e4b` | must match a served model |
| `AI_EMBEDDING_MODEL` | `text-embedding-nomic-embed-text-v2-moe` | must output **768** dims |
| `DDL_AUTO` | `update` | dev only — use `validate` + migrations in prod |
| `SECURITY_JWT_SECRET` | dev placeholder | **must be set in prod**, ≥ 32 bytes |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | — | enable Google login |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | — | enable GitHub login |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_USERNAME` | empty / empty / `Administrator` | provisions an admin at startup |
| `LOG_LEVEL` | `info` | set `debug` (e.g. `com.springrag.retrieval`) to trace retrieval/ingestion |

RAG/memory tunables (under `rag.*` in `application.yml`): `rerank-enabled`, `memory.max-messages`,
`memory.ttl`, `memory.summary-threshold`, `memory.semantic-top-k`, `memory.duplicate-threshold`.

> **Security:** OAuth client secrets and the JWT secret must come from the environment / a secret
> manager in production — never commit real credentials. Provision the first admin via `ADMIN_*`.

---

## Running the app

```bash
./mvnw spring-boot:run          # start (http://localhost:8080)
./mvnw clean package            # build a runnable jar
java -jar target/spring-rag-0.0.1-SNAPSHOT.jar
```

First-run bootstrap: with `ADMIN_EMAIL` + `ADMIN_PASSWORD` set, an administrator is created at startup;
log in with those credentials. Open `http://localhost:8080/` for the SPA.

Health & metrics: `GET /actuator/health` (aggregates PostgreSQL, Redis, and LM Studio), `/actuator/info`,
`/actuator/metrics`.

---

## Development workflow

```bash
./mvnw test                     # run tests
./mvnw clean package            # full build
```
- Package by **domain**, not by layer. A new feature owns its own `web/service/impl/repository/domain/dto`.
- Keep LLM calls out of transactions; use `TransactionRunner` to persist results afterward.
- Put off-request AI work on the shared `AsyncConfig.AI_EXECUTOR`.
- `api-workflow.http` contains ready-to-run requests that walk the full user journey (auth → interview →
  report → knowledge base → ask). Use it with an HTTP client (e.g. the IntelliJ HTTP runner).

---

## API overview

All endpoints are under `/api` and (except auth + static + health) require a `Bearer` JWT.

| Base path | Purpose |
|---|---|
| `/api/auth` | Register, login (issues JWT) |
| `/api/users` | User management (admin) |
| `/api/interviews` | Create/list interviews, lifecycle |
| `/api/interview-questions`, `/api/interview-answers` | Questions & answers |
| `/api/voice/interviews/{id}/respond`, `/transcript` | Conversational interviewer channel |
| `/api/interview-reports` | Generate / fetch reports |
| `/api/coaching` | Coaching plans |
| `/api/user-skills` | Skill progression |
| `/api/resumes` | Resume upload & analysis |
| `/api/knowledge-bases`, `/api/documents` | Knowledge base & document management |
| `/api/search`, `/api/ask` | Vector search & RAG Q&A |
| `/api/conversations`, `/api/memory`, `/api/memories` | Conversations, context, semantic memory |
| `/api/chat` | General assistant |
| `/api/admin/questions` | Question bank administration |

Conventions: DTO-in / DTO-out, `@Valid` request validation, ownership/RBAC via method-level
`@PreAuthorize`, and consistent error bodies from `common/web/GlobalExceptionHandler`.

---

## Frontend

Served from `src/main/resources/static/`. Framework-free ES modules — no build step, no bundler.

```
js/core/     api.js (single HTTP choke point), router.js (hash router), store.js (session + pub/sub),
             dom.js, format.js, voice.js (Web Speech API)
js/api/      thin per-domain API wrappers
js/ui/       shell, components, grid (AG Grid), overlay, toast
js/views/    one file per screen (auth, home, interviewRoom, report, assistant, admin, …)
css/         tokens, base, layout, components, app
```
Auth: the JWT is stored in `localStorage` and attached by `core/api.js`; a `401` on an authenticated
call clears the session and routes to login. OAuth2 login redirects back to `/?token=<jwt>`.

Voice interview: microphone capture, speech-to-text, and text-to-speech use the browser **Web Speech API**;
the server drives the interview logic.

---

## Deployment & production readiness

Before deploying to a real environment, address the following (the code is otherwise structurally sound):

- **Secrets:** set `SECURITY_JWT_SECRET` and all OAuth secrets from the environment; never rely on
  the baked-in dev defaults. Add a `prod` profile that fails fast if the JWT secret is the default.
- **Schema:** switch from `DDL_AUTO=update` to `validate` and manage schema with Flyway (generate the
  baseline from the current dev schema, including the `vector_store` table).
- **Rate limiting & cost control:** throttle LLM-backed endpoints (`/api/voice/**`, `/api/chat`,
  `/api/ask`) per user and cap interview length — Redis is already available.
- **Observability:** add business metrics (LLM latency, failure rate, token usage, async queue depth)
  and propagate a correlation id into async work; restrict `/actuator/health` detail exposure.
- **Streaming (recommended):** the interactive interview and chat replies currently return in a single
  blocking response; token streaming (SSE) is the largest perceived-latency improvement available.

---

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| Vector store fails on boot | Plain `postgres` image without pgvector — use `pgvector/pgvector`, and `CREATE EXTENSION vector` |
| App can't reach Postgres | Wrong port — default is **5433**, not 5432 |
| RAG returns nothing / empty answers | Embedding model not served, or dimension ≠ 768; verify with the `curl` checks in [Setup](#setup) |
| `Semantic search returned 0 chunks` | Document ingested into a different knowledge base, or ids mismatch — check `knowledgeBaseId` |
| 404 / connection refused to the model | LM Studio server not started or model not loaded; `AI_CHAT_MODEL`/`AI_EMBEDDING_MODEL` must match served ids |
| 401 on every API call | Missing/expired JWT, or `SECURITY_JWT_SECRET` changed between runs (invalidates old tokens) |
| Dimension changed | Update `spring.ai.vectorstore.pgvector.dimensions` and `DROP TABLE vector_store;` before restart |
| Redis connection refused | Redis container not running — `docker exec -it prepr-redis redis-cli ping` should return `PONG` |
