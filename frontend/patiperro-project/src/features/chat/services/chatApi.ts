import { API_BASE_URL } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";
import type { ChatMessage, ChatMessageTipo, ChatRole } from "../types/chat.types";

const CHAT_HISTORY_STORAGE_KEY = "patiperro_chat_history_v1";

type ChatMessageApiResponse = {
  id?: string | number | null;
  idMensaje?: string | number | null;
  idReserva?: number | null;
  reservaId?: number | null;
  senderUserId?: number | null;
  idUsuario?: number | null;
  senderRole?: ChatRole | string | null;
  rolEmisor?: ChatRole | string | null;
  senderName?: string | null;
  nombreEmisor?: string | null;
  sender?: string | null;
  tipo?: string | null;
  content?: string | null;
  contenido?: string | null;
  imageUrl?: string | null;
  urlMedia?: string | null;
  timestamp?: string | null;
  fechaEnvio?: string | null;
  estado?: string | null;
  estadoMensaje?: string | null;
};

export type GaleriaPaseoItem = {
  idMensaje: number;
  imageUrl: string;
  content: string;
  timestamp: string;
};

function historyEndpoint(reservaId: number): string {
  return `${API_BASE_URL}/api/chat/reservas/${reservaId}/mensajes`;
}

function imageUploadEndpoint(reservaId: number): string {
  return `${API_BASE_URL}/api/chat/reservas/${reservaId}/mensajes/imagen`;
}

function galeriaEndpoint(reservaId: number, idUsuario: number): string {
  return `${API_BASE_URL}/api/chat/reservas/${reservaId}/galeria-paseo?idUsuario=${idUsuario}`;
}

