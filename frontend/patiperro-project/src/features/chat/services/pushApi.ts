import { ACCESS_TOKEN_SESSION_KEY, API_ENDPOINTS } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";

type PushSuscripcionPayload = {
  endpoint: string;
  p256dh: string;
  auth: string;
  userAgent?: string;
};

const SYNCED_ENDPOINT_STORAGE_PREFIX = "patiperro_push_synced_endpoint";

let syncInFlight: Promise<boolean> | null = null;

function syncedEndpointStorageKey(): string {
  return `${SYNCED_ENDPOINT_STORAGE_PREFIX}_${window.location.origin}`;
}

function readSyncedEndpoint(): string {
  return sessionStorage.getItem(syncedEndpointStorageKey())?.trim() ?? "";
}

function markEndpointSynced(endpoint: string): void {
  sessionStorage.setItem(syncedEndpointStorageKey(), endpoint);
}

function readApiErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const row = data as { message?: unknown; error?: unknown };
    const message = String(row.message ?? row.error ?? "").trim();
    if (message) return message;
  }
  return fallback;
}

async function parseJsonSafe(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text.trim()) return null;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return null;
  }
}

/** Convierte la clave VAPID (base64url) al formato que exige `applicationServerKey`. */
export function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; i += 1) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

export function isPushSubscriptionSupported(): boolean {
  return (
    typeof window !== "undefined" &&
    "serviceWorker" in navigator &&
    "PushManager" in window &&
    "Notification" in window
  );
}

export async function fetchVapidPublicKey(): Promise<string> {
  const response = await fetch(API_ENDPOINTS.notificaciones.push.vapidPublicKey, {
    method: "GET",
    credentials: "include"
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo obtener la clave VAPID."));
  }
  const publicKey =
    data && typeof data === "object"
      ? String((data as { publicKey?: unknown }).publicKey ?? "").trim()
      : "";
  if (!publicKey) {
    throw new Error("La clave VAPID publica no esta disponible.");
  }
  return publicKey;
}

export async function registerPushSubscription(payload: PushSuscripcionPayload): Promise<void> {
  const response = await fetch(API_ENDPOINTS.notificaciones.push.suscripciones, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...bearerAuthHeaders()
    },
    body: JSON.stringify({
      endpoint: payload.endpoint,
      p256dh: payload.p256dh,
      auth: payload.auth,
      userAgent: payload.userAgent ?? navigator.userAgent
    })
  });
  const data = await parseJsonSafe(response);
  if (!response.ok) {
    throw new Error(readApiErrorMessage(data, "No se pudo registrar la suscripcion push."));
  }
}

/**
 * Obtiene (o crea) la PushSubscription del navegador y la registra en notification-service.
 * Solo actua si hay permiso granted, sesion JWT y soporte de PushManager.
 */
export async function syncPushSubscriptionWithBackend(): Promise<boolean> {
  if (syncInFlight) {
    return syncInFlight;
  }

  syncInFlight = (async () => {
    if (!isPushSubscriptionSupported()) {
      return false;
    }
    if (Notification.permission !== "granted") {
      return false;
    }
    if (!sessionStorage.getItem(ACCESS_TOKEN_SESSION_KEY)?.trim()) {
      return false;
    }

    const registration = await navigator.serviceWorker.ready;
    const publicKey = await fetchVapidPublicKey();
    const applicationServerKey = urlBase64ToUint8Array(publicKey);

    let subscription = await registration.pushManager.getSubscription();
    if (!subscription) {
      subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey
      });
    }

    const json = subscription.toJSON();
    const endpoint = json.endpoint?.trim() ?? "";
    const p256dh = json.keys?.p256dh?.trim() ?? "";
    const auth = json.keys?.auth?.trim() ?? "";
    if (!endpoint || !p256dh || !auth) {
      throw new Error("La suscripcion push del navegador esta incompleta.");
    }

    if (readSyncedEndpoint() === endpoint) {
      return true;
    }

    await registerPushSubscription({ endpoint, p256dh, auth });
    markEndpointSynced(endpoint);
    return true;
  })()
    .catch((error) => {
      console.warn("No se pudo sincronizar la suscripcion push.", error);
      return false;
    })
    .finally(() => {
      syncInFlight = null;
    });

  return syncInFlight;
}
