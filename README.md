# Patiperro

Plataforma web que conecta **tutores** (dueños de mascotas) con **paseadores** para reservar, pagar y gestionar paseos de perros y gatos en Chile.

---

## Enlaces de documentación

| Documento | Contenido |
|-----------|-----------|
| [docs/guia-arranque.md](docs/guia-arranque.md) | **Pasos para correr la aplicación (10 pasos)** |
| [docs/enlaces.md](docs/enlaces.md) | Tablero Jira y presentación |
| [docs/datos-iniciales-bd.md](docs/datos-iniciales-bd.md) | Catálogos SQL (seeds manuales) |
| [docs/arquitectura-backend.md](docs/arquitectura-backend.md) | Microservicios y arquitectura |

---

## 1. Cómo ejecutar la aplicación

Sigue la guía paso a paso: **[docs/guia-arranque.md](docs/guia-arranque.md)**

---

## 2. Contexto del proyecto

**Problema:** Los tutores necesitan encontrar paseadores confiables, ver disponibilidad real, reservar un horario y pagar de forma segura. Los paseadores necesitan gestionar agenda, recibir solicitudes pagadas y cobrar a través de la plataforma.

**Solución:** Aplicación full-stack con dos perfiles de usuario:

- **Tutor:** busca paseadores en mapa, registra mascotas, solicita paseos, paga con Mercado Pago, chatea durante el paseo y deja reseñas.
- **Paseador:** configura tarifas y agenda, verifica identidad, acepta/rechaza reservas, valida encuentro con PIN, gestiona billetera y retiros.

**Stack:**

| Capa | Tecnología |
|------|------------|
| Frontend | React 19, TypeScript, Vite, React Router, Leaflet, Mercado Pago SDK |
| Backend | Spring Boot 4, Java 21, Spring Data JPA, Spring Cloud Gateway |
| Base de datos | PostgreSQL (una BD por microservicio) |
| Pagos | Mercado Pago (Checkout Pro + Bricks, sandbox) |
| Tiempo real | WebSocket STOMP (inicio de paseo, chat) |

---

## 3. Arquitectura (resumen)

El sistema sigue una **arquitectura de microservicios**. El frontend solo consume el **api-gateway** (`http://localhost:8080`). Cada dominio de negocio tiene su propio servicio y base de datos.

```
Frontend (React :5173)
        │
        ▼
  api-gateway (:8080)
        │
        ├── tutores-service      (:8081)  tutores_db
        ├── paseadores-service   (:8082)  paseadores_db
        ├── mascotas-service     (:8083)  mascotas_db
        ├── agenda-service       (:8084)  agenda_db
        ├── reserva-service      (:8090)  reservas_db   ← orquestador principal
        ├── notification-service (:8086)  notification_db
        ├── pagos-service        (:8087)  pagos_db
        ├── resena-service       (:8088)  resenas_db
        └── chat-service         (:8089)  chat_db
```

El servicio más conectado es **`reserva-service`**: coordina agenda, usuarios, mascotas, pagos y notificaciones. Detalle completo en [docs/arquitectura-backend.md](docs/arquitectura-backend.md).

---

## 4. Requisitos adicionales

### Software necesario

