# AiFitness — Implementation Plan

**Author:** Malaika Gupta  
**Version:** 1.0  
**Status:** Draft for execution  

---

## 1. Purpose

This plan sequences the work needed to take AiFitness from its current state (a working microservices demo) to a production-grade platform. It is organized into phases, each with concrete tasks, priority, estimated effort, dependencies, and an exit criterion — the condition that must be true before moving to the next phase.

Effort estimates assume one engineer working part-time (evenings/weekends), so treat them as relative sizing, not calendar commitments.

---

## 2. Guiding Principles

- **Fix correctness and reliability before adding features.** A new dashboard on top of a dual-write bug is wasted effort.
- **Every phase must leave the app in a demoable state.** No long-lived broken branches.
- **Observability comes before optimization.** You cannot improve what you cannot measure.
- **Prefer boring, well-documented technology** (Resilience4j, Flyway, OpenTelemetry) over novel solutions — this is a portfolio/production project, not a research project.

---

## 3. Phase Overview

| Phase | Focus | Est. Effort |
|---|---|---|
| 0 | Foundations & safety net | 1 week |
| 1 | Backend reliability & correctness | 2–3 weeks |
| 2 | Observability & operations | 1–2 weeks |
| 3 | Frontend modernization | 2–3 weeks |
| 4 | Differentiating features | 3–4 weeks (pick 2–3) |
| 5 | Production hardening & deployment | 2 weeks |

---

## 4. Phase 0 — Foundations & Safety Net

**Goal:** De-risk everything that follows. No feature work happens until this phase exits.

| Task | Priority | Effort | Depends on |
|---|---|---|---|
| Set up CI pipeline (GitHub Actions): build + test on every PR for all 6 services + frontend | P0 | M | — |
| Add Flyway to `userservice` for PostgreSQL schema versioning | P0 | S | — |
| Add a documented Mongo schema/versioning convention for `activityservice`/`aiservice` | P0 | S | — |
| Rotate the currently-exposed Gemini API key; move secrets to Config Server (encrypted) or a secrets manager | P0 | S | — |
| Write a `docker-compose.yml` that boots the full stack (all services + Postgres + Mongo + RabbitMQ + Keycloak) in one command | P0 | M | — |
| Baseline unit test coverage report for all services (identify gaps) | P1 | S | CI pipeline |

**Exit criterion:** `docker-compose up` brings up the entire stack from a clean machine, CI runs on every PR, and no secrets are committed anywhere in the repo.

---

## 5. Phase 1 — Backend Reliability & Correctness

**Goal:** Eliminate the failure modes that would cause data loss or cascading outages in production.

| Task | Priority | Effort | Depends on |
|---|---|---|---|
| Resolve blocking `.block()` calls in `UserValidationService` and `GeminiService` — either go fully reactive or move to a blocking servlet stack per service | P0 | L | Phase 0 |
| Implement transactional outbox pattern for activity creation → RabbitMQ publish (no more silent dual-write failures) | P0 | L | — |
| Add Resilience4j circuit breaker + retry + timeout to: activityservice → userservice call, aiservice → Gemini call | P0 | M | — |
| Add dead-letter queue (DLQ) for the `activity.tracking` RabbitMQ consumer in aiservice | P0 | M | — |
| Make `ActivityMessageListener` idempotent (check existing recommendation by `activityId` before creating a duplicate) | P0 | S | DLQ |
| Replace `RuntimeException` usage in `ActivityService`/`UserValidationService` with typed exceptions + proper HTTP status mapping | P1 | S | — |
| Add request validation (Bean Validation `@Valid`) on all controller inputs (`ActivityRequest`, `RegisterRequest`) | P1 | S | — |
| Add rate limiting at the gateway (per-user/IP) ahead of the AI service path | P1 | M | — |

**Exit criterion:** Killing userservice, RabbitMQ, or the Gemini API mid-flow does not crash or corrupt state in any other service — the system degrades gracefully and recovers automatically.

---

## 6. Phase 2 — Observability & Operations

**Goal:** Make the distributed system debuggable and demonstrable to a technical interviewer in under 5 minutes.

| Task | Priority | Effort | Depends on |
|---|---|---|---|
| Add OpenTelemetry instrumentation across all 6 services | P0 | M | Phase 1 |
| Stand up Jaeger or Zipkin for trace visualization | P0 | S | OTel |
| Propagate a correlation ID from gateway → all downstream services (HTTP headers + RabbitMQ message headers) | P0 | M | OTel |
| Wire Prometheus + Grafana dashboards for: request latency (p50/p95/p99), error rate, RabbitMQ queue depth, JVM memory | P1 | M | — |
| Structured JSON logging (not plain text) across all services, shipped to a central place (even just `docker-compose` + Loki locally) | P1 | M | — |
| Add health check endpoints (`/actuator/health`) wired to real dependency checks (DB, RabbitMQ, Keycloak reachability) | P1 | S | — |

