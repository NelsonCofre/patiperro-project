import { useEffect, useMemo, useState } from "react";
import { NavLink, useLocation } from "react-router-dom";
import ChatWindow from "../../../chat/components/ChatWindow/ChatWindow";
import { usePushNotifications } from "../../../chat/hooks/usePushNotifications";
import { subscribeChatMessages } from "../../../chat/services/chatWs";
import type { ChatToastPayload } from "../../../chat/types/chat.types";
import { buildMessageSnippet } from "../../../chat/utils/chatFormatters";
import { dispararNotificacion } from "../../../tutor/services/notificacionesApi";
import ConfirmarDecisionSolicitudModal from "../../components/ConfirmarDecisionSolicitudModal/ConfirmarDecisionSolicitudModal";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import SolicitudPendienteCard from "../../components/SolicitudPendienteCard/SolicitudPendienteCard";
import TutorDetalleModal from "../../components/TutorDetalleModal/TutorDetalleModal";
// Importamos componentes de Leaflet
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

import {
  fetchSolicitudesPendientesPaseador,
  finalizarPaseoPaseador,
  responderSolicitudPaseador
} from "../../services/solicitudesPaseadorService";
import { PASEADOR_ID_SESSION_KEY } from "../../../../config/api";
import { buildNominatimSearchUrl } from "../../../../config/nominatimSearchUrl";
import { subscribeEncuentroTopic } from "../../../shared/services/encuentroWs";
import type {
  DecisionSolicitud,
  RechazoSolicitudForm,
  SolicitudPendientePaseador
} from "../../types/solicitudPaseador.types";
import styles from "./PaseadorSolicitudes.module.css";

// Fix para los iconos de Leaflet en React
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

const DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41]
});
L.Marker.prototype.options.icon = DefaultIcon;

type ConfirmationState = {
  solicitud: SolicitudPendientePaseador;
  decision: DecisionSolicitud;
};

type FeedbackState = {
  type: "success" | "error";
  message: string;
};

// Componente Interno del Modal del Mapa
function MapaModal({ lat, lng, direccion, onClose }: { lat: number, lng: number, direccion: string, onClose: () => void }) {
  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
      backgroundColor: 'rgba(0,0,0,0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 2000
    }}>
      <div style={{ backgroundColor: 'white', padding: '20px', borderRadius: '12px', width: '90%', maxWidth: '700px', position: 'relative' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
          <h3 style={{ margin: 0 }}>Ubicación del Encuentro</h3>
          <button onClick={onClose} style={{ cursor: 'pointer', border: 'none', background: '#eee', borderRadius: '50%', width: '30px', height: '30px', fontWeight: 'bold' }}>&times;</button>
        </div>
        
        <div style={{ height: '450px', width: '100%', borderRadius: '8px', overflow: 'hidden', border: '1px solid #ddd' }}>
          <MapContainer center={[lat, lng]} zoom={16} scrollWheelZoom style={{ height: '100%', width: '100%' }}>
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            <Marker position={[lat, lng]}>
              <Popup>{direccion}</Popup>
            </Marker>
          </MapContainer>
        </div>
        <p style={{ marginTop: '12px', fontSize: '14px', color: '#444', textAlign: 'center' }}>
          <strong>Dirección:</strong> {direccion}
        </p>
      </div>
    </div>
  );
}
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

function esSolicitudPorResponder(estado: SolicitudPendientePaseador["estado"]): boolean {
  // Regla de negocio: aunque esté PAGADA, el paseador todavía debe aceptar o rechazar.
  return estado === "Solicitada" || estado === "Pagada";
}

