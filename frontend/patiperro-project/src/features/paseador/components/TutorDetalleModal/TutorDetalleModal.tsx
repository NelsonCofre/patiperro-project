import { useEffect } from "react";
import type { SolicitudPendientePaseador } from "../../types/solicitudPaseador.types";
import MascotaFotoView from "../MascotaFotoView/MascotaFotoView";
import styles from "./TutorDetalleModal.module.css";

type TutorDetalleModalProps = {
  solicitud: SolicitudPendientePaseador;
  onClose: () => void;
};

const currencyFormatter = new Intl.NumberFormat("es-CL", {
  style: "currency",
  currency: "CLP",
  maximumFractionDigits: 0
});

const dateFormatter = new Intl.DateTimeFormat("es-CL", {
  weekday: "long",
  day: "2-digit",
  month: "long",
  year: "numeric"
});

function formatDateLabel(value: string): string {
  const date = new Date(`${value}T12:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return dateFormatter.format(date);
}

export default function TutorDetalleModal({ solicitud, onClose }: TutorDetalleModalProps) {
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  return (
    <div
      className={styles.overlay}
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="solicitud-detalle-title"
      >
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>Detalle de la solicitud</p>
            <h2 id="solicitud-detalle-title">
              {solicitud.mascotaNombre} con {solicitud.tutorNombre}
            </h2>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            Cerrar
          </button>
        </div>

        <div className={styles.tutorSummary}>
          <img
            src={solicitud.tutorFotoUrl}
            alt={`Foto de ${solicitud.tutorNombre}`}
            className={styles.tutorPhoto}
          />
          <div>
            <span>Tutor</span>
            <h3>{solicitud.tutorNombre}</h3>
            <p>
              {solicitud.tutorComuna} - {solicitud.tutorTelefono}
            </p>
          </div>
        </div>

        <div className={styles.sectionBlock}>
          <h3>Información del paseo</h3>
          <div className={styles.infoGrid}>
            <div>
              <span>Fecha</span>
              <strong>{formatDateLabel(solicitud.fecha)}</strong>
            </div>
            <div>
              <span>Horario</span>
              <strong>
                {solicitud.horaInicio} - {solicitud.horaFin}
              </strong>
            </div>
            <div>
              <span>Monto</span>
              <strong>{currencyFormatter.format(solicitud.montoTotal)}</strong>
            </div>
            <div>
              <span>Estado</span>
              <strong>{solicitud.estado}</strong>
            </div>
            <div>
              <span>Comuna</span>
              <strong>{solicitud.comuna}</strong>
            </div>
            <div>
              <span>Referencia</span>
              <strong>{solicitud.direccionReferencia}</strong>
            </div>
          </div>
        </div>

        <div className={styles.sectionBlock}>
          <h3>Información del tutor</h3>
          <div className={styles.infoGrid}>
            <div>
              <span>Nombre</span>
              <strong>{solicitud.tutorNombre}</strong>
            </div>
            <div>
              <span>Teléfono</span>
              <strong>{solicitud.tutorTelefono}</strong>
            </div>
            <div>
              <span>Correo</span>
              <strong>{solicitud.tutorCorreo}</strong>
            </div>
            <div>
              <span>Dirección</span>
              <strong>{solicitud.tutorDireccion}</strong>
            </div>
          </div>
        </div>

        <div className={styles.sectionBlock}>
          <h3>Información de la mascota</h3>
          <div className={styles.petDetail}>
            <MascotaFotoView
              url={solicitud.mascotaFotoUrl}
              alt={`Foto de ${solicitud.mascotaNombre}`}
              className={styles.petPhoto}
            />
            <div>
              <span>Mascota</span>
              <strong>{solicitud.mascotaNombre}</strong>
              <p>
                {solicitud.mascotaRaza} - {solicitud.mascotaTamano} -{" "}
                {solicitud.mascotaSexo}
              </p>
            </div>
          </div>
          <div className={styles.infoGrid}>
            <div>
              <span>Raza</span>
              <strong>{solicitud.mascotaRaza}</strong>
            </div>
            <div>
              <span>Tamaño</span>
              <strong>{solicitud.mascotaTamano}</strong>
            </div>
            <div>
              <span>Edad</span>
              <strong>{solicitud.mascotaEdad}</strong>
            </div>
            <div>
              <span>Peso</span>
              <strong>{solicitud.mascotaPeso}</strong>
            </div>
          </div>
        </div>

        <div className={styles.noteBox}>
          <span>Caracter</span>
          <p>{solicitud.mascotaCaracter}</p>
        </div>

        <div className={styles.noteBox}>
          <span>Cuidados importantes</span>
          <p>{solicitud.mascotaCuidados}</p>
        </div>

        <div className={styles.noteBox}>
          <span>Notas del tutor</span>
          <p>{solicitud.tutorNotas || "El tutor no agregó notas adicionales."}</p>
        </div>
      </section>
    </div>
  );
}
