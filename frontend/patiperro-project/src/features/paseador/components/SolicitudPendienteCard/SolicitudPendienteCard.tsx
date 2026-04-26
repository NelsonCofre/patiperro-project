import { useState } from "react";
import type {
  DecisionSolicitud,
  SolicitudPendientePaseador
} from "../../types/solicitudPaseador.types";
import CodigoEncuentroValidator from "../CodigoEncuentroValidator/CodigoEncuentroValidator";
import PaseoEnCursoCard from "../../../shared/components/PaseoEnCursoCard/PaseoEnCursoCard";
import styles from "./SolicitudPendienteCard.module.css";

type SolicitudPendienteCardProps = {
  solicitud: SolicitudPendientePaseador;
  processingDecision: DecisionSolicitud | null;
  onAccept: (solicitud: SolicitudPendientePaseador) => void;
  onReject: (solicitud: SolicitudPendientePaseador) => void;
  onViewTutor: (solicitud: SolicitudPendientePaseador) => void;
  onViewMap: (solicitud: SolicitudPendientePaseador) => void;
  onStartPaseo: (solicitud: SolicitudPendientePaseador, startTime: string) => void;
  onOpenChat: (solicitud: SolicitudPendientePaseador) => void;
};

const currencyFormatter = new Intl.NumberFormat("es-CL", {
  style: "currency",
  currency: "CLP",
  maximumFractionDigits: 0
});

const dateFormatter = new Intl.DateTimeFormat("es-CL", {
  weekday: "short",
  day: "2-digit",
  month: "short"
});

function formatDateLabel(value: string): string {
  const date = new Date(`${value}T12:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return dateFormatter.format(date);
}

export default function SolicitudPendienteCard({
  solicitud,
  processingDecision,
  onAccept,
  onReject,
  onViewTutor,
  onViewMap,
  onStartPaseo,
  onOpenChat
}: SolicitudPendienteCardProps) {
  const [codeModalOpen, setCodeModalOpen] = useState(false);
  const isProcessing = processingDecision != null;
  const accepting = processingDecision === "ACEPTAR";
  const rejecting = processingDecision === "RECHAZAR";
  const isSolicitada = solicitud.estado === "Solicitada";
  const isAceptada = solicitud.estado === "Aceptada";
  const isEnCurso = solicitud.estado === "En Curso";

  return (
    <article className={`${styles.card} ${isAceptada ? styles.cardAccepted : ""}`}>
      <div className={`${styles.photoColumn} ${isAceptada ? styles.photoColumnAccepted : ""}`}>
        <img
          src={solicitud.mascotaFotoUrl}
          alt={`Foto de ${solicitud.mascotaNombre}`}
          className={styles.petPhoto}
        />
      </div>

      <div className={styles.content}>
        <div className={styles.header}>
          <div>
            <span className={styles.eyebrow}>Solicitud #{solicitud.idReserva}</span>
            <h3>{solicitud.mascotaNombre}</h3>
            <p>
              {solicitud.mascotaRaza} - {solicitud.mascotaTamano} - Tutor:{" "}
              {solicitud.tutorNombre}
            </p>
          </div>
          <span className={styles.status}>{solicitud.estado}</span>
        </div>

        <div className={styles.detailsGrid}>
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
        </div>

        {/* Sección de Dirección y Mapa */}
        <div className={styles.locationSection}>
          <div className={styles.addressInfo}>
            <span>{solicitud.comuna.toUpperCase()}</span>
            <p>{solicitud.direccionReferencia || "Sin dirección de referencia"}</p>
          </div>
          <button 
            type="button"
            className={styles.verMapaBtn} 
            onClick={() => onViewMap(solicitud)}
          >
            Ver mapa
          </button>
        </div>

        <div className={styles.actions}>
          <button
            type="button"
            className={styles.tutorButton}
            onClick={() => onViewTutor(solicitud)}
            disabled={isProcessing}
          >
            Ver detalle
          </button>
          
          {isSolicitada ? (
            <>
              <button
                type="button"
                className={styles.rejectButton}
                onClick={() => onReject(solicitud)}
                disabled={isProcessing}
              >
                {rejecting ? <span className={styles.spinner} aria-hidden="true" /> : null}
                {rejecting ? "Rechazando..." : "Rechazar"}
              </button>
              <button
                type="button"
                className={styles.acceptButton}
                onClick={() => onAccept(solicitud)}
                disabled={isProcessing}
              >
                {accepting ? <span className={styles.spinner} aria-hidden="true" /> : null}
                {accepting ? "Aceptando..." : "Aceptar"}
              </button>
            </>
          ) : null}
          {isAceptada ? (
            <button
              type="button"
              className={styles.codeButton}
              onClick={() => setCodeModalOpen(true)}
            >
              Ingresar Codigo
            </button>
          ) : null}
        </div>

        {isEnCurso ? (
          <PaseoEnCursoCard
            statusMessage="Paseo iniciado correctamente"
            actorLabel="Tutor"
            actorNombre={solicitud.tutorNombre}
            actorFotoUrl={solicitud.tutorFotoUrl}
            mascotaNombre={solicitud.mascotaNombre}
            horaInicioRegistrada={solicitud.fechaInicioReal ?? solicitud.horaInicio}
            locationLabel="Direccion de inicio"
            locationValue={solicitud.direccionReferencia}
            chatLabel="Abrir chat del paseo"
            onOpenChat={() => onOpenChat(solicitud)}
          />
        ) : null}
      </div>

      {isAceptada && codeModalOpen ? (
        <div className={styles.modalOverlay}>
          <section className={styles.modalCard} role="dialog" aria-modal="true">
            <div className={styles.modalHeader}>
              <div>
                <p className={styles.eyebrow}>Confirmar encuentro</p>
                <h3>Ingresa el codigo del tutor</h3>
              </div>
              <button
                type="button"
                className={styles.modalClose}
                onClick={() => setCodeModalOpen(false)}
              >
                Cerrar
              </button>
            </div>

            <CodigoEncuentroValidator
              solicitud={solicitud}
              onSuccess={(item, startTime) => {
                setCodeModalOpen(false);
                onStartPaseo(item, startTime);
              }}
            />
          </section>
        </div>
      ) : null}
    </article>
  );
}