const VIEW_CONFIGS: ViewConfig[] = [
  {
    key: "solicitadas",
    title: "Solicitudes por responder",
    description: "Reservas nuevas que aun esperan tu decision.",
    emptyTitle: "No tienes solicitudes por responder",
    emptyText: "Cuando un tutor envie una reserva dentro de tus bloques disponibles, aparecera aqui.",
    filter: (solicitud) => esSolicitudPorResponder(solicitud.estado),
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
const REFRESH_MS = 15000;

function getActiveView(pathname: string): ViewConfig {
  if (pathname.endsWith("/aceptadas")) return VIEW_CONFIGS[1];
  if (pathname.endsWith("/en-curso")) return VIEW_CONFIGS[2];
  if (pathname.endsWith("/rechazadas")) return VIEW_CONFIGS[3];
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
  const [chatToast, setChatToast] = useState<ChatToastPayload | null>(null);

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
    // eslint-disable-next-line react-hooks/exhaustive-deps
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

  useEffect(() => {
    const reservaIds = solicitudes.map((solicitud) => solicitud.idReserva);
    return subscribeChatMessages(reservaIds, (message) => {
      if (
        message.senderUserId === currentPaseadorId ||
        activeChatReservaId === message.idReserva
      ) {
        return;
      }

      setChatToast({
        reservaId: message.idReserva,
        senderName: message.senderName,
        snippet: buildMessageSnippet(message.content)
      });
    });
  }, [activeChatReservaId, currentPaseadorId, solicitudes]);

  useEffect(() => {
    if (!chatToast) return;
    const timer = window.setTimeout(() => setChatToast(null), 5000);
    return () => window.clearTimeout(timer);
  }, [chatToast]);

  const pendingCount = solicitudes.filter((solicitud) => esSolicitudPorResponder(solicitud.estado)).length;
  const acceptedCount = solicitudes.filter((solicitud) => solicitud.estado === "Aceptada").length;
  const inProgressCount = solicitudes.filter((solicitud) => solicitud.estado === "En Curso").length;
  const rejectedCount = solicitudes.filter((solicitud) => solicitud.estado === "Rechazada").length;
  const totalAmount = useMemo(() => {
    const estadosPotencial = new Set<SolicitudPendientePaseador["estado"]>([
      "Solicitada",
      "Pagada",
      "Aceptada",
      "En Curso"
    ]);
    return solicitudes
      .filter((s) => estadosPotencial.has(s.estado))
      .reduce((sum, solicitud) => sum + solicitud.montoTotal, 0);
  }, [solicitudes]);
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

  // En PaseadorSolicitudes.tsx

const handleVerMapa = async (solicitud: SolicitudPendientePaseador) => {
  // 1. Si ya vienen las coordenadas del backend, las usamos directo
  if (solicitud.latitud && solicitud.longitud) {
    setMapConfig({
      lat: solicitud.latitud,
      lng: solicitud.longitud,
      dir: solicitud.direccionReferencia || "Dirección del encuentro"
    });
    return;
  }

  // 2. Si no vienen (como en tu foto), las convertimos al vuelo
  try {
    setFeedback({ type: "success", message: "Obteniendo coordenadas del mapa..." });

    // Usamos Nominatim (OpenStreetMap) igual que en el dashboard del tutor
    const query = encodeURIComponent(`${solicitud.direccionReferencia}, ${solicitud.comuna}, Santiago, Chile`);
    const response = await fetch(buildNominatimSearchUrl(query));
    if (!response.ok) {
      throw new Error(`Geocodificación respondió HTTP ${response.status}`);
    }
    const data = await response.json();

    if (data && data.length > 0) {
      const lat = parseFloat(data[0].lat);
      const lon = parseFloat(data[0].lon);

      setMapConfig({
        lat: lat,
        lng: lon,
        dir: solicitud.direccionReferencia
      });
      setFeedback(null); // Limpiamos el mensaje de carga
    } else {
      throw new Error("No se pudo encontrar la ubicación exacta en el mapa.");
    }
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  } catch (error) {
    setFeedback({
      type: "error",
      message: "Error al convertir la dirección a coordenadas. Intenta buscar la dirección manualmente."
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
      "¿Confirmás que el paseo ya terminó? La reserva pasará a Finalizada y el monto seguirá el flujo de verificación y liberación (N+2) en tu billetera."
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
      setActiveChatReservaId(solicitud.idReserva);
      return;
    }
    setFeedback({
      type: "error",
      message: `El chat aun no esta habilitado para ${solicitud.mascotaNombre}. Espera la confirmacion del encuentro.`
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

      {chatToast ? (
        <button
          type="button"
          className={styles.chatToast}
          onClick={() => {
            void requestPermission("chat-entry");
            setActiveChatReservaId(chatToast.reservaId);
            setChatToast(null);
          }}
        >
          <strong>Nuevo mensaje de {chatToast.senderName}</strong>
          <span>{chatToast.snippet}</span>
        </button>
      ) : null}

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
        <div className={`${styles.feedback} ${feedback.type === "success" ? styles.feedbackSuccess : styles.feedbackError}`} role="status">
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
            <h3>No tienes solicitudes ni encuentros pendientes</h3>
            <p>Cuando un tutor solicite un paseo aparecerá aquí.</p>
            <h3>{activeView.emptyTitle}</h3>
            <p>{activeView.emptyText}</p>
          </article>
        )}
      </section>

      {/* RENDERIZADO DEL MODAL DEL MAPA */}
      {mapConfig && (
        <MapaModal 
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
          onClose={() => setActiveChatReservaId(null)}
        />
      ) : null}
    </main>
  );
}
