import {
  ACCESS_TOKEN_SESSION_KEY,
  PASEADOR_ID_SESSION_KEY,
  TUTOR_ID_SESSION_KEY
} from "../../../config/api";
import { fetchSolicitudesPendientesPaseador } from "../../paseador/services/solicitudesPaseadorService";
import { fetchReservasDetalleTutor } from "../../tutor/services/reservaTutorApi";
import { getReservaEstadoMeta } from "../../tutor/utils/reservaEstadoUtils";
import type { ChatToastPayload } from "../types/chat.types";

export type SessionChatUser = {
  userId: number;
  role: "tutor" | "paseador";
};

export function readSessionChatUser(): SessionChatUser | null {
  const token = sessionStorage.getItem(ACCESS_TOKEN_SESSION_KEY)?.trim();
  if (!token) {
    return null;
  }

  const tutorId = Number(sessionStorage.getItem(TUTOR_ID_SESSION_KEY)?.trim());
  if (Number.isFinite(tutorId) && tutorId > 0) {
    return { userId: tutorId, role: "tutor" };
  }

  const paseadorId = Number(sessionStorage.getItem(PASEADOR_ID_SESSION_KEY)?.trim());
  if (Number.isFinite(paseadorId) && paseadorId > 0) {
    return { userId: paseadorId, role: "paseador" };
  }

  return null;
}

function tutorReservaTieneChatActivo(
  reserva: Parameters<typeof getReservaEstadoMeta>[0]
): boolean {
  const estado = getReservaEstadoMeta(reserva).key;
  return estado === "en_curso" || estado === "finalizada";
}

function paseadorSolicitudTieneChatActivo(solicitud: {
  chatActivo?: boolean;
  estado: string;
}): boolean {
  return (
    Boolean(solicitud.chatActivo) ||
    solicitud.estado === "En Curso" ||
    solicitud.estado === "Finalizada"
  );
}

export async function loadChatEligibleReservaIds(): Promise<number[]> {
  const user = readSessionChatUser();
  if (!user) {
    return [];
  }

  if (user.role === "tutor") {
    const tutorId = Number(sessionStorage.getItem(TUTOR_ID_SESSION_KEY));
    const reservas = await fetchReservasDetalleTutor(tutorId);
    return reservas
      .filter((reserva) => tutorReservaTieneChatActivo(reserva))
      .map((reserva) => reserva.idReserva);
  }

  const solicitudes = await fetchSolicitudesPendientesPaseador();
  return solicitudes
    .filter((solicitud) => paseadorSolicitudTieneChatActivo(solicitud))
    .map((solicitud) => solicitud.idReserva);
}

export function shouldSuppressChatNotification(input: {
  messageReservaId: number;
  senderUserId: number;
  currentUserId: number;
  activeChatReservaId: number | null;
  pathname: string;
}): boolean {
  if (input.senderUserId === input.currentUserId) {
    return true;
  }
  if (input.activeChatReservaId === input.messageReservaId) {
    return true;
  }
  return input.pathname === `/chat/reserva/${input.messageReservaId}`;
}

export function showBrowserChatNotification(
  payload: ChatToastPayload,
  onOpen?: (reservaId: number) => void
): void {
  if (typeof window === "undefined" || !("Notification" in window)) {
    return;
  }
  if (Notification.permission !== "granted") {
    return;
  }
  if (document.visibilityState === "visible" && document.hasFocus()) {
    return;
  }

  const notification = new Notification(`Nuevo mensaje de ${payload.senderName}`, {
    body: payload.snippet,
    tag: `chat-reserva-${payload.reservaId}`,
    icon: "/favicon.svg"
  });

  notification.onclick = () => {
    window.focus();
    notification.close();
    onOpen?.(payload.reservaId);
  };
}
