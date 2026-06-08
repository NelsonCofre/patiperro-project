import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode
} from "react";

type ChatUnreadContextValue = {
  unreadCount: number;
  registerUnreadMessage: (reservaId: number) => void;
  clearUnreadForReserva: (reservaId: number) => void;
  clearAllUnread: () => void;
};

const ChatUnreadContext = createContext<ChatUnreadContextValue | null>(null);

export function ChatUnreadProvider({ children }: { children: ReactNode }) {
  const [unreadByReserva, setUnreadByReserva] = useState<Record<number, number>>({});

  const registerUnreadMessage = useCallback((reservaId: number) => {
    if (!Number.isFinite(reservaId) || reservaId <= 0) {
      return;
    }
    setUnreadByReserva((previous) => ({
      ...previous,
      [reservaId]: (previous[reservaId] ?? 0) + 1
    }));
  }, []);

  const clearUnreadForReserva = useCallback((reservaId: number) => {
    if (!Number.isFinite(reservaId) || reservaId <= 0) {
      return;
    }
    setUnreadByReserva((previous) => {
      if (!(reservaId in previous)) {
        return previous;
      }
      const next = { ...previous };
      delete next[reservaId];
      return next;
    });
  }, []);

  const clearAllUnread = useCallback(() => {
    setUnreadByReserva({});
  }, []);

  const unreadCount = useMemo(
    () => Object.values(unreadByReserva).reduce((total, count) => total + count, 0),
    [unreadByReserva]
  );

  const value = useMemo(
    () => ({
      unreadCount,
      registerUnreadMessage,
      clearUnreadForReserva,
      clearAllUnread
    }),
    [clearAllUnread, clearUnreadForReserva, registerUnreadMessage, unreadCount]
  );

  return <ChatUnreadContext.Provider value={value}>{children}</ChatUnreadContext.Provider>;
}

export function useChatUnread(): ChatUnreadContextValue {
  const context = useContext(ChatUnreadContext);
  if (!context) {
    throw new Error("useChatUnread debe usarse dentro de ChatUnreadProvider.");
  }
  return context;
}
