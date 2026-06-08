import { createContext, useContext, useMemo, useState, type ReactNode } from "react";

type ActiveChatContextValue = {
  activeChatReservaId: number | null;
  setActiveChatReservaId: (id: number | null) => void;
};

const ActiveChatContext = createContext<ActiveChatContextValue | null>(null);

export function ActiveChatProvider({ children }: { children: ReactNode }) {
  const [activeChatReservaId, setActiveChatReservaId] = useState<number | null>(null);
  const value = useMemo(
    () => ({ activeChatReservaId, setActiveChatReservaId }),
    [activeChatReservaId]
  );

  return <ActiveChatContext.Provider value={value}>{children}</ActiveChatContext.Provider>;
}

export function useActiveChatReserva(): ActiveChatContextValue {
  const context = useContext(ActiveChatContext);
  if (!context) {
    throw new Error("useActiveChatReserva debe usarse dentro de ActiveChatProvider.");
  }
  return context;
}
