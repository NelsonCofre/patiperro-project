# Chat API — Fotos del paseo (contrato front)

Mismo prefijo que el resto del chat: `/api/chat` vía **api-gateway** (`:8080`). WebSocket STOMP sigue en `/ws/chat` (proxy Vite → `:8089` en dev).

## Modelo de mensaje (texto e imagen)

Campos canónicos para el front (STOMP y REST upload). El historial REST también expone alias legacy.

| Campo | TEXTO | IMAGEN |
|-------|-------|--------|
| `tipo` | `"TEXTO"` | `"IMAGEN"` |
| `content` | obligatorio | opcional (comentario) |
| `imageUrl` | `null` | `/api/chat/media/{uuid}.jpg` |
| `timestamp` | ISO-8601 | ISO-8601 |
| `idMensaje`, `idReserva`, `idUsuario`, `sender` | siempre | siempre |

Alias aceptados en historial (`GET .../mensajes`): `contenido` = `content`, `urlMedia` = `imageUrl`, `fechaEnvio` = `timestamp`.

## Subir foto (paseador, paseo EN CURSO)

```http
POST /api/chat/reservas/{idReserva}/mensajes/imagen
Content-Type: multipart/form-data
```

| Part | Requerido |
|------|-----------|
| `file` | sí (JPG/PNG, máx. 10 MB) |
| `idUsuario` | sí (paseador) |
| `sender` | sí (nombre visible) |
| `comentario` | no |

**201** — cuerpo `ChatMessageOutbound` (mismo shape que STOMP en `/topic/reserva.{idReserva}`).

Errores: `400` validación, `403` no es el paseador, `409` paseo no en curso, `503` integración reserva.

## Ver / descargar imagen

```http
GET /api/chat/media/{filename}
GET /api/chat/media/{filename}?download=true
```

URL pública (UUID). Prefijo host: mismo que `VITE_API_BASE_URL` / gateway.

## Galería (tutor, paseo FINALIZADA)

```http
GET /api/chat/reservas/{idReserva}/galeria-paseo?idUsuario={idTutorUsuario}
```

**200** — array de `{ idMensaje, urlMedia, imageUrl, comentario, content, fechaEnvio, timestamp, idUsuario }`.

Errores: `403` no es el tutor, `409` paseo no finalizado, `503` integración reserva.

## Tiempo real

Tras `POST` imagen, el servidor publica en **`/topic/reserva.{idReserva}`** el mismo JSON que la respuesta 201. El front debe deduplicar por `idMensaje`.

## Orden de despliegue recomendado (backend)

1. PostgreSQL `chat_db`: ejecutar `src/main/resources/db/chat_db-mensaje-imagen-migration.sql`.
2. **reserva-service** (comprobante interno con `nombreEstadoReserva`).
3. **chat-service** con `SPRING_PROFILES_ACTIVE=prod`, `RESERVA_INTERNO_SECRET` alineado y `CHAT_MEDIA_UPLOAD_DIR` en volumen persistente.
4. **api-gateway** (rutas `/api/chat/**` ya configuradas).
5. Front cuando corresponda (fuera de este documento).

Errores JSON unificados: `{ "message": "..." }` vía `GlobalExceptionHandler` (mismo patrón que `reserva-service`).
