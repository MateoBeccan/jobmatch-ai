# JobMatch AI

Plataforma web que analiza la compatibilidad entre un CV y una oferta laboral usando **Google Gemini** y calcula un porcentaje determinista de coincidencia mediante Java. Desarrollada como proyecto academico para **CoderCUP IA 2026** por [Mateo Beccan](https://github.com/MateoBeccan) y [Francisco Lorenzo](https://github.com/franLorenzo28)).

**Frontend:** React 19 + TypeScript + Vite + Tailwind CSS  
**Backend:** Spring Boot 4.1.0 + Java 21 + H2 + Spring Security  
**Despliegue:** Docker + Vercel (frontend) + Render (Backend) + GitHub Actions CI  

---

## Tabla de contenido

- [Funcionalidades](#funcionalidades)
- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Configuracion local](#configuracion-local)
- [Ejecutar](#ejecutar)
- [API REST](#api-rest)
- [Algoritmo de scoring](#algoritmo-de-scoring)
- [Integracion con Gemini](#integracion-con-gemini)
- [Busqueda de empleos](#busqueda-de-empleos)
- [Seguridad](#seguridad)
- [Rate limiting](#rate-limiting)
- [Frontend](#frontend)
- [Testing](#testing)
- [CI/CD](#cicd)
- [Docker](#docker)
- [Produccion](#produccion)
- [Estructura del proyecto](#estructura-del-proyecto)

---

## Funcionalidades

- **Analisis de compatibilidad**: Subi tu CV (PDF) y una descripcion de puesto (texto o imagen) y obtenes un porcentaje de compatibilidad con desglose por categorias.
- **Requisitos detallados**: Lista de requisitos extraidos del puesto, evaluados como Cumplido / Parcial / Faltante con evidencia.
- **Recomendaciones**: Sugerencias concretas de que mejorar en tu CV.
- **Optimizacion de CV**: Panel especifico con acciones concretas para cada requisito faltante.
- **Preguntas de entrevista**: 3-5 preguntas generadas por IA basadas en el puesto y tu perfil.
- **Perfil de busqueda laboral**: Deteccion automatica de rol, seniority y keywords a partir del CV.
- **Busqueda de empleos remotos**: Integracion con Jobicy para encontrar ofertas relevantes por keywords y ubicacion.
- **Historial local**: Guarda hasta 50 analisis en localStorage con busqueda, filtros por score, ordenamiento y comparacion versionada.
- **Comparacion versionada**: Compara dos analisis del mismo rol/puesto para ver que requisitos nuevos cumpliste.
- **Dark mode**: Tema oscuro completo con transiciones suaves.
- **Responsive**: Diseno mobile-first con navegacion inferior en movil.
- **Multiidioma del CV**: Acepta CVs en espanol e ingles.

---

## Arquitectura

```
┌──────────────────────────────────────────────────────┐
│                    Frontend (React)                    │
│  Vite :5173 → AnalyzerPage → Results → JobSearchPanel │
│  Historial localStorage → HistoryScreen               │
└──────────────────────┬───────────────────────────────┘
                       │ fetch /api/*
┌──────────────────────▼───────────────────────────────┐
│                  Backend (Spring Boot)                 │
│  AnalysisController ─→ AnalysisService                │
│  JobSearchController ─→ JobSearchService              │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │ GeminiService │  │ JobicyClient │  │ RateLimit  │ │
│  │  (Google AI)  │  │  (Jobicy API)│  │  Filter    │ │
│  └──────────────┘  └──────────────┘  └────────────┘ │
│  ┌──────────────────────────────────────────────────┐ │
│  │         MatchScoreCalculator (determinista)       │ │
│  └──────────────────────────────────────────────────┘ │
│  H2 (file: ./data/jobmatch)                           │
└──────────────────────────────────────────────────────┘
```

- El frontend es una SPA con enrutamiento custom via History API (sin react-router).
- El backend expone una REST API publica para analisis y busqueda, con endpoints `/api/analyses` protegidos cuando `SECURITY_ENABLED=true`.
- El historial se almacena **exclusivamente en `localStorage` del navegador**; el frontend no persiste datos en el backend.
- El scoring es **determinista**: Gemini solo clasifica requisitos, Java calcula el porcentaje. La misma entrada siempre produce el mismo score.

---

## Requisitos

| Componente | Version |
|---|---|
| Java | 21 |
| Maven | (incluido via wrapper `mvnw`) |
| Node.js | >= 20.19.0 |
| npm | (incluido con Node) |
| Clave de Google Gemini | (obligatoria) |

---

## Configuracion local

### Backend

Crea un archivo `.env` en la raiz del proyecto:

```env
# Obligatorio
GEMINI_API_KEY=tu_clave_de_gemini

# Opcional (valores por defecto)
GEMINI_MODEL=gemini-3.6-flash
GEMINI_TIMEOUT_MS=30000
GEMINI_RETRY_ATTEMPTS=2
GEMINI_RETRY_DELAY_MS=500
GEMINI_MAX_CONCURRENT_REQUESTS=4

SECURITY_ENABLED=false
DEMO_USERNAME=demo
DEMO_PASSWORD=demo-password
RATE_LIMIT_PER_MINUTE=10

CORS_ALLOWED_ORIGINS=http://localhost:5173

DATABASE_URL=jdbc:h2:file:./data/jobmatch;AUTO_SERVER=TRUE
DATABASE_USERNAME=sa
DATABASE_PASSWORD=
JPA_DDL_AUTO=update

JOBICY_BASE_URL=https://jobicy.com/api/v2/remote-jobs
JOBICY_TIMEOUT_MS=5000
JOBICY_RESULT_LIMIT=100
JOBICY_CACHE_TTL_MINUTES=60
JOB_SEARCH_MAX_RESULTS=8

# Limites de procesamiento (opcionales)
ANALYSIS_MAX_DESCRIPTION_LENGTH=5000
ANALYSIS_MAX_PDF_PAGES=50
ANALYSIS_MAX_PDF_TEXT_LENGTH=50000
ANALYSIS_HISTORY_MAX_PAGE_SIZE=50

# Puerto del servidor (opcional)
PORT=8080
```

### Frontend

Crea `frontend/.env`:

```env
VITE_API_URL=http://localhost:8080
```

---

## Ejecutar

### Backend

```powershell
.\mvnw.cmd spring-boot:run
```

Corre en `http://localhost:8080`.

### Frontend

```powershell
npm install --prefix frontend
npm run dev --prefix frontend
```

Disponible en `http://localhost:5173`.

---

## API REST

### Analisis

| Metodo | Ruta | Auth | Descripcion |
|---|---|---|---|
| `POST` | `/api/analyze` | No | Analiza CV + oferta, devuelve resultado sin persistir |
| `POST` | `/api/analyses` | Si | Analiza y guarda en historial |
| `GET` | `/api/analyses` | Si | Lista historial paginado |
| `GET` | `/api/analyses/{id}` | Si | Detalle de un analisis |
| `DELETE` | `/api/analyses/{id}` | Si | Elimina un analisis |

### Busqueda de empleos

| Metodo | Ruta | Auth | Descripcion |
|---|---|---|---|
| `POST` | `/api/jobs/search` | No | Busca ofertas remotas por perfil |

### Salud

| Metodo | Ruta | Auth | Descripcion |
|---|---|---|---|
| `GET` | `/actuator/health` | No | Health check (`{"status":"UP"}`) |

### POST /api/analyze

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Requerido | Descripcion |
|---|---|---|---|
| `cvFile` | File (PDF) | Si | CV del candidato, max 5 MB |
| `jobDescription` | String | * | Descripcion del puesto en texto (max 5000 chars) |
| `jobImage` | File (PNG/JPEG/WEBP) | * | Descripcion del puesto como imagen, max 5 MB, max 8000px |

\* Exactamente uno de `jobDescription` o `jobImage` debe estar presente.

**Respuesta 200:**

```json
{
  "matchPercentage": 75,
  "matchingSkills": ["Java", "Spring Boot", "SQL"],
  "missingSkills": ["Kubernetes", "AWS"],
  "recommendations": ["Inclui proyectos con cloud computing"],
  "interviewQuestions": ["Como manejas la escalabilidad?"],
  "requirements": [
    {
      "name": "Java 17+",
      "category": "MANDATORY_TECHNICAL",
      "status": "match",
      "evidence": "El candidato menciona Java 17 en experiencia"
    }
  ],
  "breakdown": {
    "mandatoryTechnical": 80,
    "experienceSeniority": 60,
    "desirable": 50,
    "complementary": 100
  },
  "jobSearchProfile": {
    "role": "Backend Developer",
    "seniority": "MID",
    "keywords": ["java", "spring boot", "rest api"]
  }
}
```

### POST /api/jobs/search

**Content-Type:** `application/json`

```json
{
  "role": "Backend Developer",
  "seniority": "MID",
  "keywords": ["java", "spring boot", "rest api"],
  "location": "Argentina"
}
```

**Respuesta 200:**

```json
{
  "provider": "JOBICY",
  "count": 5,
  "jobs": [
    {
      "id": "12345",
      "title": "Senior Java Developer",
      "company": "TechCorp",
      "location": "Argentina",
      "snippet": "Buscamos desarrollador Java...",
      "salary": "$3000-5000 USD/mes",
      "employmentType": "Full-time",
      "pubDate": "2026-08-10",
      "url": "https://jobicy.com/...",
      "source": "Jobicy",
      "matchedKeywords": ["java", "spring boot"]
    }
  ]
}
```

### Codigos de error

| HTTP | Codigo | Descripcion |
|---|---|---|
| 400 | `INVALID_REQUEST` | Parametros invalidos |
| 400 | `INVALID_CV_CONTENT` | El archivo no parece un CV |
| 400 | `MISSING_REQUEST_DATA` | Faltan campos requeridos |
| 413 | `FILE_TOO_LARGE` | Archivo supera 5 MB |
| 429 | `RATE_LIMIT_EXCEEDED` | Limite de requests por minuto |
| 429 | `AI_QUOTA_EXCEEDED` | Cuota de Gemini agotada |
| 500 | `CONFIGURATION_ERROR` | Error de configuracion del servidor |
| 502 | `AI_INVALID_RESPONSE` | Gemini devolvio una respuesta inesperada |
| 503 | `AI_UNAVAILABLE` | Gemini no disponible temporalmente |
| 504 | `AI_TIMEOUT` | Gemini tardo demasiado |

---

## Algoritmo de scoring

El porcentaje de compatibilidad se calcula de forma **completamente determinista** en Java. Gemini NO calcula porcentajes, solo clasifica requisitos.

### Categorias y pesos

| Categoria | Peso |
|---|---|
| `MANDATORY_TECHNICAL` | 60 |
| `EXPERIENCE_SENIORITY` | 20 |
| `DESIRABLE` | 10 |
| `COMPLEMENTARY` | 10 |

### Factores de estado

| Estado | Factor |
|---|---|
| `MATCH` | 1.0 |
| `PARTIAL` | 0.5 |
| `MISSING` | 0.0 |

### Formula

1. Agrupa los requisitos por categoria.
2. Para cada categoria con al menos un requisito: `ratio = suma(factores) / cantidad`.
3. `puntos_ponderados = suma(ratio * peso_categoria)` (solo categorias presentes).
4. `peso_total = suma(peso_categoria)` (solo categorias presentes).
5. `porcentaje = redondear((puntos_ponderados / peso_total) * 100)`

Las categorias sin requisitos no participan en el calculo, normalizando al conjunto de categorias identificadas por Gemini.

---

## Integracion con Gemini

- **SDK:** `com.google.genai:google-genai:1.63.0`
- **Modelo por defecto:** `gemini-3.6-flash` (configurable via `GEMINI_MODEL`)
- **Salida estructurada:** Usa `responseSchema` de Gemini para garantizar JSON valido.
- **Seed fija:** 42 (para reproducibilidad).
- **Prompt injection defense:** El prompt instruye a Gemini a tratar el CV y la oferta como datos no confiables.
- **Dos modos:**
  - **Texto:** Prompt con texto plano del CV + oferta.
  - **Imagen:** Prompt multimodal con texto + imagen inline (MIME type detectado).
- **Reintentos:** En errores 429, 502, 503 (1-2 intentos con delay configurable).
- **Concurrency:** Limite de requests simultaneas via Semaphore (default: 4).
- **Validacion de respuesta:** Max 100 requisitos, duplicados detectados, listas null normalizadas a vacias.

### Prompt

El prompt esta escrito en espanol (cientos de lineas) e incluye:
- Extraccion bifasica de requisitos (solo del puesto, luego evaluacion contra CV).
- Reglas de prioridad por categoria.
- Manejo de requisitos alternativos ("Java o Kotlin" = un solo requisito).
- Reglas estrictas de status (MATCH/PARTIAL/MISSING).
- Perfil de busqueda laboral derivado principalmente del CV.

---

### Busqueda de empleos

Integracion con la API de [Jobicy](https://jobicy.com/) para ofertas de empleo remoto.

### Flujo

1. El analisis genera un `JobSearchProfile` (rol, seniority, keywords) a partir del CV.
2. El usuario selecciona ubicacion: Argentina / Latinoamerica / Global.
3. Se buscan hasta 100 ofertas de Jobicy (filtradas por industria `engineering`, hardcodeado).
4. Se filtran y rankean localmente:
   - **Geo gate:** Argentina acepta "Argentina", "LATAM", "Anywhere"; LATAM acepta "LATAM", "Anywhere"; Global no filtra.
   - **Seniority mismatch:** Perfiles TRAINEE/JUNIOR excluyen puestos Senior/Lead/Staff.
   - **Relevance gate:** Al menos 1 keyword en titulo o descripcion. Si matchea solo en descripcion, requiere 2+ keywords y tokens significativos en titulo.
   - **Ranking:** Primario por keywords matcheadas (desc), secundario por fecha de publicacion (desc).
5. Se retornan hasta 8 resultados (configurable via `JOB_SEARCH_MAX_RESULTS`).

### Cache

Cache en memoria con TTL configurable (default 60 min, minimo 60 min). Si la API falla, sirve datos stale y extiende el TTL.

---

## Seguridad

### Autenticacion

Toggle via `SECURITY_ENABLED` (default: `false`).

| `SECURITY_ENABLED` | Comportamiento |
|---|---|
| `false` | Sin auth. Todos los endpoints publicos. |
| `true` | HTTP Basic con usuario in-memory (`demo`/`demo-password`). Historial requiere rol `USER`. |

### CORS

Dual configuration (WebMvcConfigurer + SecurityFilterChain) para `/api/**`:

- Origen por defecto: `http://localhost:5173`
- Multi-origen: lista separada por comas
- Methods: GET, POST, DELETE, OPTIONS
- Headers: Authorization, Content-Type, Accept
- Credentials: deshabilitadas
- Max-Age: 3600s

### Datos

- El CV y la oferta se envian a Google Gemini para su analisis.
- Los resultados se guardan en localStorage (no se envian a un servidor externo).
- No se almacenan archivos subidos en el backend (se procesan en memoria y se descartan).
- La H2 local esta en `.gitignore` (carpeta `data/`).

---

## Rate limiting

Filtro `OncePerRequestFilter` aplicado a POST en `/api/analyze`, `/api/analyses` y `/api/jobs/search`.

- **Ventana deslizante:** 1 minuto.
- **Limite:** 10 requests/minuto (configurable).
- **Key por bucket:** Usuarios autenticados: `analysis:<user>` / `jobs:<user>`. Anonimos: `analysis:<CF-Connecting-IP>` / `jobs:<IP>`.
- **Buckets independientes:** Analisis y busqueda tienen contadores separados.
- **Respuesta:** HTTP 429 con `Retry-After: 60` y body `{"code":"RATE_LIMIT_EXCEEDED","message":"..."}`.

---

## Frontend

### Stack

| Tecnologia | Version | Uso |
|---|---|---|
| React | 19.2.8 | UI |
| TypeScript | 7.0.2 | Type safety |
| Vite | 8.2.1 | Build tool |
| Tailwind CSS | 4.1.18 | Utilidades (uso minimo) |
| Vitest | 4.1.10 | Testing |

**Zero dependencies de routing, state management o component libraries.** Todo es vanilla React con hooks custom.

### Paginas

| Ruta | Pagina | Descripcion |
|---|---|---|
| `/` | `HomePage` | Landing page con demo animado y descripcion de funcionalidades |
| `/analizar` | `AnalyzerPage` | Pagina principal: upload CV, descripcion, resultados |
| `/historial` | `HistoryPage` | Lista de analisis anteriores con busqueda/filtros |
| `/historial/:id` | `HistoryDetail` | Detalle de un analisis |

### Componentes (Atomic Design)

**Atoms:** `ScoreRing`, `ThemeToggle`, `BottomNav`, `AppFooter`

**Molecules:** `LoadingScreen`, `AnalysisStepper`, `FileUploadCard`, `InlineFilePreview`, `ErrorState`, `EmptyState`, `AnalysisErrorAlert`, `JobOfferCard`, `AppHeader`, `ConfirmDialog`

**Organisms:** `Results`, `ScoreExplanationPanel`, `RequirementsSection`, `RecommendationList`, `CvOptimizationPanel`, `InterviewQuestionsPanel`, `JobSearchPanel`, `HistoryScreen`, `ComparisonView`

**Templates:** `AnalyzerPage`, `AppErrorBoundary`

### Flujo de analisis

1. Usuario arrastra PDF o selecciona archivo (valida: tipo, 5 MB max).
2. Escribe descripcion del puesto (texto, 5000 chars) o sube imagen (PNG/JPEG/WEBP).
3. Hace clic en "Analizar con IA".
4. `LoadingScreen` muestra progreso animado con 4 pasos.
5. `createAnalysis()` POST a `/api/analyze`, guarda en localStorage.
6. `Results` muestra score animado, desglose, requisitos, recomendaciones, optimizacion, preguntas y busqueda de empleos.

### Historial

- Almacenamiento en `localStorage` (`jobmatch-ai-history`), max 50 registros.
- Busqueda por rol, empresa, nombre de archivo, version.
- Filtros: Todos, >80%, 60-80%, <60%.
- Ordenamiento: fecha (asc/desc), score (asc/desc).
- Agrupacion: Recientes (< 7 dias) / Anteriores.
- Comparacion versionada: detecta analisis previos del mismo rol/puesto.

### Diseno

- **Fuentes:** Inter (body), Hanken Grotesk (headings, scores).
- **Dark mode:** Via `data-theme="dark"` en `<html>`, con transiciones.
- **Responsive:** Mobile-first, breakpoint en 760px.
- **Animaciones:** Score ring con conic-gradient animado, panels con entrada escalonada, loading visual.
- **Accessibility:** `prefers-reduced-motion`, `role="alert"`, `aria-live`, `aria-label`, focus outlines.

---

## Testing

### Backend (22 archivos de test)

```powershell
.\mvnw.cmd test
```

| Capa | Tests | Archivos |
|---|---|---|
| Controllers (MockMvc) | ~21 | `AnalysisControllerTest`, `JobSearchControllerTest` |
| Services | ~65+ | `GeminiServiceTest`, `AnalysisServiceTest`, `JobSearchServiceTest`, `CvContentValidatorTest`, `AnalysisHistoryServiceTest` |
| Scoring | 11 | `MatchScoreCalculatorTest` |
| Security | 16 | `SecurityConfigTest`, `SecurityDisabledConfigTest`, `RateLimitFilterTest`, `RateLimitFilterIntegrationTest` |
| Error handling | 14 | `ApiExceptionHandlerTest` |
| Integration | ~20 | `CorsConfigTest`, `CorsConfigCustomOriginTest`, `JobicyClientTest`, `JoobleClientTest`, `DeploymentConfigurationTest`, `DeploymentSupabaseConfigurationTest`, `ActuatorHealthEndpointTest`, `JobmatchAiApplicationTests` |

### Frontend (15 archivos de test)

```powershell
npm test --prefix frontend
```

| Archivo | Tests | Cubre |
|---|---|---|
| `api.test.ts` | 28 | Llamadas API, normalizacion, errores, historial, mock mode |
| `errorMessages.test.ts` | ~15 | Mapeo de errores a mensajes de usuario |
| `useObjectUrl.test.ts` | ~10 | Lifecycle de blob URLs |
| `format.test.ts` | ~6 | Clasificacion de scores |
| `AnalyzerPage.test.tsx` | ~3 | Render del formulario |
| `Results.test.tsx` | ~3 | Panel de resultados con job search |
| `JobSearchPanel.test.tsx` | ~3 | Panel de busqueda |
| `AnalysisErrorAlert.test.tsx` | ~2 | Alerta de error |
| `InlineFilePreview.test.tsx` | ~5 | Preview de PDF/imagen |
| `JobOfferCard.test.tsx` | ~6 | Tarjeta de oferta laboral |
| `AppFooter.test.tsx` | ~3 | Footer de la aplicacion |
| `FileUploadCard.test.tsx` | ~4 | Componente de carga de archivos |
| `LoadingScreen.test.tsx` | ~4 | Pantalla de carga animada |
| `HomePage.test.tsx` | ~3 | Landing page |
| `routes.test.ts` | ~4 | Enrutamiento custom |

### Validacion completa

```powershell
# Backend
.\mvnw.cmd test

# Frontend
npm run typecheck --prefix frontend
npm test --prefix frontend
npm run build --prefix frontend
npm run audit --prefix frontend
```

---

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`) ejecuta en push a `main` y en PRs:

**Job 1 - backend:** Java 21 + `./mvnw -B test`

**Job 2 - frontend:** Node 20.19.0 + `npm ci` + typecheck + test + audit + build

**Job 3 - container:** `docker build` (requiere que backend y frontend pasen)

---

## Docker

```bash
docker build -t jobmatch-ai .
docker run -p 8080:8080 \
  -e GEMINI_API_KEY=tu_clave \
  jobmatch-ai
```

Multi-stage build:
1. **Build stage:** `eclipse-temurin:21-jdk-jammy` compila con Maven.
2. **Runtime stage:** `eclipse-temurin:21-jre-jammy` ejecuta el JAR como usuario no-root.

---

## Produccion

### Frontend

Desplegado en **Vercel**: `https://jobmatch-ai-ten.vercel.app/`

SPA rewrite configurado en `vercel.json`.

### Backend

- H2 es adecuado para desarrollo y demos. No se recomienda para multiples instancias.
- La autenticacion Basic esta disponible para produccion (`SECURITY_ENABLED=true`).
- El historial se almacena en `localStorage` del navegador; el backend H2 no es utilizado por el frontend actual.

### Variables de entorno para produccion

```env
GEMINI_API_KEY=...
SECURITY_ENABLED=true
DEMO_USERNAME=usuario_produccion
DEMO_PASSWORD=contraseña_segura
CORS_ALLOWED_ORIGINS=https://jobmatch-ai-ten.vercel.app
PORT=8080
```

---

## Estructura del proyecto

```
jobmatch-ai/
├── .github/workflows/ci.yml    # CI/CD pipeline
├── AGENTS.md                    # Instrucciones para agentes de IA
├── frontend/                    # React SPA
│   ├── src/
│   │   ├── components/
│   │   │   ├── atoms/          # ScoreRing, ThemeToggle, BottomNav, AppFooter
│   │   │   ├── molecules/      # LoadingScreen, FileUploadCard, AppHeader, ConfirmDialog, ...
│   │   │   ├── organisms/      # Results, RequirementsSection, JobSearchPanel, ...
│   │   │   └── templates/      # AnalyzerPage, AppErrorBoundary
│   │   ├── pages/              # HomePage, HistoryPage, App.tsx
│   │   ├── services/           # api.ts, errorMessages.ts
│   │   ├── lib/
│   │   │   ├── constants/      # app.ts, brand.ts
│   │   │   ├── helpers/        # analysis.ts, format.ts
│   │   │   ├── hooks/          # useTheme, useObjectUrl, useHistory
│   │   │   ├── mocks/          # analysisMock.ts (modo demo)
│   │   │   ├── storage/        # historyStorage.ts (localStorage)
│   │   │   └── types/          # types.ts
│   │   ├── routes/             # Enrutamiento custom via History API
│   │   └── styles/             # globals.css, theme.css, loading.css, components.css
│   ├── package.json
│   └── vite.config.ts
├── src/main/java/com/codercup/jobmatchai/
│   ├── client/                 # JobicyClient
│   ├── controller/             # AnalysisController, JobSearchController
│   ├── dto/                    # Request/Response DTOs + internal/
│   ├── entity/                 # AnalysisEntity
│   ├── exception/              # Exception hierarchy + ApiExceptionHandler
│   ├── repository/             # AnalysisRepository
│   ├── scoring/                # MatchScoreCalculator, RequirementCategory, RequirementStatus
│   ├── security/               # SecurityConfig, RateLimitFilter, CurrentUserService
│   └── service/                # AnalysisService, GeminiService, JobSearchService, PdfService, ...
├── src/test/java/              # 22 archivos de test
├── pom.xml                     # Spring Boot 4.1.0, Java 21, H2, Gemini SDK
├── Dockerfile                  # Multi-stage build
├── .env.example                # Template de configuracion
└── data/                       # H2 database files (gitignored)
```

---

## Licencia

Proyecto academico - CoderCUP IA 2026
