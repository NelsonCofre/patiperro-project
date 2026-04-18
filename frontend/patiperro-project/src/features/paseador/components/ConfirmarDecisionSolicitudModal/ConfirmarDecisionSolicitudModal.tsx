import { useEffect, useState } from "react";
import type {
  DecisionSolicitud,
  MotivoRechazo,
  RechazoSolicitudForm,
  SolicitudPendientePaseador
} from "../../types/solicitudPaseador.types";
import styles from "./ConfirmarDecisionSolicitudModal.module.css";

type ConfirmarDecisionSolicitudModalProps = {
  solicitud: SolicitudPendientePaseador;
  decision: DecisionSolicitud;
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: (rechazo: RechazoSolicitudForm) => void;
};

const REJECTION_REASONS: MotivoRechazo[] = [
  "Emergencia personal",
  "Mascota no compatible",
  "Horario no disponible",
  "Otro"
];

export default function ConfirmarDecisionSolicitudModal({
  solicitud,
  decision,
  isSubmitting,
  onCancel,
  onConfirm
}: ConfirmarDecisionSolicitudModalProps) {
  const [rechazo, setRechazo] = useState<RechazoSolicitudForm>({
    motivo: "",
    detalle: ""
  });
  const isRejecting = decision === "RECHAZAR";
  const actionLabel = isRejecting ? "rechazar" : "aceptar";

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !isSubmitting) {
        onCancel();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isSubmitting, onCancel]);

  return (
    <div className={styles.overlay} onMouseDown={isSubmitting ? undefined : onCancel}>
      <section
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirmar-decision-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <span className={isRejecting ? styles.rejectEyebrow : styles.acceptEyebrow}>
          Confirmar decisión
        </span>
        <h2 id="confirmar-decision-title">
          ¿Estás seguro de que deseas {actionLabel} este paseo?
        </h2>
        <p>
          Solicitud de {solicitud.tutorNombre} para pasear a {solicitud.mascotaNombre}.
          Esta acción actualizará la vista de solicitudes pendientes.
        </p>

        {isRejecting ? (
          <div className={styles.rejectFields}>
            <label>
              Motivo del rechazo
              <select
                value={rechazo.motivo}
                onChange={(event) =>
                  setRechazo((prev) => ({
                    ...prev,
                    motivo: event.target.value as MotivoRechazo | ""
                  }))
                }
                disabled={isSubmitting}
              >
                <option value="">Selecciona un motivo opcional</option>
                {REJECTION_REASONS.map((reason) => (
                  <option key={reason} value={reason}>
                    {reason}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Detalle para el tutor
              <textarea
                value={rechazo.detalle}
                onChange={(event) =>
                  setRechazo((prev) => ({ ...prev, detalle: event.target.value }))
                }
                placeholder="Ej: Hoy tuve una emergencia y no podré tomar este paseo."
                disabled={isSubmitting}
              />
            </label>
          </div>
        ) : null}

        <div className={styles.actions}>
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={onCancel}
            disabled={isSubmitting}
          >
            Cancelar
          </button>
          <button
            type="button"
            className={isRejecting ? styles.rejectButton : styles.acceptButton}
            onClick={() => onConfirm(rechazo)}
            disabled={isSubmitting}
          >
            {isSubmitting ? <span className={styles.spinner} aria-hidden="true" /> : null}
            {isSubmitting ? "Procesando..." : isRejecting ? "Confirmar rechazo" : "Confirmar aceptación"}
          </button>
        </div>
      </section>
    </div>
  );
}
