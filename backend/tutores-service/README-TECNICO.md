# Tutores Service - README Tecnico

## Objetivo
Este microservicio gestiona autenticacion basica de tutores y sus datos principales.

## Endpoints actuales
- `POST /api/auth/register`: registra un tutor nuevo.
- `POST /api/auth/login`: autentica tutor por `correo` + `contrasena`.
- `GET /api/tutores/{id}`: retorna datos de tutor (sin contrasena).
- `GET /api/tutores/{tutorId}/fotos`: lista fotos de un tutor.
- `POST /api/tutores/{tutorId}/fotos`: crea foto para un tutor.
- `DELETE /api/tutores/{tutorId}/fotos/{id}`: elimina foto.

## Modelo de datos
- `Tutor` (tabla `tutor`): datos personales, credenciales y relacion con direccion/fotos.
- `Direccion` (tabla `direccion`): direccion del tutor.
- `Foto` (tabla `foto`): fotos asociadas a tutor.

Relaciones:
- `Tutor` -> `Direccion`: 1:1
- `Tutor` -> `Foto`: 1:N

## Flujo de autenticacion actual (sin JWT)
1. Frontend llama a `POST /api/auth/login` con correo y contrasena.
2. `AuthController` delega en `AuthService`.
3. `AuthService` busca tutor por correo y compara contrasena.
4. Si es valido, responde mensaje de login exitoso.
5. Si es invalido, responde error de credenciales.

## Flujo objetivo con API Gateway + JWT
1. Frontend llama a `POST /api/auth/login` en el Gateway.
2. Gateway enruta a `tutores-service` (puerto `8081`).
3. `tutores-service` valida credenciales y emite JWT en cookie `access_token`.
4. Frontend guarda token y lo envia como `Authorization: Bearer <token>`.
5. Gateway valida JWT en rutas privadas antes de enrutar.
6. (Recomendado produccion) cada microservicio tambien valida JWT.

## Rutas publicas vs privadas (objetivo)
- Publicas: `/api/auth/**`
- Privadas: resto de rutas de negocio (`/api/tutores/**`, etc.)

## Notas para el equipo
- Se recomienda hash de contrasenas (BCrypt) antes de pasar a produccion.
- Se recomienda migraciones SQL (Flyway/Liquibase) para versionar schema.
- Para desarrollo, usar `spring.jpa.hibernate.ddl-auto=update`.
