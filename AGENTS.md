# AGENTS.md

## Repo layout

Monorepo: Spring Boot backend (root) + React SPA (`frontend/`).

- `src/main/java/com/codercup/jobmatchai/` — Java backend (Spring Boot 4.1, Java 21)
- `src/test/java/` — 18 JUnit 5 test files
- `frontend/src/` — React 19 + TypeScript 7 + Vite 8 + Vitest
- `data/` — H2 file database (gitignored, created at runtime)
- `.env` — backend secrets (gitignored, copy from `.env.example`)
- `frontend/.env` — `VITE_API_URL` (defaults to `http://localhost:8080`)

## Commands

**Backend tests** (no env needed, tests use mocks):
```
.\mvnw.cmd test
```

**Frontend tests** (from repo root):
```
npm test --prefix frontend
```

**Frontend typecheck:**
```
npm run typecheck --prefix frontend
```

**Frontend build:**
```
npm run build --prefix frontend
```

**Run backend locally:**
```
.\mvnw.cmd spring-boot:run
```

**Run frontend locally:**
```
npm run dev --prefix frontend
```

CI runs all five: `backend test`, `frontend typecheck`, `frontend test`, `frontend audit`, `frontend build`, then `docker build`.

## Key architecture facts

- **Gemini never calculates scores.** It returns structured requirement assessments (match/partial/missing). `MatchScoreCalculator` computes the deterministic percentage in Java.
- **Frontend has no router library.** Uses browser History API directly in `App.tsx` with custom route matching (`/analizar`, `/historial`, `/historial/:id`).
- **Frontend history is localStorage only** (`jobmatch-ai-history` key, max 50 records). The backend's `/api/analyses` CRUD endpoints exist but the frontend does not call them by default.
- **Security is off by default** (`SECURITY_ENABLED=false`). When enabled, uses HTTP Basic with in-memory user. All analysis endpoints are public either way.
- **CORS** configured via `cors.allowed-origins` property, defaults to `http://localhost:5173`.
- **Rate limiting** applies to `POST /api/analyze`, `/api/analyses`, `/api/jobs/search` only. Default 10 req/min. Uses `CF-Connecting-IP` header (Cloudflare) or `remoteAddr`.

## Testing patterns

- Backend: `@SpringBootTest` + MockMvc for controllers, unit tests for services. No external services required — `GeminiService` and `JobicyClient` are mocked in tests.
- Frontend: Vitest with `renderToStaticMarkup` (no DOM/testing-library). Heavy use of `vi.mock` and `vi.stubGlobal('fetch', ...)`. Tests live next to source as `*.test.ts` / `*.test.tsx`.
- Frontend mock mode: set `VITE_USE_MOCKS=true` to bypass API calls in `api.ts` (reads from `src/lib/mocks/analysisMock.ts`).

## Gotchas

- `application.properties` loads `.env` via `spring.config.import=optional:file:.env[.properties]` — values from `.env` override defaults.
- H2 database path is `./data/jobmatch` (relative to working dir). The `data/` directory is gitignored.
- Frontend `api.ts` strips trailing slashes from `VITE_API_URL`.
- PDF upload max 5MB, job description max 5000 chars, image max 5MB/8000px.
- Tests must pass on Java 21 and Node 20.19+ (CI uses exact Node `20.19.0`).
