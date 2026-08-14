# JobMatch AI

Aplicación web que compara un CV con una oferta laboral usando Gemini y devuelve un análisis determinista de compatibilidad.

## Requisitos

- Java 21
- Node.js 20+
- Una clave de Gemini

## Configuración local

Crea un `.env` en la raíz del proyecto:

```env
GEMINI_API_KEY=tu_clave
GEMINI_MODEL=gemini-3.6-flash
DEMO_USERNAME=demo
DEMO_PASSWORD=cambia-esta-clave
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

La aplicación usa autenticación HTTP Basic. El frontend toma las credenciales de `frontend/.env`:

```env
VITE_API_URL=http://localhost:8080
VITE_API_USERNAME=demo
VITE_API_PASSWORD=cambia-esta-clave
```

Las credenciales demo son para desarrollo local. Para producción se debe reemplazar este mecanismo por un proveedor de identidad y usuarios persistidos.

## Ejecutar

Backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
npm install --prefix frontend
npm run dev --prefix frontend
```

## Validar

```powershell
.\mvnw.cmd test
npm run build --prefix frontend
```

## Producción

### Supabase

1. En Supabase abre `Connect` y elige `Session pooler` para obtener una conexión PostgreSQL.
2. Convierte la URL a formato JDBC: `postgresql://...` pasa a `jdbc:postgresql://...`.
3. Configura estas variables en el entorno del backend:

```env
SPRING_PROFILES_ACTIVE=supabase
SUPABASE_DB_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
SUPABASE_DB_USERNAME=postgres.<project-ref>
SUPABASE_DB_PASSWORD=tu_password_de_supabase
GEMINI_API_KEY=tu_clave
DEMO_PASSWORD=una_clave_larga
CORS_ALLOWED_ORIGINS=https://tu-frontend.example
```

El perfil `supabase` fuerza SSL, limita el pool de conexiones, usa UTC, ejecuta Flyway y valida el esquema con Hibernate. No uses el connection string directo de Supabase sin convertirlo a JDBC.

No versiones archivos de `data/`, claves, builds ni resultados locales. El contenedor espera recibir el puerto mediante `PORT`.
