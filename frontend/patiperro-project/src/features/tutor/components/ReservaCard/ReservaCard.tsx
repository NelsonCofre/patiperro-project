import type { ReservaTutorDetalleDTO } from "../../types/reservaTutor.types";
import {
  formatReservaDate,
  formatReservaMoney,
  formatReservaTime,
  getReservaEstadoMeta,
  isReservaFinalizada,
  isReservaSolicitada
} from "../../utils/reservaEstadoUtils";
import styles from "./ReservaCard.module.css";

type Props = {
  reserva: ReservaTutorDetalleDTO;
  onDetalle: (reserva: ReservaTutorDetalleDTO) => void;
  onCancelar: (reserva: ReservaTutorDetalleDTO) => void;
  onCalificar: (reserva: ReservaTutorDetalleDTO) => void;
};

export default function ReservaCard({ reserva, onDetalle, onCancelar, onCalificar }: Props) {
  const estado = getReservaEstadoMeta(reserva);

  return (
    <article className={styles.card}>
      <div className={styles.cardHeader}>
        <div>
          <span className={`${styles.statusBadge} ${styles[estado.className]}`}>
            {estado.label}
          </span>
          <p className={styles.statusHelper}>{estado.helper}</p>
        </div>
        <strong className={styles.reservaId}>Reserva #{reserva.idReserva}</strong>
      </div>

      <div className={styles.mainInfo}>
        <div>
          <span>Paseador</span>
          <strong>{reserva.paseadorNombre}</strong>
        </div>
        <div>
          <span>Mascota</span>
          <strong>{reserva.mascotaNombre}</strong>
        </div>
        <div>
          <span>Fecha</span>
          <strong>{formatReservaDate(reserva.fecha ?? reserva.horaInicio)}</strong>
        </div>
        <div>
          <span>Horario</span>
          <strong>
            {formatReservaTime(reserva.horaInicio)} - {formatReservaTime(reserva.horaFinal)}
          </strong>
        </div>
      </div>

      <div className={styles.footer}>
        <span>{formatReservaMoney(reserva.montoTotal)}</span>
        <div className={styles.actions}>
          <button type="button" className={styles.secondaryButton} onClick={() => onDetalle(reserva)}>
            Ver detalle
          </button>
          {isReservaSolicitada(reserva) ? (
            <button type="button" className={styles.warningButton} onClick={() => onCancelar(reserva)}>
              Cancelar Solicitud
            </button>
          ) : null}
          {isReservaFinalizada(reserva) ? (
            <button type="button" className={styles.primaryButton} onClick={() => onCalificar(reserva)}>
              Calificar Paseador
            </button>
          ) : null}
        </div>
      </div>
    </article>
  );
}
