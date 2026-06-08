import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useActiveChatReserva } from "../../context/ActiveChatContext";
import { useChatUnread } from "../../context/ChatUnreadContext";
import {
  loadChatEligibleReservaIds,
  readSessionChatUser,
  shouldSuppressChatNotification,
  showBrowserChatNotification
} from "../../services/chatNotificationSupport";
import { subscribeChatMessages } from "../../services/chatWs";
import type { ChatToastPayload } from "../../types/chat.types";
import { buildChatMessageSnippet } from "../../utils/chatFormatters";
import styles from "./GlobalChatNotifications.module.css";

const REFRESH_MS = 15000;
const TOAST_MS = 8000;

function sameReservaIdSet(previous: number[], next: number[]): boolean {
  if (previous.length !== next.length) {
    return false;
  }
  const sortedPrevious = [...previous].sort((a, b) => a - b);
  const sortedNext = [...next].sort((a, b) => a - b);
  return sortedPrevious.every((id, index) => id === sortedNext[index]);
}

export default function GlobalChatNotifications() {
  const navigate = useNavigate();
  const location = useLocation();
  const { activeChatReservaId } = useActiveChatReserva();
  const { registerUnreadMessage, clearUnreadForReserva } = useChatUnread();
  const [reservaIds, setReservaIds] = useState<number[]>([]);
  const [chatToast, setChatToast] = useState<ChatToastPayload | null>(null);

  const sessionUser = useMemo(() => readSessionChatUser(), [location.pathname]);
  const activeChatReservaIdRef = useRef(activeChatReservaId);
  const pathnameRef = useRef(location.pathname);
  const sessionUserRef = useRef(sessionUser);

  useEffect(() => {
    activeChatReservaIdRef.current = activeChatReservaId;
  }, [activeChatReservaId]);

  useEffect(() => {
    pathnameRef.current = location.pathname;
  }, [location.pathname]);

  useEffect(() => {
    sessionUserRef.current = sessionUser;
  }, [sessionUser]);

  const openChat = useCallback(
    (reservaId: number) => {
      clearUnreadForReserva(reservaId);
      navigate(`/chat/reserva/${reservaId}`);
    },
    [clearUnreadForReserva, navigate]
  );

  const loadReservaIds = useCallback(async () => {
    const user = readSessionChatUser();
    if (!user) {
      setReservaIds([]);
      return;
    }

    try {
      const nextIds = await loadChatEligibleReservaIds();
      setReservaIds((previous) => (sameReservaIdSet(previous, nextIds) ? previous : nextIds));
    } catch {
      // Mantenemos los IDs previos si falla un refresco en segundo plano.
    }
  }, []);

  useEffect(() => {
    void loadReservaIds();
    const timer = window.setInterval(() => void loadReservaIds(), REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [loadReservaIds, location.pathname]);

  useEffect(() => {
    const handleStorage = (event: StorageEvent) => {
      if (
        event.key === null ||
        event.key.includes("patiperro_access_token") ||
        event.key.includes("patiperro_tutor_id") ||
        event.key.includes("patiperro_paseador_id")
      ) {
        void loadReservaIds();
      }
    };

    window.addEventListener("storage", handleStorage);
    return () => window.removeEventListener("storage", handleStorage);
  }, [loadReservaIds]);

  useEffect(() => {
    if (!sessionUser || reservaIds.length === 0) {
      return;
    }

    return subscribeChatMessages(reservaIds, (message) => {
      const user = sessionUserRef.current;
      if (!user) {
        return;
      }

      if (
        shouldSuppressChatNotification({
          messageReservaId: message.idReserva,
          senderUserId: message.senderUserId,
          currentUserId: user.userId,
          activeChatReservaId: activeChatReservaIdRef.current,
          pathname: pathnameRef.current
        })
      ) {
        return;
      }

      const payload: ChatToastPayload = {
        reservaId: message.idReserva,
        senderName: message.senderName,
        snippet: buildChatMessageSnippet(message)
      };

      registerUnreadMessage(message.idReserva);
      setChatToast(payload);
      showBrowserChatNotification(payload, openChat);
    });
  }, [openChat, registerUnreadMessage, reservaIds, sessionUser]);

  useEffect(() => {
    if (!chatToast) {
      return;
    }

    const timer = window.setTimeout(() => setChatToast(null), TOAST_MS);
    return () => window.clearTimeout(timer);
  }, [chatToast]);

  if (!sessionUser || !chatToast) {
    return null;
  }

  return (
    <button
      type="button"
      className={styles.chatToast}
      aria-live="polite"
      onClick={() => {
        const reservaId = chatToast.reservaId;
        setChatToast(null);
        openChat(reservaId);
      }}
    >
      <strong>Nuevo mensaje de {chatToast.senderName}</strong>
      <span>{chatToast.snippet}</span>
    </button>
  );
}
