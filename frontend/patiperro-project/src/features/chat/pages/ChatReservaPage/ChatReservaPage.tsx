import { useMemo } from "react";
import { Navigate, useNavigate, useParams } from "react-router-dom";
import { PASEADOR_ID_SESSION_KEY, TUTOR_ID_SESSION_KEY } from "../../../../config/api";
import ChatWindow from "../../components/ChatWindow/ChatWindow";
import type { ChatRole } from "../../types/chat.types";

type ChatSessionContext = {
  currentUserId: number;
  currentUserRole: ChatRole;
  currentUserName: string;
  counterpartName: string;
  mascotaNombre: string;
  closePath: string;
};

function readChatSessionContext(): ChatSessionContext | null {
  const tutorIdRaw = sessionStorage.getItem(TUTOR_ID_SESSION_KEY)?.trim() ?? "";
  const paseadorIdRaw = sessionStorage.getItem(PASEADOR_ID_SESSION_KEY)?.trim() ?? "";
  const currentUserName =
    sessionStorage.getItem("patiperro_nombre_usuario")?.trim() || "Usuario";

  const tutorId = Number(tutorIdRaw);
  if (Number.isFinite(tutorId) && tutorId > 0) {
    return {
      currentUserId: tutorId,
      currentUserRole: "tutor",
      currentUserName,
      counterpartName: "Paseador",
      mascotaNombre: "Mascota",
      closePath: "/tutor/reservas"
    };
  }

  const paseadorId = Number(paseadorIdRaw);
  if (Number.isFinite(paseadorId) && paseadorId > 0) {
    return {
      currentUserId: paseadorId,
      currentUserRole: "paseador",
      currentUserName,
      counterpartName: "Tutor",
      mascotaNombre: "Mascota",
      closePath: "/paseador/dashboard/solicitudes"
    };
  }

  return null;
}

export default function ChatReservaPage() {
  const navigate = useNavigate();
  const { idReserva } = useParams();

  const chatSession = useMemo(() => readChatSessionContext(), []);
  const reservaId = idReserva && /^\d+$/.test(idReserva) ? Number(idReserva) : NaN;

  if (!chatSession) {
    return <Navigate to="/" replace />;
  }

  if (!Number.isFinite(reservaId) || reservaId <= 0) {
    return <Navigate to={chatSession.closePath} replace />;
  }

  return (
    <ChatWindow
      isOpen
      reservaId={reservaId}
      currentUserId={chatSession.currentUserId}
      currentUserRole={chatSession.currentUserRole}
      currentUserName={chatSession.currentUserName}
      counterpartName={chatSession.counterpartName}
      mascotaNombre={chatSession.mascotaNombre}
      onClose={() => navigate(chatSession.closePath, { replace: true })}
    />
  );
}
