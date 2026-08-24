# 🚀 JobMatch AI

### Analizá tu perfil. Descubrí oportunidades. Explorá tu futuro profesional.

JobMatch AI es una plataforma web impulsada por Inteligencia Artificial que analiza la compatibilidad entre un CV y una oferta laboral, identifica fortalezas y brechas del perfil y genera recomendaciones concretas para mejorar una candidatura.

Además, incorpora **Career Multiverse**, una experiencia de exploración profesional que propone caminos posibles de evolución laboral a partir del perfil detectado, las habilidades actuales y señales del mercado.

> Proyecto desarrollado para la CoderCup AI 2026 de Coderhouse.

---

##  Demo

| Recurso | Link |
|---|---|
| Aplicación | https://jobmatch-ai-ten.vercel.app/ |


---

##  El problema

Buscar trabajo no es solo enviar CVs. Muchas veces es difícil entender si un perfil encaja realmente con una oferta, qué requisitos se cumplen, qué habilidades faltan, si el seniority solicitado es compatible y qué conviene mejorar antes de postularse.

También suele faltar una mirada más amplia: qué otros roles podrían tener sentido, qué habilidades abren nuevas oportunidades y hacia dónde podría evolucionar una carrera.

**JobMatch AI convierte esa comparación en información estructurada, explicable y accionable.**

---

##  Funcionalidades principales

###  Análisis de CV + oferta

Permite subir un CV en PDF y compararlo contra una oferta laboral ingresada como texto o como imagen.

###  Compatibilidad

Devuelve un porcentaje de compatibilidad y un desglose por categorías: requisitos técnicos obligatorios, experiencia/seniority, deseables y complementarios.

###  Evaluación de requisitos

Extrae requisitos de la oferta y los clasifica como:

| Estado | Significado |
|---|---|
| `MATCH` | El CV demuestra evidencia suficiente. |
| `PARTIAL` | El CV cubre una parte real del requisito. |
| `MISSING` | No hay evidencia suficiente en el CV. |

###  Requisitos críticos

Detecta requisitos marcados como críticos por la oferta. Si faltan o están parcialmente cubiertos, pueden limitar el score final.

###  Brecha de experiencia

Identifica gaps de seniority o experiencia profesional cuando la oferta exige años, nivel o experiencia laboral que el CV no demuestra completamente.

###  Recomendaciones

Genera acciones concretas para fortalecer la candidatura sin sugerir exagerar ni inventar habilidades.

###  Optimización del CV

El frontend muestra sugerencias orientadas a comunicar mejor el perfil, reforzar evidencia y priorizar requisitos relevantes.

###  Preparación para entrevistas

Genera preguntas relacionadas con la oferta, las tecnologías clave y las brechas detectadas.

###  Exploración de oportunidades

Crea un `JobSearchProfile` con rol, seniority y keywords para buscar ofertas remotas relevantes.

###  Career Multiverse

Career Multiverse responde una pregunta distinta al análisis de compatibilidad:

**“¿Hacia dónde podría evolucionar profesionalmente?”**

Propone tres caminos:

| Camino | Enfoque |
|---|---|
| Natural | Evolución cercana al perfil actual. |
| Expansión | Camino adyacente que aprovecha habilidades existentes y suma nuevas capacidades. |
| Alternativo | Dirección profesional distinta, pero razonable desde el perfil detectado. |

Cada camino puede incluir compatibilidad con el mercado, habilidades relacionadas, fortalezas, aspectos a desarrollar, roadmap de próximos pasos y un desafío de portfolio.

---

#  ¿Cómo funciona?

```text
CV en PDF + Oferta laboral
        ↓
Extracción de contenido
        ↓
Validación
        ↓
Google Gemini
        ↓
Extracción / clasificación de requisitos
        ↓
Scoring determinístico en Java
        ↓
Resultados explicables
        ↓
Career Multiverse
```

---

##  Arquitectura

```text
Frontend React SPA
  ├─ AnalyzerPage
  ├─ Results
  ├─ HistoryPage / HistoryDetail
  ├─ CareerMultiversePage
  └─ localStorage: jobmatch-ai-history
             │
             │ REST /api/*
             ▼
Backend Spring Boot
  ├─ AnalysisController
  │   └─ AnalysisService
  │       ├─ PdfService
  │       ├─ GeminiService
  │       ├─ MatchScoreCalculator
  │       └─ SkillNormalizer
  ├─ JobSearchController
  │   └─ JobSearchService ── JobicyClient
  ├─ CareerMultiverseController
  │   ├─ CareerMarketService ── JobicyClient
  │   └─ CareerMultiverseService
  │       ├─ GeminiCareerPathService
  │       └─ JobicyClient
  ├─ RateLimitFilter
  ├─ SecurityConfig
  └─ H2 / JPA: AnalysisEntity
```

