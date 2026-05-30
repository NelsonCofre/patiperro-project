import type { ReservaTutorDetalleDTO } from "../../types/reservaTutor.types";
import {
  formatReservaDate,
  formatReservaMoney,
  formatReservaTime,
  getReservaEstadoMeta,
  isReservaFinalizada,
} from "../../utils/reservaEstadoUtils";
import styles from "./ReservaCard.module.css";

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
};

export default function ReservaCard({
  reserva,
  onDetalle,
  onCalificar,
  onVerResumenPago,
}: Props) {
  const estado = getReservaEstadoMeta(reserva);
  const paymentLabel = getPaymentLabel(reserva);
  const finalizada = isReservaFinalizada(reserva);
  const yaCalificada = reserva.calificada;
  const pagoConfirmado = normalizePaymentStatus(reserva.paymentStatus).includes("pagad") || reserva.idPago != null;

  return (
    <article className={styles.card}>
      <div className={styles.cardHeader}>
        <div>
          <span className={`${styles.statusBadge} ${styles[estado.className]}`}>
            {estado.label}
          </span>
          <p className={styles.statusHelper}>{estado.helper}</p>
        </div>
        <strong className={styles.reservaId}>
          Reserva #{reserva.idReserva}
        </strong>
      </div>

      <div className={styles.mainInfo}>
        <div><span>Paseador</span><strong>{reserva.paseadorNombre}</strong></div>
        <div><span>Mascota</span><strong>{reserva.mascotaNombre}</strong></div>
        <div><span>Fecha</span><strong>{formatReservaDate(reserva.fecha ?? reserva.horaInicio)}</strong></div>
        <div><span>Horario</span><strong>{formatReservaTime(reserva.horaInicio)} - {formatReservaTime(reserva.horaFinal)}</strong></div>
      </div>

      <div className={styles.footer}>
        <div>
          <span>{formatReservaMoney(reserva.montoTotal)}</span>
          <p className={styles.paymentHelper}>{paymentLabel}</p>
        </div>
        <div className={styles.actions}>
          <button type="button" className={styles.secondaryButton} onClick={() => onDetalle(reserva)}>
            Ver detalle
          </button>

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
                "Calificar Paseador"
              )}
            </button>
          )}
        </div>
      </div>
    </article>
  );
}
