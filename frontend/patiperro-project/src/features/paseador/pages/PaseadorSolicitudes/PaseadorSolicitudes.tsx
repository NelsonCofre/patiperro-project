import { useEffect, useMemo, useState } from "react";
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

export default function PaseadorSolicitudes() {
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
        setSolicitudes(
          data.filter(
            (solicitud) =>
              solicitud.estado === "Solicitada" ||
              solicitud.estado === "Aceptada" ||
              solicitud.estado === "En Curso"
          )
        );
      } catch (error) {
        if (!active) return;
        setLoadError(
          error instanceof Error ? error.message : "No se pudieron cargar las solicitudes."
        );
      } finally {
        if (active) setIsLoading(false);
      }
    }

    loadSolicitudes();
    return () => {
      active = false;
    };
  }, []);

  const pendingCount = solicitudes.filter((solicitud) => solicitud.estado === "Solicitada").length;
  const acceptedCount = solicitudes.filter((solicitud) => solicitud.estado === "Aceptada").length;
  const inProgressCount = solicitudes.filter((solicitud) => solicitud.estado === "En Curso").length;
  const totalAmount = useMemo(
    () => solicitudes.reduce((sum, solicitud) => sum + solicitud.montoTotal, 0),
    [solicitudes]
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
      // 1. Llamada a la API de Reservas (Backend 8080)
      await responderSolicitudPaseador(solicitud.idReserva, {
        decision,
        motivoRechazo: rechazo.motivo || undefined,
        detalleRechazo: rechazo.detalle.trim() || undefined
      });

      // 2. DISPARAR NOTIFICACIÓN AL TUTOR (Backend 8086) 🚀
      try {
        await dispararNotificacion({
          emailDestino: solicitud.tutorCorreo, // Asegúrate de que el DTO traiga este campo
          tipoEvento: decision === "ACEPTAR" ? "RESERVA_ACEPTADA" : "RESERVA_RECHAZADA",
          variables: {
            nombreTutor: solicitud.tutorNombre,
            nombrePaseador: sessionStorage.getItem("nombreUsuario") || "Tu paseador",
            nombreMascota: solicitud.mascotaNombre,
            fechaPaseo: solicitud.fecha,
            // Si es rechazo, enviamos el motivo al correo
            motivo: decision === "RECHAZAR" ? (rechazo.motivo || "No disponible") : ""
          }
        });
        console.log(`Notificación de ${decision} enviada al tutor.`);
      } catch (emailError) {
        // Logueamos pero no bloqueamos la UI, la reserva ya se actualizó
        console.error("Error al enviar notificación:", emailError);
      }

      // 3. Actualizar UI Local
      setSolicitudes((prev) =>
        prev.filter((item) => item.idReserva !== solicitud.idReserva)
      );
      setConfirmation(null);
      setFeedback({
        type: "success",
        message:
          decision === "ACEPTAR"
            ? "Solicitud aceptada y tutor notificado por correo."
            : "Solicitud rechazada. Se le informó al tutor el motivo."
      });
    } catch (error) {
      setFeedback({
        type: "error",
        message:
          error instanceof Error
            ? error.message
            : "No se pudo procesar la decisión. Inténtalo nuevamente."
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
          <p className={styles.eyebrow}>Solicitudes pendientes</p>
          <h1 className={styles.title}>Acepta o rechaza paseos con seguridad</h1>
          <p className={styles.description}>
            Revisa cada solicitud antes de confirmar. Al aceptar se prepara el flujo de pago; al
            rechazar se deja listo el motivo para informar al tutor.
          </p>
        </div>

        <div className={styles.summaryPanel}>
          <span>Solicitudes activas</span>
          <strong>{pendingCount}</strong>
          <p>
            Aceptadas listas para validar: {acceptedCount}.{" "}
            Paseos en curso: {inProgressCount}.{" "}
            Monto potencial:{" "}
            {new Intl.NumberFormat("es-CL", {
              style: "currency",
              currency: "CLP",
              maximumFractionDigits: 0
            }).format(totalAmount)}
          </p>
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
            <h2>Solicitudes y encuentros</h2>
          </div>
        </div>

        {isLoading ? (
          <div className={styles.skeletonList} aria-label="Cargando solicitudes pendientes">
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
        ) : solicitudes.length > 0 ? (
          <div className={styles.cardsList}>
            {solicitudes.map((solicitud) => (
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
            <h3>No tienes solicitudes ni encuentros pendientes</h3>
            <p>
              Cuando un tutor solicite un paseo dentro de tus bloques disponibles, aparecerá aquí
              para que puedas aceptarlo o rechazarlo.
            </p>
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