Puntos importantes:

- El frontend es una SPA React sin router externo; usa History API y rutas custom.
- El historial visible en la aplicación se guarda en `localStorage` con un máximo de 50 análisis.
- El backend también expone endpoints CRUD de análisis con H2, aunque el frontend actual no depende de ellos por defecto.
- El scoring final lo calcula Java. Gemini interpreta requisitos, pero no decide el porcentaje.

---

#  Scoring determinístico

> Gemini no decide el porcentaje final de compatibilidad.

Gemini interpreta la oferta y el CV, extrae requisitos estructurados y clasifica cada requisito. Después, `MatchScoreCalculator` calcula el porcentaje final de forma determinística en Java.


#  Integración con Google Gemini

El backend usa `com.google.genai:google-genai:1.63.0` y el modelo configurado por defecto es:

```env
GEMINI_MODEL=gemini-3.6-flash
```

Gemini se utiliza para:

- extraer requisitos de la oferta;
- clasificar cada requisito por categoría, criticidad y estado;
- devolver evidencia textual;
- detectar seniority y brechas de experiencia;
- generar recomendaciones;
- generar preguntas de entrevista;
- construir el perfil de búsqueda laboral;
- proponer caminos de Career Multiverse.

La integración usa `responseSchema` para pedir JSON estructurado, `responseMimeType=application/json`, seed fija `42`, timeout configurable, reintentos para errores transitorios y límite de concurrencia con `Semaphore`.

Después de recibir la respuesta, el backend valida y normaliza:

- requisitos obligatorios;
- categorías, criticidad y estados válidos;
- duplicados;
- consistencia entre `matchingSkills`, `missingSkills` y requisitos;
- mínimos y máximos de recomendaciones, preguntas y keywords;
- `JobSearchProfile`.

---

#  Defensas del análisis

### Prompt injection

El prompt instruye a Gemini a tratar el CV, la oferta y la imagen como datos no confiables. Cualquier instrucción dentro de esos documentos que intente cambiar reglas, schema, score o formato debe ser ignorada.

### Datos sensibles

El análisis no debe usar edad, género, nacionalidad, raza, religión, discapacidad, orientación sexual, estado civil, dirección, salud, foto u otros atributos sensibles para evaluar compatibilidad, seniority, recomendaciones o búsqueda laboral.

### Experiencia profesional

El sistema diferencia conocimiento, proyectos personales, proyectos académicos, prácticas, freelance y experiencia profesional. Un proyecto académico puede demostrar conocimiento, pero no equivale automáticamente a años de experiencia profesional.



#  Búsqueda de oportunidades

El análisis genera un `JobSearchProfile` basado principalmente en el CV:

| Campo | Descripción |
|---|---|
| `role` | Rol objetivo breve y buscable. |
| `seniority` | `TRAINEE`, `JUNIOR`, `MID`, `SENIOR` o `UNSPECIFIED`. |
| `keywords` | Entre 3 y 6 habilidades demostradas por el CV. |