**Exit criterion:** You can trigger a real user flow (login → log activity → get recommendation), then pull up one trace in Jaeger showing the request crossing all 6 services, and one Grafana dashboard showing live metrics for that request.

---

## 7. Phase 3 — Frontend Modernization

**Goal:** Move the frontend from "functional demo" to "product-quality UI."

| Task | Priority | Effort | Depends on |
|---|---|---|---|
| Migrate data fetching from manual axios/useState to RTK Query | P0 | M | — |
| Add WebSocket/SSE-based real-time recommendation delivery (push instead of poll/refresh) | P0 | L | Backend event pipeline stable (Phase 1) |
| Add loading skeletons, empty states, and error boundaries throughout | P0 | M | — |
| Add a dashboard view with charts (calories over time, activity type breakdown, weekly trend) via `recharts` | P1 | M | RTK Query |
| Form validation via React Hook Form + Zod | P1 | S | — |
| Component/unit tests via Vitest + React Testing Library | P1 | M | — |
| Accessibility pass (ARIA labels, keyboard nav, contrast check) | P1 | S | — |
| PWA support (installable, offline view of past activities) via Vite PWA plugin | P2 | S | — |

**Exit criterion:** A first-time user can log an activity and see the AI recommendation appear live, without refreshing, with proper loading/error feedback at every step.

---

## 8. Phase 4 — Differentiating Features (pick 2–3, not all)

**Goal:** Add depth in a small number of areas rather than shipping every idea shallowly.

| Feature | Priority | Effort | Notes |
|---|---|---|---|
| Conversational AI coach (multi-turn chat per recommendation) | P1 | L | Natural extension of existing Gemini integration |
| Goals & streaks (weekly targets, streak tracking) | P1 | M | High product value, moderate effort |
| Redis caching layer (user profile lookups, recent activity lists) | P2 | M | Also demonstrates read-optimization understanding |
| Wearable/third-party integration (Strava/Google Fit import) | P2 | L | Highest effort, highest "wow" factor |
| Admin/ops dashboard (system health, queue depth, AI latency, role-gated) | P2 | M | Doubles as your observability demo |

**Recommendation:** Ship the conversational AI coach + goals/streaks first — they build directly on what already exists and have the clearest user-facing payoff.

**Exit criterion:** Each shipped feature has its own tests and is demoable independently.

---

## 9. Phase 5 — Production Hardening & Deployment

**Goal:** Make the system deployable and operable outside your local machine.

| Task | Priority | Effort | Depends on |
|---|---|---|---|
| Write Kubernetes manifests (Deployments, Services, ConfigMaps, Secrets) for all 6 services | P0 | L | Phase 0–2 |
| Add liveness/readiness probes tied to real health checks | P0 | S | K8s manifests |
| CI/CD pipeline: build → test → containerize → deploy (GitHub Actions + a target cluster, even a free-tier one) | P0 | M | K8s manifests |
| Horizontal Pod Autoscaling config for activityservice/aiservice (the most load-sensitive services) | P1 | S | K8s manifests |
| Load testing (k6 or Gatling) against the full pipeline to validate the Phase 1 resilience work | P1 | M | Phase 1–2 |
| Security review: dependency scanning (Dependabot/Snyk), JWT expiry/refresh handling, CORS tightening beyond localhost | P1 | M | — |

**Exit criterion:** The full stack deploys to a real cluster (even a single-node kind/minikube cluster is fine for demo purposes) via a single CI/CD pipeline run, survives a load test at your target RPS, and self-heals when a pod is killed.

---

## 10. Suggested Timeline (part-time pace)

```
Week 1        Phase 0 — Foundations
Weeks 2-4     Phase 1 — Backend reliability
Weeks 5-6     Phase 2 — Observability
Weeks 7-9     Phase 3 — Frontend modernization
Weeks 10-13   Phase 4 — 2-3 chosen features
Weeks 14-15   Phase 5 — Production hardening & deployment
```

Total: roughly **15 weeks** part-time to go from current state to a genuinely production-grade, deployable, observable system with 1-2 standout features.

---

## 11. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Scope creep in Phase 4 (trying to build all 5 features) | Hard cap: pick 2–3 before starting, revisit only after Phase 5 |
| Reactive/blocking mismatch fix (Phase 1) turns into a bigger rewrite than expected | Time-box to 1 week; if not resolved, fall back to isolating blocking calls on a dedicated thread pool (`Schedulers.boundedElastic()`) as an interim fix |
| Gemini API cost from load testing / feature experimentation | Add a mock/stub AI response mode for local dev and load tests, reserve real Gemini calls for demo runs |
| Kubernetes learning curve if unfamiliar | Start with `kind` (Kubernetes-in-Docker) locally before targeting a managed cluster |
