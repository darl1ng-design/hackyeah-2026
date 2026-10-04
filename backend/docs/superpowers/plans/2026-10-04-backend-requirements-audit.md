# Backend Requirements Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Verify the current backend against the ROPS Kraków challenge and exercise every implemented happy path through HTTP and persistence.

**Architecture:** Use the existing Spring Boot test application with an H2 database for HTTP, session, and persistence flows. Test the matchmaking algorithm separately with controlled vector and repository results. Record missing challenge capabilities separately from defects in implemented flows.

**Tech Stack:** Java 17, Spring Boot 4.1, MockMvc, JUnit 5, H2, Maven.

**Spec:** User-provided “TEMPLATE WYZWANIA” in this conversation, 2026-10-04.

## Global Constraints

- Do not build Docker containers.
- Guest reporting, advice, matching, and idea submission must remain possible without login.
- Staff and administrator functions require accounts; resident registration uses e-mail and password.
- Do not claim PostgreSQL, model quality, frontend accessibility, or deployed demo validation from H2 tests.

## Review Focus

- Anonymous match submission should save both report and recommended innovations for later GET.
- A registered resident should see a pending idea after login, while the public cannot see it before moderation.
- Staff should view reports; only an administrator should change status and moderate ideas.
- Search should still suggest an innovation when AI/vector/BM25 are unavailable and a keyword match exists.
- A browser should obtain a CSRF token and use it for public writes without logging in.

---

### Task 1: Application happy paths

**Files:**
- Create: `src/test/java/pl/hubmalopolski/hub/ApiHappyPathTests.java`

**Interfaces:**
- Consumes: existing `/api/v1/**` routes and JPA repositories.
- Produces: integration evidence for catalog, account, idea moderation, reports, matching, and assistant routes.

- [x] Add MockMvc tests for the review focus paths with real H2 persistence and mocked external AI/vector services.
- [x] Run `mvn -o -q -pl backend -Dtest=ApiHappyPathTests test` and inspect each result.

### Task 2: Core matchmaking fallback

**Files:**
- Create: `src/test/java/pl/hubmalopolski/hub/match/MatchmakingServiceTests.java`
- Modify if the test fails: `src/main/java/pl/hubmalopolski/hub/match/MatchmakingService.java`

**Interfaces:**
- Consumes: `InnovationRepository.searchKeyword(String, Pageable)`.
- Produces: `MatchmakingService.match(String)` with a usable result when remote search systems fail.

- [x] Write and run a failing test for keyword fallback.
- [x] Implement the smallest fallback that preserves current hybrid results when available.
- [x] Run the targeted test and full Maven suite.

### Task 3: Requirements verdict

**Files:**
- No production file required; report findings to the user with file and test evidence.

- [x] Compare each challenge module and formal requirement with the implemented routes and behavior.
- [x] Identify backend gaps, frontend/deployment gaps, and what cannot be verified without PostgreSQL and model services.
- [x] Run final verification: `mvn -o -q test`, inspect Surefire counts, and `git diff --check`.
