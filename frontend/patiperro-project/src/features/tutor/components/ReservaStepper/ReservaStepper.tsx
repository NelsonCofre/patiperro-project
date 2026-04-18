import type { ReservaTutorDetalleDTO } from "../../types/reservaTutor.types";
import { getReservaEstadoMeta, formatReservaDate, formatReservaTime } from "../../utils/reservaEstadoUtils";
import styles from "./ReservaStepper.module.css";

type Props = {
  reserva: ReservaTutorDetalleDTO;
};

export default function ReservaStepper({ reserva }: Props) {
  const estado = getReservaEstadoMeta(reserva);
  const aceptada = ["aceptada", "en_curso", "finalizada"].includes(estado.key);

  const steps = [
    {
      label: "Creada",
      complete: Boolean(reserva.fechaSolicitud),
      time: reserva.fechaSolicitud
    },
    {
      label: "Aceptada",
      complete: aceptada,
      time: null
    },
    {
      label: "Iniciada",
      complete: Boolean(reserva.fechaInicioReal),
      time: reserva.fechaInicioReal
    },
    {
      label: "Finalizada",
      complete: Boolean(reserva.fechaFin) || estado.key === "finalizada",
      time: reserva.fechaFin
    }
  ];

  return (
    <div className={styles.stepper} aria-label="Progreso de la reserva">
      {steps.map((step) => (
        <div
          key={step.label}
          className={`${styles.step} ${step.complete ? styles.stepComplete : ""}`}
        >
          <span className={styles.marker} aria-hidden="true" />
          <div>
            <strong>{step.label}</strong>
            <p>
              {step.time
                ? `${formatReservaDate(step.time)} - ${formatReservaTime(step.time)}`
                : step.complete
                  ? "Hora no registrada"
                  : "Pendiente"}
            </p>
          </div>
        </div>
      ))}
    </div>
  );
}
