# Chat en tiempo real — historias y tareas

**Revisión en código (2026-05-18)**  
Leyenda: **[x]** hecho · **[~]** parcial · **[ ]** pendiente.

---

## Historia: Como Usuario, quiero enviar mensajes de texto en tiempo real para coordinar el encuentro.

### Criterios de aceptación

| Escenario | Backend (`chat-service` :8089) | Frontend (`features/chat`) |
|-----------|------------------------------|----------------------------|
| Envío/recepción instantánea (<1 s) | [x] STOMP `/app/chat.send` → `/topic/reserva.{id}` | [x] `chatWs.ts` STOMP + SockJS |
| Historial desde PostgreSQL | [x] `GET /api/chat/reservas/{id}/mensajes` | [x] `fetchChatHistory` vía gateway; envío persiste por STOMP |
| Indicador "Escribiendo..." | [x] `/app/chat.typing` + `idUsuario` | [x] `sendTypingSignal` por STOMP |
| Notificación fuera del chat | [~] push vía notification-service (flag **false** por defecto) | [x] toast en listas vía `subscribeChatMessages(reservaIds, …)` |
| Validación mensajes vacíos | [x] `validarChatRealtime` | [x] `draft.trim()`, botón deshabilitado |

### Tareas técnicas

- [x] **Cliente WebSocket:** `@stomp/stompjs` + `sockjs-client` en `chatWs.ts`.
- [x] **STOMP en Spring Boot:** `WebSocketConfig`, `ChatRealtimeController`.
- [x] **SockJS:** `.withSockJS()` en `/ws/chat`.
- [x] **api-gateway:** rutas `/api/chat/**` y `/ws/chat/**` → `:8089`.
- [x] **Integración E2E:** proxy Vite `/ws/chat` + envío `/app/chat.send`.

### Archivos clave

| Área | Archivos |
|------|----------|
| Backend | `config/WebSocketConfig.java`, `controller/ChatRealtimeController.java`, `controller/ChatController.java`, `service/ChatService.java` |
| Front | `services/chatWs.ts`, `services/chatApi.ts`, `hooks/useReservaChat.ts`, `components/ChatWindow/ChatWindow.tsx` |
| UI integrada | `PaseadorSolicitudes.tsx`, `TutorReservas.tsx` |

### Resumen (post-integración 2026-05-18)

- **Backend:** [x] STOMP + SockJS + REST + push (push opcional, flag deshabilitado por defecto).
- **Frontend:** [x] `chatWs.ts` con `@stomp/stompjs` + `sockjs-client` → `CHAT_SOCKJS_URL` (mismo origen / proxy Vite).
- **Infra:** [x] Rutas `api-gateway` `/api/chat/**` y `/ws/chat/**`; proxy Vite `/ws/chat`.

### Cómo probar

1. Levantar **api-gateway** (:8080), **chat-service** (:8089), PostgreSQL `chat_db`.
2. `npm run dev` en el front (`VITE_API_BASE_URL=proxy`).
3. Tutor y paseador en dos navegadores (o normal + incógnito), misma reserva, abrir chat y enviar mensaje.
4. Opcional push: `CHAT_INTEGRACION_NOTIFICATION_ENABLED=true` en chat-service + Web Push en notification-service.

### Cloudflare + ngrok (dev)

| Tráfico | Ruta | Flujo |
|---------|------|--------|
| Front | `https://*.trycloudflare.com` | cloudflared → Vite :5173 |
| REST chat | `/api/chat/...` | Cloudflare → Vite → ngrok (o :8080) → gateway → chat-service |
| WebSocket STOMP | `wss://*.trycloudflare.com/ws/chat` | Vite proxifica `/ws/chat` → **chat-service :8089** (no gateway) |

En `.env.local` del front (como el resto del proyecto):

- `VITE_API_BASE_URL=proxy`
- `VITE_GATEWAY_PROXY_TARGET=https://tu-subdominio.ngrok-free.dev` (ngrok → gateway :8080)
- `GATEWAY_EXTRA_CORS_ORIGINS` en api-gateway con la URL Cloudflare del front

El WebSocket usa el host de Cloudflare (`wss://…/ws/chat`). Vite reenvía `/ws/chat` **directo** a `http://127.0.0.1:8089` (chat-service en la misma PC que `npm run dev`). El REST `/api/chat` sigue por gateway/ngrok.

Override opcional en `.env.local`: `VITE_CHAT_WS_PROXY_TARGET=http://127.0.0.1:8089`

---

## Historia: Como Paseador, quiero enviar fotos del perro durante el paseo

**Revisión backend (2026-05-20)** — Contrato: `CHAT_API_FOTOS.md`.

| Escenario | Backend (`chat-service`) |
|-----------|--------------------------|
| Subir JPG/PNG ≤ 10 MB (paseador, paseo EN CURSO) | [x] `POST /api/chat/reservas/{id}/mensajes/imagen` + `ChatMediaStorageService` |
| Validación integración reserva | [x] `ReservaChatIntegracionClient` → comprobante interno |
| Media pública por UUID | [x] `GET /api/chat/media/{filename}` |
| Galería tutor (paseo FINALIZADA) | [x] `GET /api/chat/reservas/{id}/galeria-paseo?idUsuario=` |
| STOMP tras subida | [x] `/topic/reserva.{id}` |
| Push preview foto | [x] `ChatNuevoMensajePushIntegracionRequest` (flag notification off por defecto) |
| Errores JSON `{ message }` | [x] `GlobalExceptionHandler` |
| JWT en chat/media | [ ] pendiente (cambio transversal; requiere decisión explícita) |

### Despliegue manual

1. SQL: `db/chat_db-mensaje-imagen-migration.sql` en `chat_db`.
2. Reiniciar **reserva-service** y **chat-service** (`RESERVA_INTERNO_SECRET` = `patiperro.reserva.interno.secret`).
3. Prod: volumen en `CHAT_MEDIA_UPLOAD_DIR`.
