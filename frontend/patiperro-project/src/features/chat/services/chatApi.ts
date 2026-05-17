import { API_BASE_URL } from "../../../config/api";
import { bearerAuthHeaders } from "../../../config/authHeaders";
import type { ChatMessage, ChatRole } from "../types/chat.types";

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
  content?: string | null;
  contenido?: string | null;
  timestamp?: string | null;
  fechaEnvio?: string | null;
  estado?: string | null;
};

function historyEndpoint(reservaId: number): string {
  return `${API_BASE_URL}/api/chat/reservas/${reservaId}/mensajes`;
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

function mapApiMessage(
  reservaId: number,
  row: ChatMessageApiResponse
): ChatMessage | null {
  const content = String(row.content ?? row.contenido ?? "").trim();
  const timestamp = String(row.timestamp ?? row.fechaEnvio ?? "").trim();
  const senderUserId = Number(row.senderUserId ?? row.idUsuario ?? 0);
  if (!content || !timestamp || !Number.isFinite(senderUserId) || senderUserId <= 0) {
    return null;
  }

  return {
    id: String(row.id ?? row.idMensaje ?? `${reservaId}-${timestamp}-${senderUserId}`),
    idReserva: Number(row.idReserva ?? row.reservaId ?? reservaId) || reservaId,
    senderUserId,
    senderRole: normalizeRole(row.senderRole ?? row.rolEmisor),
    senderName: String(row.senderName ?? row.nombreEmisor ?? "Usuario").trim() || "Usuario",
    content,
    timestamp,
    estado: normalizeEstado(row.estado)
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