La búsqueda usa la API de [Jobicy](https://jobicy.com/) para obtener ofertas remotas y luego filtra/rankea localmente:

- búsqueda en industria `engineering`;
- filtro por ubicación: Argentina, LATAM o Global;
- exclusión de roles Senior/Lead/Staff para perfiles Trainee o Junior;
- relevancia por keywords en título o descripción;
- ordenamiento por cantidad de keywords coincidentes y fecha;
- máximo de resultados configurable con `JOB_SEARCH_MAX_RESULTS` (default: 8);
- cache en memoria con TTL configurable.

---

#  Historial

El historial principal del frontend se guarda en `localStorage` bajo la key:

```text
jobmatch-ai-history
```

Permite:

- guardar análisis anteriores;
- buscar por rol, empresa, archivo o versión de CV;
- filtrar por rangos de score;
- ordenar por fecha o score;
- reabrir resultados;
- comparar versiones de análisis del mismo rol/puesto;
- conservar hasta 50 registros locales.

El backend también tiene endpoints `/api/analyses` respaldados por H2, pero el flujo actual del frontend persiste localmente.

---

#  Tecnologías

### Backend

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje backend |
| Spring Boot 4.1.0 | Framework principal |
| Maven Wrapper | Build y ejecución |
| Google Gemini API | Análisis con IA |
| PDFBox 3.0.8 | Extracción de texto desde PDF |
| Jackson | JSON y validación de respuestas |
| Spring Security | HTTP Basic opcional |
| Spring Data JPA | Persistencia de análisis backend |
| H2 | Base local por archivo |
| JUnit 5 | Testing backend |
| Mockito | Mocks en tests |

### Frontend

| Tecnología | Uso |
|---|---|
| React 19.2.8 | UI |
| TypeScript 7.0.2 | Tipado |
| Vite 8.2.1 | Build tool |
| Vitest 4.1.10 | Tests |
| CSS modularizado en `frontend/src/styles` | Estilos de la SPA |

El frontend no usa React Router, librerías externas de estado ni librerías de componentes.

### Infraestructura

| Tecnología | Uso |
|---|---|
| Docker | Imagen del backend |
| GitHub Actions | CI |
| Vercel | Deploy del frontend |
| H2 file database | Persistencia local/backend |

No hay configuración de Render o Railway versionada en el repositorio.

---

#  Requisitos para desarrollo

| Requisito | Versión / detalle |
|---|---|
| Java | 21 |
| Node.js | 20.19.0 o superior |
| Maven | Incluido vía `mvnw` / `mvnw.cmd` |
| npm | Incluido con Node |
| Gemini API Key | Obligatoria para análisis real |

---

#  Ejecución local

Clonar el repositorio:

```bash
git clone https://github.com/MateoBeccan/jobmatch-ai.git
cd jobmatch-ai
```

Crear `.env` en la raíz:

```env
GEMINI_API_KEY=tu_clave_de_gemini
GEMINI_MODEL=gemini-3.6-flash
CORS_ALLOWED_ORIGINS=http://localhost:5173
DATABASE_URL=jdbc:h2:file:./data/jobmatch;AUTO_SERVER=TRUE
DATABASE_USERNAME=sa
DATABASE_PASSWORD=
JPA_DDL_AUTO=update
```

Ejecutar backend en Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Ejecutar backend en Linux/macOS:

```bash
./mvnw spring-boot:run
```

Ejecutar frontend:

```bash
npm install --prefix frontend
npm run dev --prefix frontend
```

URLs locales:

| Servicio | URL |
|---|---|
| Backend | http://localhost:8080 |
| Frontend | http://localhost:5173 |
| Health check | http://localhost:8080/actuator/health |

## Frontend

Comandos:

```bash
npm run typecheck --prefix frontend
npm test --prefix frontend
npm run build --prefix frontend
```

El repositorio tiene 18 archivos de test frontend con cobertura sobre:

- `Results`;
- `History`;
- API frontend;
- errores de usuario;
- carga de archivos;
- preview de PDF/imagen;
- búsqueda de empleos;
- Career Multiverse;
- rutas custom;
- componentes principales.





#  Build

Backend:

```bash
./mvnw clean package
```

Tests backend:

```bash
./mvnw test
```

Frontend:

```bash
npm run build --prefix frontend
npm test --prefix frontend
npm run typecheck --prefix frontend
```

---

#  Docker

El repositorio incluye `Dockerfile` multi-stage:

```bash
docker build -t jobmatch-ai .
docker run -p 8080:8080 -e GEMINI_API_KEY=tu_clave jobmatch-ai
```

La imagen compila con `eclipse-temurin:21-jdk-jammy` y ejecuta el JAR con `eclipse-temurin:21-jre-jammy` usando un usuario no-root.

---

#  Estructura general

```text
jobmatch-ai/
├── .github/workflows/ci.yml
├── Dockerfile
├── README.md
├── pom.xml
├── src/
│   ├── main/java/com/codercup/jobmatchai/
│   │   ├── client/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── repository/
│   │   ├── scoring/
│   │   ├── security/
│   │   └── service/
│   ├── main/resources/
│   └── test/
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── lib/
│   │   ├── pages/
│   │   ├── routes/
│   │   ├── services/
│   │   └── styles/
│   ├── package.json
│   └── vercel.json
└── data/
```

---

# 👥 Equipo

## Mateo Beccan

Desarrollo de Software<br>
GitHub: https://github.com/MateoBeccan

## Francisco Lorenzo

Desarrollo de Software<br>
GitHub: https://github.com/franLorenzo28

El proyecto fue desarrollado de forma colaborativa e iterativa, dividiendo responsabilidades y utilizando Git y GitHub con ramas, commits, Pull Requests, CI y pruebas automatizadas antes de integrar cambios a main.

---

#  CoderCup AI 2026

JobMatch AI fue desarrollado para la CoderCup AI de Coderhouse.

El objetivo fue aplicar IA a un problema real: comparar un perfil profesional con una oferta laboral, explicar la compatibilidad de forma transparente y ayudar a convertir esa información en próximos pasos concretos.

La idea central es usar IA no solo para automatizar una evaluación, sino para **entender mejor nuestro perfil profesional y tomar mejores decisiones al buscar trabajo**.

---

#  Próximos pasos

- ampliar Career Multiverse;
- sumar nuevas fuentes de empleos;
- mejorar recomendaciones accionables;
- incorporar métricas de evolución del perfil;
- profundizar la comparación histórica;
- ampliar la suite de regresiones.

---



