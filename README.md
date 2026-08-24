#  JobMatch AI

### Analizá tu perfil. Descubrí oportunidades. Explorá tu futuro profesional.

**JobMatch AI** es una plataforma web impulsada por Inteligencia Artificial que analiza la compatibilidad entre un CV y una oferta laboral, identifica fortalezas y brechas del perfil y genera recomendaciones concretas para mejorar una candidatura.

Además, incorpora **Career Multiverse**, una experiencia de exploración profesional que propone posibles caminos de evolución laboral a partir del perfil, las habilidades actuales y las oportunidades del mercado.

> Proyecto desarrollado para la **CoderCup AI 2026 de Coderhouse**.

---

##  Demo

| Recurso | Link |
|---|---|
|  Aplicación | https://jobmatch-ai-ten.vercel.app/ |


---

##  El problema

Buscar trabajo no es solamente enviar CVs. Muchas veces es difícil entender si nuestro perfil realmente encaja con una oferta, qué requisitos cumplimos, qué habilidades nos faltan o si nuestra experiencia coincide con el nivel solicitado.

También suele faltar una mirada más amplia: **qué otros roles podrían tener sentido para nuestro perfil y hacia dónde podríamos evolucionar profesionalmente**.

**JobMatch AI convierte esa comparación en información estructurada, explicable y accionable.**

---

##  Funcionalidades principales

###  Análisis de CV + oferta

Permite subir un **CV en PDF** y compararlo con una oferta laboral ingresada mediante **texto o imagen**.

###  Compatibilidad

Genera un porcentaje de compatibilidad acompañado de un desglose que permite entender cómo se relaciona el perfil con los requisitos de la oferta.

###  Skills y requisitos

Identifica requisitos cumplidos, parciales y faltantes, además de detectar posibles brechas técnicas o de experiencia.

###  Recomendaciones

Genera acciones concretas para fortalecer la candidatura y comunicar mejor las habilidades relevantes.

### Optimización del CV

Sugiere mejoras para presentar con mayor claridad la experiencia, proyectos y conocimientos relacionados con la posición.

###  Preparación para entrevistas

Genera posibles preguntas de entrevista relacionadas con la oferta, las tecnologías principales y las brechas detectadas.

###  Exploración de oportunidades

A partir del perfil analizado, permite descubrir oportunidades laborales relacionadas con el rol, seniority y habilidades del usuario.

---

##  Career Multiverse

Career Multiverse amplía el análisis inicial y busca responder una pregunta diferente:

> **¿Hacia dónde podría evolucionar profesionalmente?**

A partir del perfil detectado, JobMatch AI propone tres posibles caminos:

| Camino | Enfoque |
|---|---|
|  **Natural** | Evolución cercana al perfil profesional actual. |
|  **Expansión** | Camino adyacente que aprovecha habilidades existentes e incorpora nuevas capacidades. |
|  **Alternativo** | Dirección diferente, pero relacionada y alcanzable desde el perfil actual. |

Cada camino permite conocer:

- compatibilidad con ese perfil profesional;
- habilidades relacionadas;
- fortalezas actuales;
- aspectos a desarrollar;
- próximos pasos sugeridos.

De esta forma, JobMatch AI no solamente busca responder:

> **“¿Soy compatible con esta oferta?”**

sino también:

> **“¿Qué otros caminos profesionales podría explorar?”**

---

##  ¿Cómo funciona?

```text
CV en PDF + Oferta laboral
          │
          ▼
 Extracción de contenido
          │
          ▼
       Validación
          │
          ▼
     Google Gemini
          │
          ▼
 Análisis de requisitos
          │
          ▼
Scoring determinístico
       en Java
          │
          ▼
Resultados explicables
          │
          ▼
  Career Multiverse
```

---

##  Arquitectura

