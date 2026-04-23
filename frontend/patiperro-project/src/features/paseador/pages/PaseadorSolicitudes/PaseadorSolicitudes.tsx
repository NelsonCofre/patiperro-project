import { useEffect, useMemo, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { dispararNotificacion } from "../../../tutor/services/notificacionesApi";
import ConfirmarDecisionSolicitudModal from "../../components/ConfirmarDecisionSolicitudModal/ConfirmarDecisionSolicitudModal";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import SolicitudPendienteCard from "../../components/SolicitudPendienteCard/SolicitudPendienteCard";
import TutorDetalleModal from "../../components/TutorDetalleModal/TutorDetalleModal";
import {
  fetchSolicitudesPendientesPaseador,
  responderSolicitudPaseador
} from "../../services/solicitudesPaseadorService";
import type {
  DecisionSolicitud,
  RechazoSolicitudForm,
  SolicitudPendientePaseador
} from "../../types/solicitudPaseador.types";
import styles from "./PaseadorSolicitudes.module.css";

type ConfirmationState = {
  solicitud: SolicitudPendientePaseador;
  decision: DecisionSolicitud;
};

type FeedbackState = {
  type: "success" | "error";
  message: string;
};

type ViewKey = "solicitadas" | "aceptadas" | "en-curso" | "rechazadas";

type ViewConfig = {
  key: ViewKey;
  title: string;
  description: string;
  emptyTitle: string;
  emptyText: string;
  filter: (solicitud: SolicitudPendientePaseador) => boolean;
  route: string;
};

const VIEW_CONFIGS: ViewConfig[] = [
  {
    key: "solicitadas",
    title: "Solicitudes por responder",
    description: "Reservas nuevas que aun esperan tu decision.",
    emptyTitle: "No tienes solicitudes por responder",
    emptyText: "Cuando un tutor envie una reserva dentro de tus bloques disponibles, aparecera aqui.",
    filter: (solicitud) => solicitud.estado === "Solicitada",
    route: "/paseador/dashboard/solicitudes"
  },
  {
    key: "aceptadas",
    title: "Reservas aceptadas",
    description: "Reservas listas para validar el codigo e iniciar el paseo.",
    emptyTitle: "No tienes reservas aceptadas",
    emptyText: "Las reservas que aceptes apareceran aqui para continuar con el encuentro.",
    filter: (solicitud) => solicitud.estado === "Aceptada",
    route: "/paseador/dashboard/solicitudes/aceptadas"
  },
  {
    key: "en-curso",
    title: "Paseos en curso",
    description: "Servicios que ya fueron confirmados y se estan realizando ahora.",
    emptyTitle: "No tienes paseos en curso",
    emptyText: "Cuando confirmes el inicio de un paseo, quedara visible aqui con acceso al chat.",
    filter: (solicitud) => solicitud.estado === "En Curso",
    route: "/paseador/dashboard/solicitudes/en-curso"
  },
  {
    key: "rechazadas",
    title: "Solicitudes rechazadas",
    description: "Historial reciente de reservas que decidiste no tomar.",
    emptyTitle: "No tienes solicitudes rechazadas",
    emptyText: "Las solicitudes rechazadas seguiran visibles aqui como referencia.",
    filter: (solicitud) => solicitud.estado === "Rechazada",
    route: "/paseador/dashboard/solicitudes/rechazadas"
  }
];

function getActiveView(pathname: string): ViewConfig {
  if (pathname.endsWith("/aceptadas")) return VIEW_CONFIGS[1];
  if (pathname.endsWith("/en-curso")) return VIEW_CONFIGS[2];
  if (pathname.endsWith("/rechazadas")) return VIEW_CONFIGS[3];
  return VIEW_CONFIGS[0];
}

export default function PaseadorSolicitudes() {
  const location = useLocation();
  const activeView = useMemo(() => getActiveView(location.pathname), [location.pathname]);

  const [solicitudes, setSolicitudes] = useState<SolicitudPendientePaseador[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [confirmation, setConfirmation] = useState<ConfirmationState | null>(null);
  const [processing, setProcessing] = useState<ConfirmationState | null>(null);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [selectedTutorSolicitud, setSelectedTutorSolicitud] =
    useState<SolicitudPendientePaseador | null>(null);

  useEffect(() => {
    let active = true;

    async function loadSolicitudes() {
      setIsLoading(true);
      setLoadError("");

      try {
        const data = await fetchSolicitudesPendientesPaseador();
        if (!active) return;
        setSolicitudes(data);
      } catch (error) {
        if (!active) return;
        setLoadError(
          error instanceof Error ? error.message : "No se pudieron cargar las solicitudes."
        );
      } finally {
        if (active) setIsLoading(false);
      }
    }

    void loadSolicitudes();
    return () => {
      active = false;
    };
  }, []);

  const pendingCount = solicitudes.filter((solicitud) => solicitud.estado === "Solicitada").length;
  const acceptedCount = solicitudes.filter((solicitud) => solicitud.estado === "Aceptada").length;
  const inProgressCount = solicitudes.filter((solicitud) => solicitud.estado === "En Curso").length;
  const rejectedCount = solicitudes.filter((solicitud) => solicitud.estado === "Rechazada").length;
  const totalAmount = useMemo(
    () => solicitudes.reduce((sum, solicitud) => sum + solicitud.montoTotal, 0),
    [solicitudes]
  );
  const visibleSolicitudes = useMemo(
    () => solicitudes.filter(activeView.filter),
    [activeView, solicitudes]
  );

  const processingId = processing?.solicitud.idReserva ?? null;

  function openConfirmation(solicitud: SolicitudPendientePaseador, decision: DecisionSolicitud) {
    if (processing) return;
    setFeedback(null);
    setConfirmation({ solicitud, decision });
  }

  function handleViewMap(solicitud: SolicitudPendientePaseador) {
    setFeedback({
      type: "success",
      message: `Mapa pendiente de integrar para ${solicitud.direccionReferencia}.`
    });
  }

  function handleStartPaseo(solicitud: SolicitudPendientePaseador, startTime: string) {
    setSolicitudes((prev) =>
      prev.map((item) =>
        item.idReserva === solicitud.idReserva
          ? { ...item, estado: "En Curso", fechaInicioReal: startTime }
          : item
      )
    );
    setFeedback({
      type: "success",
      message: `Paseo iniciado correctamente para ${solicitud.mascotaNombre}.`
    });
  }

  function handleOpenChat(solicitud: SolicitudPendientePaseador) {
    setFeedback({
      type: "success",
      message: `El chat del paseo para ${solicitud.mascotaNombre} quedo habilitado para una siguiente etapa del MVP.`
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
            nombreTutor: solicitud.tutorNombre,
            nombrePaseador: sessionStorage.getItem("nombreUsuario") || "Tu paseador",
            nombreMascota: solicitud.mascotaNombre,
            fechaPaseo: solicitud.fecha,
            motivo: decision === "RECHAZAR" ? rechazo.motivo || "No disponible" : ""
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

      <section className={styles.hero}>
        <div>
          <p className={styles.eyebrow}>Solicitudes del paseador</p>
          <h1 className={styles.title}>Resumen de solicitudes</h1>
          <p className={styles.description}>
            Revisa el estado general de tus reservas y entra a cada vista para responder,
            validar encuentros o seguir el progreso del paseo.
          </p>
        </div>

        <div className={styles.summaryPanel}>
          <div className={styles.summaryHeader}>
            <span>Dashboard de solicitudes</span>
            <strong>{solicitudes.length}</strong>
            <p>Vista general de tus reservas y su estado actual.</p>
          </div>

          <div className={styles.metricsGrid}>
            <article className={styles.metricCard}>
              <span>Pendientes</span>
              <strong>{pendingCount}</strong>
            </article>
            <article className={styles.metricCard}>
              <span>Aceptadas</span>
              <strong>{acceptedCount}</strong>
            </article>
            <article className={styles.metricCard}>
              <span>En curso</span>
              <strong>{inProgressCount}</strong>
            </article>
            <article className={styles.metricCard}>
              <span>Rechazadas</span>
              <strong>{rejectedCount}</strong>
            </article>
          </div>

          <div className={styles.summaryFooter}>
            <span>Monto potencial</span>
            <strong>
              {new Intl.NumberFormat("es-CL", {
                style: "currency",
                currency: "CLP",
                maximumFractionDigits: 0
              }).format(totalAmount)}
            </strong>
          </div>
        </div>
      </section>

      {feedback ? (
        <div
          className={`${styles.feedback} ${
            feedback.type === "success" ? styles.feedbackSuccess : styles.feedbackError
          }`}
          role="status"
        >
          {feedback.message}
        </div>
      ) : null}

      <section className={styles.listSection}>
        <div className={styles.sectionHeading}>
          <div>
            <p className={styles.cardEyebrow}>Panel del paseador</p>
            <h2>{activeView.title}</h2>
          </div>
        </div>

        <nav className={styles.stateTabs} aria-label="Estados de solicitudes">
          {VIEW_CONFIGS.map((view) => (
            <NavLink
              key={view.key}
              to={view.route}
              end={view.key === "solicitadas"}
              className={({ isActive }) => `${styles.stateTab} ${isActive ? styles.stateTabActive : ""}`}
            >
              {view.title}
            </NavLink>
          ))}
        </nav>

        {isLoading ? (
          <div className={styles.skeletonList} aria-label="Cargando solicitudes">
            {["skeleton-1", "skeleton-2", "skeleton-3"].map((item) => (
              <article key={item} className={styles.skeletonCard}>
                <span />
                <span />
                <span />
                <span />
              </article>
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
                processingDecision={
                  processingId === solicitud.idReserva ? processing?.decision ?? null : null
                }
                onAccept={(nextSolicitud) => openConfirmation(nextSolicitud, "ACEPTAR")}
                onReject={(nextSolicitud) => openConfirmation(nextSolicitud, "RECHAZAR")}
                onViewTutor={setSelectedTutorSolicitud}
                onViewMap={handleViewMap}
                onStartPaseo={handleStartPaseo}
                onOpenChat={handleOpenChat}
              />
            ))}
          </div>
        ) : (
          <article className={styles.emptyState}>
            <h3>{activeView.emptyTitle}</h3>
            <p>{activeView.emptyText}</p>
          </article>
        )}
      </section>

      {confirmation ? (
        <ConfirmarDecisionSolicitudModal
          solicitud={confirmation.solicitud}
          decision={confirmation.decision}
          isSubmitting={processing?.solicitud.idReserva === confirmation.solicitud.idReserva}
          onCancel={() => {
            if (!processing) setConfirmation(null);
          }}
          onConfirm={handleConfirmDecision}
        />
      ) : null}

      {selectedTutorSolicitud ? (
        <TutorDetalleModal
          solicitud={selectedTutorSolicitud}
          onClose={() => setSelectedTutorSolicitud(null)}
        />
      ) : null}
    </main>
  );
}