| Herramienta | Versión sugerida | Para qué |
|-------------|------------------|----------|
| [Java JDK](https://adoptium.net/) | 21 | Microservicios Spring Boot |
| [Maven](https://maven.apache.org/) | 3.9+ | Compilar y ejecutar backend (incluye wrapper `./mvnw`) |
| [Node.js](https://nodejs.org/) | 20+ | Frontend (Vite) |
| [PostgreSQL](https://www.postgresql.org/) | 15+ | Bases de datos locales |
| IDE | VS Code / IntelliJ | Launchers en `.vscode/launch.json` |

### Bases de datos — crear antes de arrancar

> **Importante:** Hay que **crear las 9 bases de datos en PostgreSQL antes** de levantar cualquier microservicio. Si no existen, Spring Boot fallará al conectar (`FATAL: database "..." does not exist`).

Orden obligatorio de preparación:

| Paso | Acción | Quién lo hace |
|------|--------|---------------|
| 1 | Crear las 9 bases vacías (`CREATE DATABASE`) | Manual (pgAdmin / DBeaver / `psql`) |
| 2 | Levantar microservicios al menos una vez | Spring Boot + Hibernate |
| 3 | Hibernate crea las **tablas** en cada BD | Automático al arrancar |
| 4 | Ejecutar los **seeds** (`INSERT` de catálogos) | **Manual** — solo cuando las tablas ya existen |

> Los seeds **no se cargan solos**. Hay que copiar y ejecutar cada `INSERT` de [docs/datos-iniciales-bd.md](docs/datos-iniciales-bd.md) a mano en la BD correspondiente. Si corres los seeds antes de que existan las tablas, PostgreSQL devolverá error (`relation "..." does not exist`).

Nombres requeridos (una BD por microservicio):

| Base de datos | Microservicio |
|---------------|--------------|
| `tutores_db` | tutores-service |
| `paseadores_db` | paseadores-service |
| `mascotas_db` | mascotas-service |
| `agenda_db` | agenda-service |
| `reservas_db` | reserva-service |
| `pagos_db` | pagos-service |
| `notification_db` | notification-service |
| `resenas_db` | resena-service |
| `chat_db` | chat-service |

Script en pgAdmin, DBeaver o `psql` (como superusuario, p. ej. `postgres`):

```sql
CREATE DATABASE tutores_db;
CREATE DATABASE paseadores_db;
CREATE DATABASE mascotas_db;
CREATE DATABASE agenda_db;
CREATE DATABASE reservas_db;
CREATE DATABASE pagos_db;
CREATE DATABASE notification_db;
CREATE DATABASE resenas_db;
CREATE DATABASE chat_db;
```

Credenciales por defecto en dev: usuario `postgres`, contraseña `12345` (ver `application.properties` de cada servicio).

### Catálogos iniciales (seeds — carga manual)

Los seeds son datos de catálogo (`estado_reserva`, `especie`, `tamano`, etc.) que el proyecto **no inserta automáticamente**. Debes ejecutarlos **manualmente** en pgAdmin, DBeaver o `psql`, **después** de que Hibernate haya creado las tablas (es decir, tras el primer arranque de los microservicios).

Consulta las queries en [docs/datos-iniciales-bd.md](docs/datos-iniciales-bd.md). Sin ellos, fallan flujos de mascotas, agenda, reservas y pagos bancarios.

---

## 5. Credenciales de prueba — Mercado Pago (sandbox)

La aplicación usa credenciales de **cuenta de prueba** de Mercado Pago Chile.

> Sigue [docs/guia-arranque.md](docs/guia-arranque.md) con túneles Cloudflare + ngrok activos.

### Credenciales de la aplicación

| Uso | Valor | Dónde se configura |
|-----|-------|-------------------|
| **Public Key** (frontend, Bricks/Wallet) | `APP_USR-76e22f21-12a1-418c-b02c-c03c8f25d7d9` | `frontend/.../src/config/mercadopago.ts` o env `VITE_MERCADOPAGO_PUBLIC_KEY` |
| **Access Token** (backend, API MP) | `APP_USR-6514616507944830-050513-11d4559886689fd290f65bee57f10ce3-3379090418` | `backend/pagos-service/.../application.properties` o env `MERCADOPAGO_ACCESS_TOKEN` |
| Modo sandbox | Activado (`use-sandbox=true`) | `pagos-service` dev |

### Tarjetas de prueba (Checkout)

Usar datos de [usuarios de prueba de Mercado Pago](https://www.mercadopago.cl/developers/es/docs/your-integrations/test/accounts). Regla importante: el **comprador de prueba debe ser distinto** al vendedor asociado al Access Token.

Tarjetas de prueba estándar (Chile):

| Campo | Valor de ejemplo |
|-------|------------------|
| Número | `5031 7557 3453 0604` (Mastercard) |
| CVV | `123` |
| Vencimiento | `11/30` |
| Titular | `APRO` → pago aprobado; `OTHE` → rechazado |

### Rutas útiles de pago en la app

| Ruta | Descripción |
|------|-------------|
| Flujo tutor normal | Solicitud de paseo → pago embebido (Bricks) |
| `/labs/checkout-pro` | Sandbox aislado de Checkout Pro (solo desarrollo) |

---

## 6. Microservicios y entidades

Cada servicio persiste en su propia base PostgreSQL. Entidades JPA principales:

| Microservicio | BD | Entidades principales |
|--------------|-----|------------------------|
| **tutores-service** | `tutores_db` | `Tutor`, `Direccion`, `Foto` |
| **paseadores-service** | `paseadores_db` | `Paseador`, `Direccion`, `Foto`, `Configuracion`, `TarifaPaseador`, `Tamano` |
| **mascotas-service** | `mascotas_db` | `Mascota`, `Foto`, `Especie`, `Raza`, `Tamano` |
| **agenda-service** | `agenda_db` | `AgendaBloque`, `AgendaBloqueoDia`, `EstadoBloque`, `DiaSemana` |
| **reserva-service** | `reservas_db` | `Reserva`, `EstadoReserva` |
| **pagos-service** | `pagos_db` | `Transaccion`, `PagoExterno`, `Billetera`, `BilleteraReservaTracking`, `ComprobantePago`, `Cuenta`, `Banco`, `TipoCuenta`, `RetiroFondo`, `RecaudacionPlataformaLog` |
| **notification-service** | `notification_db` | `PlantillaCorreo`, `LogEnvio`, `PushSuscripcion` |
| **resena-service** | `resenas_db` | `Resena` |
| **chat-service** | `chat_db` | `Conversacion`, `Mensaje`, `EstadoChat`, `EstadoMensaje`, `TipoMensaje` |

Estados de reserva relevantes: `SOLICITADA`, `PENDIENTE_PAGO`, `PAGADA`, `ACEPTADA`, `EN CURSO`, `FINALIZADA`, `CANCELADA`, `EXPIRADA`.

---

## 7. Estructura del repositorio

```
patiperro-project/
├── backend/           # Microservicios Spring Boot
│   ├── api-gateway/
│   ├── tutores-service/
│   ├── paseadores-service/
│   ├── mascotas-service/
│   ├── agenda-service/
│   ├── reserva-service/
│   ├── pagos-service/
│   ├── notification-service/
│   ├── resena-service/
│   └── chat-service/
├── frontend/
│   └── patiperro-project/   # App React (Vite)
├── docs/              # Documentación para evaluadores
└── test-assets/       # Fixtures manuales QA (>5 MB)
```

---

## 8. Checklist para evaluadores

1. [docs/enlaces.md](docs/enlaces.md) (Jira + presentación).
2. [docs/guia-arranque.md](docs/guia-arranque.md) — **10 pasos de arranque**.
3. BD + seeds: [sección 4](#4-requisitos-adicionales) y [datos-iniciales-bd.md](docs/datos-iniciales-bd.md).
4. Probar pago sandbox.

---

## 9. Notas operativas

- Tras cambios de **UI**, basta refrescar Vite; tras cambios de **DTOs Java**, reiniciar el microservicio afectado.
- El puerto de `reserva-service` es **8090** (no 8085); debe coincidir con `api-gateway` y `pagos-service`.
- Geocodificación es opcional: si Nominatim falla, la dirección se guarda sin coordenadas; el mapa requiere lat/lng del paseador.

---

**Equipo Patiperro** · Entrega académica · Junio 2026
