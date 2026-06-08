import { useState } from "react";
import type { ReservaTutorDetalleDTO } from "../../types/reservaTutor.types";
import {
  formatReservaDate,
  formatReservaMoney,
  formatReservaTime,
  formatDireccionEncuentro,
  getReservaEstadoMeta,
  isReservaFinalizada,
  tieneDireccionEncuentro,
} from "../../utils/reservaEstadoUtils";
import EstadoBadge from "../../../shared/components/EstadoBadge/EstadoBadge";
import MapaEncuentroModal from "../../../shared/components/MapaEncuentroModal/MapaEncuentroModal";
import PaseoEnCursoCard from "../../../shared/components/PaseoEnCursoCard/PaseoEnCursoCard";
import { geocodeEncuentroAddress } from "../../../shared/utils/geocodeAddress";
import { formatFechaCorta, FALLBACK_PASEADOR } from "../../../shared/utils/displayLabels";
import CodigoEncuentroModal from "../CodigoEncuentro/CodigoEncuentroModal";
import styles from "./ReservaCard.module.css";

function getInitials(name: string): string {
  return (
    name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? "")
      .join("") || "P"
  );
}

function normalizePaymentStatus(value?: string | null): string {
  return (value ?? "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, "_");
}

function getPaymentLabel(reserva: ReservaTutorDetalleDTO): string {
  const status = normalizePaymentStatus(reserva.paymentStatus);
  if (status.includes("pagad") || reserva.idPago != null) return "Pago confirmado";
  if (status.includes("pendiente_pago") || status.includes("pending_payment")) return "Pendiente de pago";
  if (status.includes("fall")) return "Pago no completado";
  return "Pago disponible";
}

type Props = {
  reserva: ReservaTutorDetalleDTO;
  onDetalle: (reserva: ReservaTutorDetalleDTO) => void;
  onCancelar: (reserva: ReservaTutorDetalleDTO) => void;
  onCalificar: (reserva: ReservaTutorDetalleDTO) => void;
  onVerResumenPago: (reserva: ReservaTutorDetalleDTO) => void;
  onOpenChat?: (reserva: ReservaTutorDetalleDTO) => void;
};

export default function ReservaCard({
  reserva,
  onDetalle,
  onCalificar,
  onVerResumenPago,
  onOpenChat,
}: Props) {
  const [mapConfig, setMapConfig] = useState<{ lat: number; lng: number; dir: string } | null>(null);
  const [mapLoading, setMapLoading] = useState(false);
  const [showCodigo, setShowCodigo] = useState(false);

  const estado = getReservaEstadoMeta(reserva);
  const paymentLabel = getPaymentLabel(reserva);
  const finalizada = isReservaFinalizada(reserva);
  const yaCalificada = reserva.calificada;
  const pagoConfirmado = normalizePaymentStatus(reserva.paymentStatus).includes("pagad") || reserva.idPago != null;
  const enCurso = estado.key === "en_curso";
  const aceptada = estado.key === "aceptada";
  const direccionEncuentro = formatDireccionEncuentro(reserva.comuna, reserva.direccionReferencia);
  const paseadorFotoUrl = reserva.paseadorFotoUrl?.trim() || null;
  const cardToneClass =
    enCurso
      ? styles.cardEnCurso
      : estado.key === "finalizada"
        ? styles.cardFinalizada
        : estado.key === "rechazada" || estado.key === "cancelada" || estado.key === "expirada"
          ? styles.cardCerrada
          : "";

  async function handleVerMapa() {
    if (mapLoading) return;
    setMapLoading(true);
    try {
      const { lat, lng } = await geocodeEncuentroAddress({
        direccionReferencia: reserva.direccionReferencia,
        comuna: reserva.comuna
      });
      setMapConfig({ lat, lng, dir: direccionEncuentro });
    } catch {
      window.alert(
        "No pudimos ubicar la dirección en el mapa. Revisa que calle, número y comuna estén completos en tu perfil."
      );
    } finally {
      setMapLoading(false);
    }
  }

  return (
    <>
      <article className={`${styles.card} ${cardToneClass} ${enCurso ? styles.cardEnCursoLayout : ""}`}>
        <div className={styles.cardAccent} aria-hidden="true" />

        {enCurso ? (
          <div className={styles.photoColumn}>
            {paseadorFotoUrl ? (
              <img
                src={paseadorFotoUrl}
                alt={`Foto de ${reserva.paseadorNombre || FALLBACK_PASEADOR}`}
                className={styles.paseadorPhoto}
              />
            ) : (
              <div className={styles.photoFallback} aria-hidden="true">
                {getInitials(reserva.paseadorNombre || FALLBACK_PASEADOR)}
              </div>
            )}
          </div>
        ) : null}

        <div className={styles.cardBody}>
        <div className={styles.cardHeader}>
          <div className={styles.headerMain}>
            <EstadoBadge label={estado.label} badgeClass={estado.className} />
            <p className={styles.statusHelper}>{estado.helper}</p>
          </div>
          <div className={styles.headerAside}>
            <span className={styles.reservaId}>
              {formatFechaCorta(reserva.fecha ?? reserva.horaInicio)}
            </span>
            <strong className={styles.amount}>{formatReservaMoney(reserva.montoTotal)}</strong>
          </div>
        </div>

        <div className={`${styles.mainInfo} ${enCurso ? styles.mainInfoCompact : ""}`}>
          <div className={styles.infoTile}>
            <span>Paseador</span>
            <strong>{reserva.paseadorNombre}</strong>
          </div>
          <div className={styles.infoTile}>
            <span>Mascota</span>
            <strong>{reserva.mascotaNombre}</strong>
          </div>
          <div className={styles.infoTile}>
            <span>Fecha</span>
            <strong>{formatReservaDate(reserva.fecha ?? reserva.horaInicio)}</strong>
          </div>
          <div className={styles.infoTile}>
            <span>Horario</span>
            <strong>
              {formatReservaTime(reserva.horaInicio)} - {formatReservaTime(reserva.horaFinal)}
            </strong>
          </div>
        </div>

        {enCurso ? (
          <>
            <div className={styles.enCursoToolbar}>
              <button type="button" className={styles.secondaryButton} onClick={() => onDetalle(reserva)}>
                Ver detalle
              </button>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => void handleVerMapa()}
                disabled={mapLoading}
              >
                {mapLoading ? "Cargando mapa..." : "Ver mapa"}
              </button>
              {onOpenChat ? (
                <button type="button" className={styles.chatButton} onClick={() => onOpenChat(reserva)}>
                  Chat
                </button>
              ) : null}
            </div>

            <PaseoEnCursoCard
              variant="compact"
              statusMessage="El paseo ha comenzado. Tu mascota está en buenas manos."
              actorLabel="Paseador"
              actorNombre={reserva.paseadorNombre}
              actorFotoUrl={paseadorFotoUrl}
              mascotaNombre={reserva.mascotaNombre}
              horaInicioRegistrada={reserva.fechaInicioReal ?? reserva.horaInicio}
              comuna={reserva.comuna}
              locationLabel="Encuentro"
              locationValue={reserva.direccionReferencia}
            />

            {!tieneDireccionEncuentro(reserva.comuna, reserva.direccionReferencia) ? (
              <p className={styles.addressHint}>
                Completa tu dirección en el perfil para orientar al paseador.
              </p>
            ) : null}
          </>
        ) : null}

        <div className={styles.footer}>
          <p className={styles.paymentHelper}>{paymentLabel}</p>
          <div className={styles.actions}>
            {!enCurso ? (
              <button type="button" className={styles.secondaryButton} onClick={() => onDetalle(reserva)}>
                Ver detalle
              </button>
            ) : null}

            {aceptada ? (
              <button type="button" className={styles.codeButton} onClick={() => setShowCodigo(true)}>
                Ver código
              </button>
            ) : null}

            {pagoConfirmado ? (
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => onVerResumenPago(reserva)}
              >
                Resumen pago
              </button>
            ) : null}

            {finalizada && (
              <button
                type="button"
                disabled={yaCalificada}
                aria-disabled={yaCalificada}
                title={yaCalificada ? "Ya calificaste este paseo" : "Calificar al paseador"}
                className={yaCalificada ? styles.ratedButton : styles.primaryButton}
                onClick={() => !yaCalificada && onCalificar(reserva)}
              >
                {yaCalificada ? (
                  <>
                    <span className={styles.ratedButtonIcon} aria-hidden="true">
                      ✓
                    </span>
                    Reseña enviada
                  </>
                ) : (
                  "Calificar paseador"
                )}
              </button>
            )}
          </div>
        </div>
        </div>
      </article>

      {mapConfig ? (
        <MapaEncuentroModal
          lat={mapConfig.lat}
          lng={mapConfig.lng}
          direccion={mapConfig.dir}
          onClose={() => setMapConfig(null)}
        />
      ) : null}

      {showCodigo ? (
        <CodigoEncuentroModal
          codigo={reserva.codigoEncuentro}
          onClose={() => setShowCodigo(false)}
        />
      ) : null}
    </>
  );
}
