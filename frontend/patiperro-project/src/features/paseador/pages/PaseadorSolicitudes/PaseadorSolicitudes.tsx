import { useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import ChatWindow from "../../../chat/components/ChatWindow/ChatWindow";
import { usePushNotifications } from "../../../chat/hooks/usePushNotifications";
import { dispararNotificacion } from "../../../tutor/services/notificacionesApi";
import EstadoFilterTabs from "../../../shared/components/EstadoFilterTabs/EstadoFilterTabs";
import type { EstadoFilterTab } from "../../../shared/components/EstadoFilterTabs/EstadoFilterTabs";
import ConfirmarDecisionSolicitudModal from "../../components/ConfirmarDecisionSolicitudModal/ConfirmarDecisionSolicitudModal";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import SolicitudPendienteCard from "../../components/SolicitudPendienteCard/SolicitudPendienteCard";
import TutorDetalleModal from "../../components/TutorDetalleModal/TutorDetalleModal";
import MapaEncuentroModal from "../../../shared/components/MapaEncuentroModal/MapaEncuentroModal";
import { geocodeEncuentroAddress } from "../../../shared/utils/geocodeAddress";
import {
  fetchSolicitudesPendientesPaseador,
  finalizarPaseoPaseador,
  responderSolicitudPaseador
} from "../../services/solicitudesPaseadorService";
import { PASEADOR_ID_SESSION_KEY } from "../../../../config/api";
import { subscribeEncuentroTopic } from "../../../shared/services/encuentroWs";
import { findProximaSolicitudId } from "../../utils/paseoHorarioUtils";
import type {
  DecisionSolicitud,
  RechazoSolicitudForm,
  SolicitudPendientePaseador
} from "../../types/solicitudPaseador.types";
import { esSolicitudPorResponder } from "../../utils/solicitudEstadoUtils";
import styles from "./PaseadorSolicitudes.module.css";

type ConfirmationState = {
  solicitud: SolicitudPendientePaseador;
  decision: DecisionSolicitud;
};

type FeedbackState = {
  type: "success" | "error";
  message: string;
};

type ViewKey = "solicitadas" | "aceptadas" | "en-curso" | "finalizadas" | "rechazadas" | "expiradas";

type ViewConfig = {
  key: ViewKey;
  title: string;
  shortLabel: string;
  description: string;
  emptyTitle: string;
  emptyText: string;
  filter: (solicitud: SolicitudPendientePaseador) => boolean;
  route: string;
};

function esSolicitudPorResponderLocal(estado: SolicitudPendientePaseador["estado"]): boolean {
  return esSolicitudPorResponder(estado);
}

const VIEW_CONFIGS: ViewConfig[] = [
  {
    key: "solicitadas",
    title: "Solicitudes por responder",
    shortLabel: "Por responder",
    description: "Reservas nuevas que aún esperan tu decisión.",
    emptyTitle: "No tienes solicitudes por responder",
    emptyText: "Cuando un tutor envíe una reserva dentro de tus bloques disponibles, aparecerá aquí.",
    filter: (solicitud) => esSolicitudPorResponderLocal(solicitud.estado),
    route: "/paseador/dashboard/solicitudes"
  },
  {
    key: "aceptadas",
    title: "Reservas aceptadas",
    shortLabel: "Aceptadas",
    description: "Reservas listas para validar el código e iniciar el paseo.",
    emptyTitle: "No tienes reservas aceptadas",
    emptyText: "Las reservas que aceptes aparecerán aquí para continuar con el encuentro.",
    filter: (solicitud) => solicitud.estado === "Aceptada",
    route: "/paseador/dashboard/solicitudes/aceptadas"
  },
  {
    key: "en-curso",
    title: "Paseos en curso",
    shortLabel: "En curso",
    description: "Servicios que ya fueron confirmados y se están realizando ahora.",
    emptyTitle: "No tienes paseos en curso",
    emptyText: "Cuando confirmes el inicio de un paseo, quedará visible aquí con acceso al chat.",
    filter: (solicitud) => solicitud.estado === "En Curso",
    route: "/paseador/dashboard/solicitudes/en-curso"
  },
  {
    key: "finalizadas",
    title: "Paseos finalizados",
    shortLabel: "Finalizadas",
    description: "Historial de servicios completados y en proceso de liberación de pago.",
    emptyTitle: "No tienes paseos finalizados",
    emptyText: "Los paseos que cierres aparecerán aquí como referencia.",
    filter: (solicitud) => solicitud.estado === "Finalizada",
    route: "/paseador/dashboard/solicitudes/finalizadas"
  },
  {
    key: "rechazadas",
    title: "Solicitudes rechazadas",
    shortLabel: "Rechazadas",
    description: "Historial de reservas que decidiste no tomar.",
    emptyTitle: "No tienes solicitudes rechazadas",
    emptyText: "Las solicitudes rechazadas seguirán visibles aquí como referencia.",
    filter: (solicitud) => solicitud.estado === "Rechazada",
    route: "/paseador/dashboard/solicitudes/rechazadas"
  },
  {
    key: "expiradas",
    title: "Solicitudes expiradas",
    shortLabel: "Expiradas",
    description: "Reservas que vencieron porque no se respondieron a tiempo.",
    emptyTitle: "No tienes solicitudes expiradas",
    emptyText: "Las solicitudes expiradas por plazo de aceptación aparecerán aquí.",
    filter: (solicitud) => solicitud.estado === "Expirada",
    route: "/paseador/dashboard/solicitudes/expiradas"
  }
];
const REFRESH_MS = 15000;

function getActiveView(pathname: string): ViewConfig {
  if (pathname.endsWith("/aceptadas")) return VIEW_CONFIGS[1];
  if (pathname.endsWith("/en-curso")) return VIEW_CONFIGS[2];
  if (pathname.endsWith("/finalizadas")) return VIEW_CONFIGS[3];
  if (pathname.endsWith("/rechazadas")) return VIEW_CONFIGS[4];
  if (pathname.endsWith("/expiradas")) return VIEW_CONFIGS[5];
  return VIEW_CONFIGS[0];
}

export default function PaseadorSolicitudes() {
  const { requestPermission } = usePushNotifications();
  const location = useLocation();
  const activeView = useMemo(() => getActiveView(location.pathname), [location.pathname]);

  const [solicitudes, setSolicitudes] = useState<SolicitudPendientePaseador[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  
  // Estados para el Mapa
  const [mapConfig, setMapConfig] = useState<{ lat: number, lng: number, dir: string } | null>(null);

  const [confirmation, setConfirmation] = useState<ConfirmationState | null>(null);
  const [processing, setProcessing] = useState<ConfirmationState | null>(null);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [selectedTutorSolicitud, setSelectedTutorSolicitud] = useState<SolicitudPendientePaseador | null>(null);
  const [finalizingId, setFinalizingId] = useState<number | null>(null);
  const [activeChatReservaId, setActiveChatReservaId] = useState<number | null>(null);

  const paseadorIdRaw = sessionStorage.getItem(PASEADOR_ID_SESSION_KEY);
  const currentPaseadorId = paseadorIdRaw ? Number(paseadorIdRaw) : 0;
  const currentPaseadorName =
    sessionStorage.getItem("patiperro_nombre_usuario")?.trim() || "Paseador";

  async function loadSolicitudes(silent = false) {
    if (!silent) {
      setIsLoading(true);
    }
    setLoadError("");
    try {
      const data = await fetchSolicitudesPendientesPaseador();
      setSolicitudes(data);
    } catch (error) {
      setLoadError(
        error instanceof Error ? error.message : "No se pudieron cargar las solicitudes."
      );
    } finally {
      if (!silent) {
        setIsLoading(false);
      }
    }
  }

  useEffect(() => {
    void loadSolicitudes();
    const timer = window.setInterval(() => void loadSolicitudes(true), REFRESH_MS);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    const raw = sessionStorage.getItem(PASEADOR_ID_SESSION_KEY);
    const idPaseador = raw ? Number(raw) : NaN;
    if (!Number.isFinite(idPaseador) || idPaseador <= 0) {
      return;
    }

    return subscribeEncuentroTopic({
      topic: `/topic/paseador/${idPaseador}/encuentro`,
      onEvent: (event) => {
        if (!event.idReserva) return;
        const horaInicio = event.horaInicioRegistrada ?? new Date().toISOString();
        setSolicitudes((prev) =>
          prev.map((item) =>
            item.idReserva === event.idReserva
              ? {
                  ...item,
                  estado: "En Curso",
                  fechaInicioReal: horaInicio,
                  trackingActivo: Boolean(event.trackingActivo),
                  chatActivo: Boolean(event.chatActivo)
                }
              : item
          )
        );
        setFeedback({
          type: "success",
          message:
            event.mensajePaseador?.trim() ||
            `Paseo iniciado correctamente para ${event.mascotaNombre ?? "la mascota"}.`
        });
      },
      onError: (message) => {
        setFeedback({
          type: "error",
          message
        });
      }
    });
  }, []);

  const pendingCount = solicitudes.filter((solicitud) => esSolicitudPorResponderLocal(solicitud.estado)).length;
  const acceptedCount = solicitudes.filter((solicitud) => solicitud.estado === "Aceptada").length;
  const inProgressCount = solicitudes.filter((solicitud) => solicitud.estado === "En Curso").length;
  const finishedCount = solicitudes.filter((solicitud) => solicitud.estado === "Finalizada").length;
  const rejectedCount = solicitudes.filter((solicitud) => solicitud.estado === "Rechazada").length;
  const expiredCount = solicitudes.filter((solicitud) => solicitud.estado === "Expirada").length;
  const visibleSolicitudes = useMemo(
    () => solicitudes.filter(activeView.filter),
    [activeView, solicitudes]
  );

  const proximaSolicitudId = useMemo(
    () => findProximaSolicitudId(visibleSolicitudes),
    [visibleSolicitudes]
  );

  const filterTabs: EstadoFilterTab[] = useMemo(
    () =>
      VIEW_CONFIGS.map((view) => ({
        key: view.key,
        label: view.shortLabel,
        count:
          view.key === "solicitadas"
            ? pendingCount
            : view.key === "aceptadas"
              ? acceptedCount
              : view.key === "en-curso"
                ? inProgressCount
                : view.key === "finalizadas"
                  ? finishedCount
                  : view.key === "rechazadas"
                    ? rejectedCount
                    : expiredCount,
        route: view.route,
        end: view.key === "solicitadas"
      })),
    [acceptedCount, expiredCount, finishedCount, inProgressCount, pendingCount, rejectedCount]
  );

  const processingId = processing?.solicitud.idReserva ?? null;

  function openConfirmation(solicitud: SolicitudPendientePaseador, decision: DecisionSolicitud) {
    if (processing) return;
    setFeedback(null);
    setConfirmation({ solicitud, decision });
  }

  // En PaseadorSolicitudes.tsx

const handleVerMapa = async (solicitud: SolicitudPendientePaseador) => {
  const direccion = solicitud.direccionReferencia?.trim() || "Dirección del encuentro";

  if (Number.isFinite(solicitud.latitud) && Number.isFinite(solicitud.longitud)) {
    setMapConfig({
      lat: solicitud.latitud!,
      lng: solicitud.longitud!,
      dir: direccion
    });
    return;
  }

  try {
    setFeedback({ type: "success", message: "Obteniendo coordenadas del mapa..." });

    const { lat, lng } = await geocodeEncuentroAddress({
      direccionReferencia: solicitud.direccionReferencia,
      comuna: solicitud.comuna
    });

    setMapConfig({ lat, lng, dir: direccion });
    setFeedback(null);
  } catch {
    setFeedback({
      type: "error",
      message:
        "No pudimos ubicar la dirección en el mapa. Revisa que calle, número y comuna estén completos en el perfil del tutor."
    });
  }
};

  function handleStartPaseo(solicitud: SolicitudPendientePaseador, startTime: string) {
    setSolicitudes((prev) =>
      prev.map((item) =>
        item.idReserva === solicitud.idReserva
          ? {
              ...item,
              estado: "En Curso",
              fechaInicioReal: startTime,
              trackingActivo: true,
              chatActivo: true
            }
          : item
      )
    );
    setFeedback({
      type: "success",
      message: `Paseo iniciado correctamente para ${solicitud.mascotaNombre}.`
    });
  }

  async function handleFinalizarPaseo(solicitud: SolicitudPendientePaseador) {
    const ok = window.confirm(
      "¿Confirmas que el paseo ya terminó? La reserva pasará a Finalizada y el monto seguirá el flujo de verificación y liberación (N+2) en tu billetera."
    );
    if (!ok) return;

    setFinalizingId(solicitud.idReserva);
    setFeedback(null);
    try {
      await finalizarPaseoPaseador(solicitud.idReserva);
      setSolicitudes((prev) =>
        prev.map((item) =>
          item.idReserva === solicitud.idReserva ? { ...item, estado: "Finalizada" as const } : item
        )
      );
      void loadSolicitudes(true);
      setFeedback({
        type: "success",
        message: `Paseo finalizado para ${solicitud.mascotaNombre}. El saldo quedará en verificación y luego disponible según la política acordada.`
      });
    } catch (error) {
      setFeedback({
        type: "error",
        message:
          error instanceof Error ? error.message : "No se pudo finalizar el paseo. Intentalo de nuevo."
      });
    } finally {
      setFinalizingId(null);
    }
  }

  function handleOpenChat(solicitud: SolicitudPendientePaseador) {
    const puedeChat =
      solicitud.chatActivo ||
      solicitud.estado === "En Curso" ||
      solicitud.estado === "Finalizada";
    if (puedeChat) {
      void requestPermission("chat-entry");
      setActiveChatReservaId(solicitud.idReserva);
      return;
    }
    setFeedback({
      type: "error",
      message: `El chat aún no está habilitado para ${solicitud.mascotaNombre}. Espera la confirmación del encuentro.`
    });
  }

  async function handleConfirmDecision(rechazo: RechazoSolicitudForm) {
    if (!confirmation) return;

    const { solicitud, decision } = confirmation;
    setProcessing(confirmation);
    setFeedback(null);

    try {
      await responderSolicitudPaseador(solicitud.idReserva, {
        decision,
        motivoRechazo: rechazo.motivo || undefined,
        detalleRechazo: rechazo.detalle.trim() || undefined
      });

      try {
        await dispararNotificacion({
          emailDestino: solicitud.tutorCorreo,
          tipoEvento: decision === "ACEPTAR" ? "RESERVA_ACEPTADA" : "RESERVA_RECHAZADA",
          variables: {
            nombreTutor: solicitud.tutorNombre.split(" ")[0],
            nombrePaseador: sessionStorage.getItem("patiperro_nombre_usuario")?.split(" ")[0] || "Tu paseador",
            nombreMascota: solicitud.mascotaNombre,
            fechaPaseo: solicitud.fecha,
            montoTotal: String(solicitud.montoTotal || "0"),
            motivo: decision === "RECHAZAR" ? (rechazo.motivo || "No disponible") : "",
            urlReserva: "http://localhost:5173/login/tutor"
          }
        });
      } catch (emailError) {
        console.error("Error al enviar notificacion:", emailError);
      }

      setSolicitudes((prev) =>
        prev.map((item) =>
          item.idReserva === solicitud.idReserva
            ? {
                ...item,
                estado: decision === "ACEPTAR" ? "Aceptada" : "Rechazada"
              }
            : item
        )
      );
      setConfirmation(null);
      setFeedback({
        type: "success",
        message:
          decision === "ACEPTAR"
            ? "Solicitud aceptada y tutor notificado por correo."
            : "Solicitud rechazada. Se informo al tutor el motivo."
      });
    } catch (error) {
      setFeedback({
        type: "error",
        message:
          error instanceof Error
            ? error.message
            : "No se pudo procesar la decision. Intentalo nuevamente."
      });
    } finally {
      setProcessing(null);
    }
  }

  return (
    <main className={styles.page}>
      <PaseadorNavbar />

      {feedback ? (
        <div className={`${styles.feedback} ${feedback.type === "success" ? styles.feedbackSuccess : styles.feedbackError}`} role="status">
          {feedback.message}
        </div>
      ) : null}

      <section className={styles.listSection}>
        <div className={styles.sectionHeading}>
          <div>
            <p className={styles.cardEyebrow}>Filtrar por estado</p>
            <h2>{activeView.title}</h2>
            <p className={styles.sectionDescription}>{activeView.description}</p>
          </div>
          <span className={styles.resultCount}>
            {visibleSolicitudes.length} {visibleSolicitudes.length === 1 ? "resultado" : "resultados"}
          </span>
        </div>

        <EstadoFilterTabs tabs={filterTabs} ariaLabel="Estados de solicitudes del paseador" mode="route" />

        {isLoading ? (
          <div className={styles.skeletonList} aria-label="Cargando solicitudes">
            {["skeleton-1", "skeleton-2", "skeleton-3"].map((item) => (
              <article key={item} className={styles.skeletonCard}><span /><span /><span /><span /><span /></article>
            ))}
          </div>
        ) : loadError ? (
          <article className={styles.emptyState} role="alert">
            <h3>No pudimos cargar tus solicitudes</h3>
            <p>{loadError}</p>
          </article>
        ) : visibleSolicitudes.length > 0 ? (
          <div className={styles.cardsList}>
            {visibleSolicitudes.map((solicitud) => (
              <SolicitudPendienteCard
                key={solicitud.idReserva}
                solicitud={solicitud}
                destacada={proximaSolicitudId === solicitud.idReserva}
                processingDecision={processingId === solicitud.idReserva ? processing?.decision ?? null : null}
                onAccept={(nextSolicitud) => openConfirmation(nextSolicitud, "ACEPTAR")}
                onReject={(nextSolicitud) => openConfirmation(nextSolicitud, "RECHAZAR")}
                onViewTutor={setSelectedTutorSolicitud}
                onViewMap={handleVerMapa}
                onStartPaseo={handleStartPaseo}
                onOpenChat={handleOpenChat}
                onFinalizarPaseo={handleFinalizarPaseo}
                finalizingPaseo={finalizingId === solicitud.idReserva}
              />
            ))}
          </div>
        ) : (
          <article className={styles.emptyState}>
            <div className={styles.emptyIcon} aria-hidden="true">
              ◌
            </div>
            <h3>{activeView.emptyTitle}</h3>
            <p>{activeView.emptyText}</p>
          </article>
        )}
      </section>

      {/* RENDERIZADO DEL MODAL DEL MAPA */}
      {mapConfig && (
        <MapaEncuentroModal
          lat={mapConfig.lat}
          lng={mapConfig.lng}
          direccion={mapConfig.dir}
          onClose={() => setMapConfig(null)}
        />
      )}

      {confirmation ? (
        <ConfirmarDecisionSolicitudModal
          solicitud={confirmation.solicitud}
          decision={confirmation.decision}
          isSubmitting={processing?.solicitud.idReserva === confirmation.solicitud.idReserva}
          onCancel={() => { if (!processing) setConfirmation(null); }}
          onConfirm={handleConfirmDecision}
        />
      ) : null}

      {selectedTutorSolicitud ? (
        <TutorDetalleModal
          solicitud={selectedTutorSolicitud}
          onClose={() => setSelectedTutorSolicitud(null)}
        />
      ) : null}

      {activeChatReservaId != null ? (
        <ChatWindow
          isOpen
          reservaId={activeChatReservaId}
          currentUserId={Number.isFinite(currentPaseadorId) ? currentPaseadorId : 0}
          currentUserRole="paseador"
          currentUserName={currentPaseadorName}
          counterpartUserId={
            solicitudes.find((item) => item.idReserva === activeChatReservaId)?.idTutorUsuario
          }
          counterpartName={
            solicitudes.find((item) => item.idReserva === activeChatReservaId)?.tutorNombre ||
            "Tutor"
          }
          mascotaNombre={
            solicitudes.find((item) => item.idReserva === activeChatReservaId)?.mascotaNombre ||
            "Mascota"
          }
          canSendPhotos={
            solicitudes.find((item) => item.idReserva === activeChatReservaId)?.estado ===
            "En Curso"
          }
          onClose={() => setActiveChatReservaId(null)}
        />
      ) : null}
    </main>
  );
}