function readLocalHistory(): Record<string, ChatMessage[]> {
  if (typeof window === "undefined") return {};
  try {
    const raw = window.localStorage.getItem(CHAT_HISTORY_STORAGE_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as Record<string, ChatMessage[]>;
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

function writeLocalHistory(next: Record<string, ChatMessage[]>): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(CHAT_HISTORY_STORAGE_KEY, JSON.stringify(next));
}

function normalizeRole(value: unknown): ChatRole {
  const raw = String(value ?? "").trim().toLowerCase();
  return raw === "paseador" ? "paseador" : "tutor";
}

function normalizeEstado(value: unknown): ChatMessage["estado"] {
  const raw = String(value ?? "").trim().toLowerCase();
  if (raw === "pendiente" || raw === "error" || raw === "enviado") return raw;
  return "enviado";
}

function normalizeTipo(value: unknown): ChatMessageTipo {
  return String(value ?? "TEXTO").trim().toUpperCase() === "IMAGEN" ? "IMAGEN" : "TEXTO";
}

function readErrorMessage(data: unknown, fallback: string): string {
  if (data && typeof data === "object") {
    const o = data as { message?: string; error?: string; mensaje?: string };
    const m = o.message ?? o.mensaje ?? o.error;
    if (typeof m === "string" && m.trim()) return m;
  }
  return fallback;
}

export function mapApiMessage(
  reservaId: number,
  row: ChatMessageApiResponse
): ChatMessage | null {
  const tipo = normalizeTipo(row.tipo);
  const content = String(row.content ?? row.contenido ?? "").trim();
  const imageUrl = String(row.imageUrl ?? row.urlMedia ?? "").trim() || undefined;
  const timestamp = String(row.timestamp ?? row.fechaEnvio ?? "").trim();
  const senderUserId = Number(row.senderUserId ?? row.idUsuario ?? 0);

  if (!timestamp || !Number.isFinite(senderUserId) || senderUserId <= 0) {
    return null;
  }
  if (tipo === "TEXTO" && !content) {
    return null;
  }
  if (tipo === "IMAGEN" && !imageUrl) {
    return null;
  }

  return {
    id: String(row.id ?? row.idMensaje ?? `${reservaId}-${timestamp}-${senderUserId}`),
    idReserva: Number(row.idReserva ?? row.reservaId ?? reservaId) || reservaId,
    senderUserId,
    senderRole: normalizeRole(row.senderRole ?? row.rolEmisor),
    senderName: String(row.senderName ?? row.nombreEmisor ?? row.sender ?? "Usuario").trim() || "Usuario",
    tipo,
    content,
    imageUrl,
    timestamp,
    estado: normalizeEstado(row.estado ?? row.estadoMensaje)
  };
}

function sortMessages(messages: ChatMessage[]): ChatMessage[] {
  return [...messages].sort((a, b) => {
    const ta = new Date(a.timestamp).getTime();
    const tb = new Date(b.timestamp).getTime();
    if (Number.isNaN(ta) || Number.isNaN(tb)) {
      return a.timestamp.localeCompare(b.timestamp);
    }
    return ta - tb;
  });
}

export function readLocalChatHistory(reservaId: number): ChatMessage[] {
  const store = readLocalHistory();
  return sortMessages(store[String(reservaId)] ?? []);
}

export function persistLocalChatMessage(message: ChatMessage): void {
  const store = readLocalHistory();
  const key = String(message.idReserva);
  const current = store[key] ?? [];
  if (current.some((item) => item.id === message.id)) {
    return;
  }
  store[key] = sortMessages([...current, message]);
  writeLocalHistory(store);
}

export async function fetchChatHistory(reservaId: number): Promise<ChatMessage[]> {
  try {
    const response = await fetch(historyEndpoint(reservaId), {
      method: "GET",
      credentials: "include",
      headers: {
        ...bearerAuthHeaders()
      }
    });

    if (!response.ok) {
      throw new Error("Historial no disponible todavia.");
    }

    const data = (await response.json()) as unknown;
    if (!Array.isArray(data)) {
      return readLocalChatHistory(reservaId);
    }

    const mapped = data
      .map((row) => mapApiMessage(reservaId, row as ChatMessageApiResponse))
      .filter((row): row is ChatMessage => row != null);

    if (mapped.length > 0) {
      const store = readLocalHistory();
      store[String(reservaId)] = sortMessages(mapped);
      writeLocalHistory(store);
    }

    return sortMessages(mapped);
  } catch {
    return readLocalChatHistory(reservaId);
  }
}

export async function uploadChatImage(
  reservaId: number,
  idUsuario: number,
  sender: string,
  file: File,
  comentario?: string
): Promise<ChatMessage> {
  const body = new FormData();
  body.append("file", file);
  body.append("idUsuario", String(idUsuario));
  body.append("sender", sender);
  if (comentario?.trim()) {
    body.append("comentario", comentario.trim());
  }

  const response = await fetch(imageUploadEndpoint(reservaId), {
    method: "POST",
    credentials: "include",
    headers: {
      ...bearerAuthHeaders()
    },
    body
  });

  let data: unknown = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(readErrorMessage(data, "No se pudo enviar la foto."));
  }

  const mapped = mapApiMessage(reservaId, data as ChatMessageApiResponse);
  if (!mapped) {
    throw new Error("Respuesta inválida al subir la imagen.");
  }
  return mapped;
}

export async function fetchGaleriaPaseo(
  reservaId: number,
  idUsuario: number
): Promise<GaleriaPaseoItem[]> {
  const response = await fetch(galeriaEndpoint(reservaId, idUsuario), {
    method: "GET",
    credentials: "include",
    headers: {
      ...bearerAuthHeaders()
    }
  });

  let data: unknown = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(readErrorMessage(data, "No se pudo cargar la galería del paseo."));
  }

  if (!Array.isArray(data)) {
    return [];
  }

  return data
    .map((row) => {
      const item = row as ChatMessageApiResponse & { idMensaje?: number };
      const imageUrl = String(item.imageUrl ?? item.urlMedia ?? "").trim();
      const timestamp = String(item.timestamp ?? item.fechaEnvio ?? "").trim();
      const idMensaje = Number(item.idMensaje ?? item.id ?? 0);
      if (!imageUrl || !timestamp || !Number.isFinite(idMensaje)) {
        return null;
      }
      return {
        idMensaje,
        imageUrl,
        content: String(item.content ?? item.contenido ?? "").trim(),
        timestamp
      } satisfies GaleriaPaseoItem;
    })
    .filter((item): item is GaleriaPaseoItem => item != null);
}

export function chatMediaDownloadUrl(imageUrl: string): string {
  const base = imageUrl.includes("?") ? imageUrl : `${imageUrl}?download=true`;
  return base.includes("download=") ? base : `${imageUrl}${imageUrl.includes("?") ? "&" : "?"}download=true`;
}