```text
┌─────────────────────────────────────────────┐
│              Frontend - React               │
│                                             │
│  Análisis → Resultados → Historial          │
│                 ↓                           │
│          Career Multiverse                  │
└───────────────────┬─────────────────────────┘
                    │
                    │ REST API
                    ▼
┌─────────────────────────────────────────────┐
│          Backend - Spring Boot              │
│                                             │
│  AnalysisService                            │
│      ├── PdfService                         │
│      ├── GeminiService                      │
│      └── MatchScoreCalculator               │
│                                             │
│  Job Search                                 │
│      └── Jobicy API                         │
│                                             │
│  Career Multiverse                          │
│      └── Gemini + señales del mercado       │
└─────────────────────────────────────────────┘
```

El frontend funciona como una SPA desarrollada con **React y TypeScript**, mientras que el backend concentra el procesamiento, las reglas de análisis y la integración con servicios externos mediante **Java y Spring Boot**.

---

##  Inteligencia Artificial y scoring

JobMatch AI utiliza **Google Gemini** para interpretar el CV y la oferta laboral, identificar requisitos, analizar fortalezas y brechas, generar recomendaciones y construir información para la exploración profesional.

Una decisión importante del proyecto es que:

> **Gemini no decide directamente el porcentaje final de compatibilidad.**

La IA interpreta la información y devuelve datos estructurados. Posteriormente, el backend utiliza un **sistema de scoring determinístico desarrollado en Java** para calcular el resultado final.

Esto permite obtener resultados más consistentes y explicables, especialmente cuando existen requisitos importantes de experiencia, seniority o conocimientos técnicos.

---

##  Búsqueda de oportunidades

A partir del perfil detectado, JobMatch AI permite explorar oportunidades laborales remotas relacionadas con el **rol, seniority y habilidades** del usuario.

La búsqueda se integra con **Jobicy** y prioriza ofertas relevantes para el perfil profesional analizado.

---

##  Historial

La aplicación permite conservar análisis anteriores para volver a consultar sus resultados.

Desde el historial es posible buscar, filtrar y ordenar análisis, facilitando la comparación de diferentes oportunidades y versiones del perfil.

---

##  Tecnologías

### Backend

- Java 21
- Spring Boot 4.1
- Google Gemini API
- Apache PDFBox
- Spring Data JPA
- H2
- Maven

### Frontend

- React
- TypeScript
- Vite
- CSS

### Testing e infraestructura

- JUnit
- Mockito
- Vitest
- Testing Library
- Docker
- GitHub Actions
- Vercel

---

##  Ejecución local

### 1. Clonar el repositorio

```bash
git clone https://github.com/MateoBeccan/jobmatch-ai.git
cd jobmatch-ai
```

### 2. Configurar Gemini

Crear un archivo `.env` en la raíz del proyecto:

```env
GEMINI_API_KEY=tu_clave_de_gemini
GEMINI_MODEL=gemini-3.6-flash
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### 3. Ejecutar el backend

**Windows**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS**

```bash
./mvnw spring-boot:run
```

Backend disponible en:

```text
http://localhost:8080
```

### 4. Ejecutar el frontend

```bash
npm install --prefix frontend
npm run dev --prefix frontend
```

Frontend disponible en:

```text
http://localhost:5173
```

---

##  Equipo

### Mateo Beccan

Desarrollo de Software  
GitHub: https://github.com/MateoBeccan

### Francisco Lorenzo

Desarrollo de Software  
GitHub: https://github.com/franLorenzo28

JobMatch AI fue desarrollado de forma colaborativa e iterativa, dividiendo responsabilidades y utilizando **Git y GitHub con ramas, commits, Pull Requests, CI y pruebas automatizadas** antes de integrar los cambios a `main`.

---

##  CoderCup AI 2026

JobMatch AI fue desarrollado para la **CoderCup AI de Coderhouse**, con el desafío de aplicar Inteligencia Artificial a un problema real y transformar una idea en un producto funcional.

Nuestro objetivo fue utilizar IA no solamente para automatizar una comparación entre un CV y una oferta, sino para transformar esa información en herramientas que ayuden a:

- entender mejor el perfil profesional;
- identificar fortalezas y oportunidades de mejora;
- prepararse para una postulación;
- descubrir oportunidades;
- explorar posibles caminos de crecimiento profesional.

---
