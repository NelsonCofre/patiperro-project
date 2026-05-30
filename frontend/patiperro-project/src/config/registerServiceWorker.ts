const SERVICE_WORKER_URL = "/service-worker.js";

export async function registerServiceWorker(): Promise<void> {
  if (typeof window === "undefined" || !("serviceWorker" in navigator)) {
    return;
  }

  try {
    await navigator.serviceWorker.register(SERVICE_WORKER_URL);
  } catch (error) {
    console.error("No se pudo registrar el service worker.", error);
  }
}
