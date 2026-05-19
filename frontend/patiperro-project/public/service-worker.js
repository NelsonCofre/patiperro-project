// Service Worker base para futuras subtareas de push notifications del chat.
// En esta fase solo dejamos listeners seguros para background events.

self.addEventListener("install", (event) => {
  // Activa esta version apenas termine la instalacion.
  event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", (event) => {
  // Toma control de clientes abiertos sin esperar proxima navegacion.
  event.waitUntil(self.clients.claim());
});

self.addEventListener("push", (event) => {
  // Placeholder: la logica real de mostrar notificaciones se implementa despues.
  if (!event.data) {
    return;
  }
});

self.addEventListener("notificationclick", (event) => {
  // Placeholder: el deep-link al chat se resuelve en la subtarea correspondiente.
  event.notification?.close();
});

self.addEventListener("sync", (event) => {
  // Placeholder para futuras tareas de background sync.
  if (!event.tag) {
    return;
  }
});
