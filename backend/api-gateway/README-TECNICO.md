# API Gateway - README Tecnico

## Objetivo
Centralizar el acceso desde frontend a los microservicios.

## Estado actual (fase inicial)
- Puerto: `8080`
- Ruta configurada:
  - `Path=/api/**` -> `http://localhost:8081` (`tutores-service`)

Esto significa que el frontend puede llamar al gateway en vez de llamar directo al microservicio.

Ejemplo:
- Frontend llama: `http://localhost:8080/api/auth/login`
- Gateway reenvia a: `http://localhost:8081/api/auth/login`

## Seguridad actual
- Configuracion abierta (`permitAll`) para no romper pruebas de desarrollo.
- `httpBasic` y `formLogin` deshabilitados.

## Proxima etapa (JWT)
1. `tutores-service` emite JWT en login (cookie `access_token`).
2. El mismo token puede enviarse tambien como `Authorization: Bearer <token>` en pruebas.
3. Gateway valida JWT en rutas privadas.
4. Se deja publico solo `/api/auth/**`.
