# Arquitectura del backend

Detalle ampliado del backend. Para una vista rápida, ver el [README principal](../README.md).

---

## Vista general

Patiperro usa **microservicios Spring Boot 4** (Java 21) con **PostgreSQL** (una BD por servicio en dev). El frontend React habla solo con el **api-gateway** (`:8080`); los servicios se comunican entre sí por HTTP (`RestClient` / `FeignClient`) con cabecera interna `X-Patiperro-Interno-Secret` y JWT compartido para rutas de usuario.


## Rol de cada microservicio

| Servicio | Responsabilidad principal |
|----------|---------------------------|
| **api-gateway** | Entrada pública; enruta `/api/**` al servicio correcto; CORS; no expone rutas `*/interno/**`. |
| **tutores-service** | Registro, login, perfil y fotos del tutor (dueño de mascota). |
| **paseadores-service** | Registro, perfil, configuración, tarifas, búsqueda cercana, verificación de identidad. |
| **mascotas-service** | CRUD mascotas, fotos, catálogos especie/raza/tamaño. |
| **agenda-service** | Bloques horarios, bloqueos por día, disponibilidad para reservas. |
| **reserva-service** | Orquestador central: reservas, estados, PIN de encuentro, checkout, eventos STOMP. |
| **pagos-service** | Mercado Pago (Checkout Pro / Bricks), webhooks, billetera, retiros, comprobantes. |
| **notification-service** | Plantillas y envío de correo (Brevo), logs, eventos internos. |
| **resena-service** | Reseñas post-paseo y promedio de reputación. |
| **chat-service** | Mensajería y fotos del paseo (REST + WebSocket STOMP). |

---

## Flujo de negocio principal

1. Tutor se autentica y busca paseadores (`paseadores-service` + `agenda-service` si filtra disponibilidad).
2. Tutor registra mascotas (`mascotas-service`).
3. Tutor crea reserva (`reserva-service`), que valida y enriquece datos vía otros servicios.
4. Tutor paga vía `pagos-service` → Mercado Pago; webhook actualiza estado en `reserva-service`.
5. Paseador acepta/rechaza; al iniciar paseo se valida PIN y se emiten eventos en tiempo real.
6. Tras finalizar, tutor deja reseña (`resena-service`).

---

## Patrones de diseño

| Patrón | Dónde se ve |
|--------|-------------|
| **Arquitectura por capas** | Controller → Service → Repository → Entity en cada servicio. |
| **Repository** | `JpaRepository` + queries declarativas. |
| **DTO** | Request/Response desacoplados de entidades JPA; DTOs de integración entre servicios. |
| **Adapter / Gateway interno** | Clientes HTTP dedicados (`AgendaIntegracionClient`, etc.). |
| **Event-driven** | Publicación de eventos al confirmar inicio de paseo (side effects desacoplados). |
| **Máquina de estados** | Transiciones de reserva centralizadas por reglas de negocio. |

---

## Notas técnicas

- **`reserva-service`** es el coordinador con más dependencias.
- **`pagos-service`** integra Mercado Pago y gestiona billetera (saldo retenido / verificación / disponible).
- Geocodificación (Nominatim) es *best-effort*: la dirección puede guardarse sin lat/lng si falla el servicio externo.
- Para mapa de tutor, el paseador necesita coordenadas en su dirección.
