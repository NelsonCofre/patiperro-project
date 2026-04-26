import { useEffect, useMemo, useState } from "react";
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
  responderSolicitudPaseador
} from "../../services/solicitudesPaseadorService";
import type {
  DecisionSolicitud,
  RechazoSolicitudForm,
  SolicitudPendientePaseador
} from "../../types/solicitudPaseador.types";
import styles from "./PaseadorSolicitudes.module.css";

// Fix para los iconos de Leaflet en React
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
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

export default function PaseadorSolicitudes() {
  const [solicitudes, setSolicitudes] = useState<SolicitudPendientePaseador[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  
  // Estados para el Mapa
  const [mapConfig, setMapConfig] = useState<{ lat: number, lng: number, dir: string } | null>(null);

  const [confirmation, setConfirmation] = useState<ConfirmationState | null>(null);
  const [processing, setProcessing] = useState<ConfirmationState | null>(null);
  const [feedback, setFeedback] = useState<FeedbackState | null>(null);
  const [selectedTutorSolicitud, setSelectedTutorSolicitud] = useState<SolicitudPendientePaseador | null>(null);

  useEffect(() => {
    let active = true;

    async function loadSolicitudes() {
      setIsLoading(true);
      setLoadError("");

      try {
        const data = await fetchSolicitudesPendientesPaseador();
        if (!active) return;
        setSolicitudes(
          data.filter((solicitud) => solicitud.estado === "Solicitada" || solicitud.estado === "Aceptada")
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
    const response = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${query}&limit=1`);
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
  } catch (error) {
    setFeedback({
      type: "error",
      message: "Error al convertir la dirección a coordenadas. Intenta buscar la dirección manualmente."
    });
  }
};

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
        console.log(`Notificación de ${decision} enviada al tutor.`);
      } catch (emailError) {
        console.error("Error al enviar notificación:", emailError);
      }

      setSolicitudes((prev) => prev.filter((item) => item.idReserva !== solicitud.idReserva));
      setConfirmation(null);
      setFeedback({
        type: "success",
        message: decision === "ACEPTAR" ? "Solicitud aceptada correctamente." : "Solicitud rechazada correctamente."
      });
    } catch (error) {
      setFeedback({
        type: "error",
        message: error instanceof Error ? error.message : "No se pudo procesar la decisión."
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
        <div className={`${styles.feedback} ${feedback.type === "success" ? styles.feedbackSuccess : styles.feedbackError}`} role="status">
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
          <div className={styles.skeletonList}>
            {["skeleton-1", "skeleton-2", "skeleton-3"].map((item) => (
              <article key={item} className={styles.skeletonCard}><span /><span /><span /><span /><span /></article>
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
                processingDecision={processingId === solicitud.idReserva ? processing?.decision ?? null : null}
                onAccept={(nextSolicitud) => openConfirmation(nextSolicitud, "ACEPTAR")}
                onReject={(nextSolicitud) => openConfirmation(nextSolicitud, "RECHAZAR")}
                onViewTutor={setSelectedTutorSolicitud}
                onViewMap={() => handleVerMapa(solicitud)} // Pasamos la función de mapa aquí
              />
            ))}
          </div>
        ) : (
          <article className={styles.emptyState}>
            <h3>No tienes solicitudes ni encuentros pendientes</h3>
            <p>Cuando un tutor solicite un paseo aparecerá aquí.</p>
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
    </main>
  );
}