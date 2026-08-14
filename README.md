# JobMatch AI

Aplicación web que compara un CV con una oferta laboral usando Gemini y calcula un porcentaje determinista de compatibilidad mediante Java.

## Requisitos

- Java 21
- Node.js 20.19+
- Una clave de Gemini

## Configuración local

Crea un archivo `.env` en la raíz del proyecto. Este archivo es local y no debe subirse a Git:

```env
GEMINI_API_KEY=tu_clave
GEMINI_MODEL=gemini-3.5-flash-lite
GEMINI_TIMEOUT_MS=120000
SECURITY_ENABLED=false
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Con `SECURITY_ENABLED=false` la aplicación local no solicita usuario ni contraseña.

El frontend puede configurar la URL del backend en `frontend/.env`:

```env
VITE_API_URL=http://localhost:8080
```

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

La aplicación queda disponible normalmente en `http://localhost:5173`.

## Base de datos local

En desarrollo se utiliza H2 en archivo dentro de `data/`. Esos archivos están ignorados por Git y se conservan localmente para no perder el historial.

## Validar

```powershell
.\mvnw.cmd test
npm run typecheck --prefix frontend
npm test --prefix frontend
npm run build --prefix frontend
npm run audit --prefix frontend
```

## Producción

Por ahora la aplicación utiliza H2 local y no depende de Supabase ni de otro servicio externo de base de datos. El despliegue del backend y del frontend queda pendiente.

H2 es adecuado para desarrollo, demos y una instancia única. No se recomienda para múltiples instancias o producción pública porque el historial depende del archivo local `data/jobmatch`.

## Seguridad

- No subas `.env`, claves, bases H2, builds ni resultados locales.
- La autenticación Basic queda disponible para producción, pero está desactivada localmente.
- Revoca cualquier clave de Gemini que haya sido expuesta.
