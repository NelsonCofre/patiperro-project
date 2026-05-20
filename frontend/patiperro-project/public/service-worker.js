// Service Worker base para futuras subtareas de push notifications del chat.
// En esta fase solo dejamos listeners seguros para background events.

const DEFAULT_PUSH_TITLE = "Nuevo mensaje";
const DEFAULT_PUSH_BODY = "Tienes un nuevo mensaje en el chat.";
const DEFAULT_PUSH_ICON = "/favicon.svg";
const DEFAULT_CHAT_PATH_PREFIX = "/chat/reserva/";
const CHAT_VISIBILITY_EVENT = "chat-visibility";
const CHAT_PRESENCE_TTL_MS = 15000;

const activeChatPresenceByReserva = new Map();

function normalizePushPayload(rawPayload) {
  if (!rawPayload || typeof rawPayload !== "object") {
    return null;
  }

  const payload = rawPayload;
  const title =
    typeof payload.title === "string" && payload.title.trim()
      ? payload.title.trim()
      : DEFAULT_PUSH_TITLE;
  const body =
    typeof payload.body === "string" && payload.body.trim()
      ? payload.body.trim()
      : DEFAULT_PUSH_BODY;
  const tag =
    typeof payload.tag === "string" && payload.tag.trim() ? payload.tag.trim() : "chat-message";
  const icon =
    typeof payload.icon === "string" && payload.icon.trim()
      ? payload.icon.trim()
      : DEFAULT_PUSH_ICON;
  const badge =
    typeof payload.badge === "string" && payload.badge.trim()
      ? payload.badge.trim()
      : icon;
  const data =
    payload.data && typeof payload.data === "object" ? payload.data : {};

  return {
    title,
    options: {
      body,
      tag,
      icon,
      badge,
      data
    }
  };
}

function readPushPayload(event) {
  if (!event.data) {
    return null;
  }

  try {
    return normalizePushPayload(event.data.json());
  } catch (error) {
    try {
      const text = event.data.text();
      return normalizePushPayload({ title: DEFAULT_PUSH_TITLE, body: text });
    } catch (textError) {
      console.warn("No se pudo interpretar el payload push.", error, textError);
      return null;
    }
  }
}

function normalizeReservaId(value) {
  if (typeof value === "number" && Number.isFinite(value) && value > 0) {
    return value;
  }
  if (typeof value === "string" && /^\d+$/.test(value.trim())) {
    return Number(value.trim());
  }
  return null;
}

function cleanupStaleChatPresence(now = Date.now()) {
  for (const [reservaId, presence] of activeChatPresenceByReserva.entries()) {
    if (!presence || now - presence.lastUpdatedAt > CHAT_PRESENCE_TTL_MS) {
      activeChatPresenceByReserva.delete(reservaId);
    }
  }
}

function updateChatPresence(message) {
  const reservaId = normalizeReservaId(message?.idReserva);
  if (!reservaId) {
    return;
  }

  if (!message?.isChatOpen) {
    activeChatPresenceByReserva.delete(reservaId);
    return;
  }

  activeChatPresenceByReserva.set(reservaId, {
    visibilityState: message.visibilityState === "visible" ? "visible" : "hidden",
    focused: Boolean(message.focused),
    lastUpdatedAt: Date.now()
  });
}

function shouldSuppressPushNotification(payload) {
  cleanupStaleChatPresence();

  const reservaId = normalizeReservaId(payload?.options?.data?.idReserva);
  if (!reservaId) {
    return false;
  }

  const presence = activeChatPresenceByReserva.get(reservaId);
  if (!presence) {
    return false;
  }

  return presence.visibilityState === "visible" && presence.focused === true;
}

self.addEventListener("install", (event) => {
  // Activa esta version apenas termine la instalacion.
  event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", (event) => {
  // Toma control de clientes abiertos sin esperar proxima navegacion.
  event.waitUntil(self.clients.claim());
});

self.addEventListener("push", (event) => {
  const payload = readPushPayload(event);
  if (!payload) return;
  if (shouldSuppressPushNotification(payload)) {
    return;
  }

  event.waitUntil(self.registration.showNotification(payload.title, payload.options));
});

self.addEventListener("message", (event) => {
  if (!event.data || event.data.type !== CHAT_VISIBILITY_EVENT) {
    return;
  }
  updateChatPresence(event.data);
});

self.addEventListener("notificationclick", (event) => {
  event.notification?.close();

  event.waitUntil(
    (async () => {
      const notificationData =
        event.notification && typeof event.notification.data === "object"
          ? event.notification.data
          : {};

      let targetPath =
        typeof notificationData.url === "string" && notificationData.url.trim()
          ? notificationData.url.trim()
          : "";

      if (!targetPath) {
        const idReserva = notificationData.idReserva;
        if (typeof idReserva === "number" || typeof idReserva === "string") {
          targetPath = `${DEFAULT_CHAT_PATH_PREFIX}${idReserva}`;
        }
      }

      const targetUrl = new URL(targetPath || "/", self.location.origin).href;
      const windowClients = await self.clients.matchAll({
        type: "window",
        includeUncontrolled: true
      });

      for (const client of windowClients) {
        if (!("url" in client)) {
          continue;
        }

        const sameOrigin = client.url.startsWith(self.location.origin);
        if (!sameOrigin) {
          continue;
        }

        if ("navigate" in client && client.url !== targetUrl) {
          await client.navigate(targetUrl);
        }
        await client.focus();
        return;
      }

      await self.clients.openWindow(targetUrl);
    })()
  );
});

self.addEventListener("sync", (event) => {
  // Placeholder para futuras tareas de background sync.
  if (!event.tag) {
    return;
  }
});
