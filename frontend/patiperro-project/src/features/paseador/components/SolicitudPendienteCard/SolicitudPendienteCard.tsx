import { useEffect, useState } from "react";
import type {
  DecisionSolicitud,
  SolicitudPendientePaseador
} from "../../types/solicitudPaseador.types";
import CodigoEncuentroValidator from "../CodigoEncuentroValidator/CodigoEncuentroValidator";
import PaseoEnCursoCard from "../../../shared/components/PaseoEnCursoCard/PaseoEnCursoCard";
import { yaPasoHorarioFinPaseo } from "../../utils/paseoHorarioUtils";
import { getSolicitudEstadoMeta, esSolicitudPorResponder } from "../../utils/solicitudEstadoUtils";
import EstadoBadge from "../../../shared/components/EstadoBadge/EstadoBadge";
import { etiquetaPaseoProgramado } from "../../../shared/utils/displayLabels";
import MascotaFotoView from "../MascotaFotoView/MascotaFotoView";
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
  onFinalizarPaseo?: (solicitud: SolicitudPendientePaseador) => void;
  finalizingPaseo?: boolean;
  destacada?: boolean;
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
  onOpenChat,
  onFinalizarPaseo,
  finalizingPaseo,
  destacada = false
}: SolicitudPendienteCardProps) {
  const [codeModalOpen, setCodeModalOpen] = useState(false);
  const [, setHorarioTick] = useState(0);
  const isProcessing = processingDecision != null;
  const accepting = processingDecision === "ACEPTAR";
  const rejecting = processingDecision === "RECHAZAR";
  // Regla de negocio: PAGADA pero aún no aceptada también debe poder aceptarse/rechazarse.
  const isPorResponder = esSolicitudPorResponder(solicitud.estado);
  const isAceptada = solicitud.estado === "Aceptada";
  const isEnCurso = solicitud.estado === "En Curso";
  const puedeFinalizar = yaPasoHorarioFinPaseo(solicitud.fecha, solicitud.horaFin);
  const estadoMeta = getSolicitudEstadoMeta(solicitud.estado);
  const cardToneClass =
    isEnCurso ? styles.cardEnCurso : isAceptada ? styles.cardAcceptedTone : isPorResponder ? styles.cardPendingTone : "";

  useEffect(() => {
    if (!isEnCurso || puedeFinalizar) return;
    const id = window.setInterval(() => setHorarioTick((t) => t + 1), 30_000);
    return () => window.clearInterval(id);
  }, [isEnCurso, puedeFinalizar, solicitud.fecha, solicitud.horaFin]);

  return (
    <article
      className={`${styles.card} ${isAceptada ? styles.cardAccepted : ""} ${cardToneClass} ${destacada ? styles.cardProxima : ""}`}
    >
      <div className={styles.cardAccent} aria-hidden="true" />
      <div
        className={`${styles.photoColumn} ${isAceptada ? styles.photoColumnAccepted : ""} ${isEnCurso ? styles.photoColumnEnCurso : ""}`}
      >
        <MascotaFotoView
          url={solicitud.mascotaFotoUrl}
          alt={`Foto de ${solicitud.mascotaNombre}`}
          className={styles.petPhoto}
        />
      </div>

      <div className={styles.content}>
        <div className={styles.header}>
          <div>
            <span className={styles.eyebrow}>{etiquetaPaseoProgramado(solicitud.fecha)}</span>
            <h3>{solicitud.mascotaNombre}</h3>
            <p>
              {solicitud.mascotaRaza} · {solicitud.mascotaTamano} · Tutor: {solicitud.tutorNombre}
            </p>
          </div>
          <div className={styles.statusWrap}>
            {destacada ? <span className={styles.proximaBadge}>Proximo</span> : null}
            <EstadoBadge label={estadoMeta.label} badgeClass={estadoMeta.className} />
            <span className={styles.statusHelper}>{estadoMeta.helper}</span>
          </div>
        </div>

        <div className={`${styles.detailsGrid} ${isEnCurso ? styles.detailsGridCompact : ""}`}>
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

        {!isEnCurso ? (
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
        ) : null}

        {isEnCurso ? (
          <div className={styles.enCursoToolbar}>
            <button
              type="button"
              className={styles.tutorButton}
              onClick={() => onViewTutor(solicitud)}
              disabled={isProcessing}
            >
              Ver detalle
            </button>
            <button
              type="button"
              className={styles.mapButton}
              onClick={() => onViewMap(solicitud)}
            >
              Ver mapa
            </button>
            <button
              type="button"
              className={styles.chatButtonOutline}
              onClick={() => onOpenChat(solicitud)}
            >
              Chat
            </button>
            {onFinalizarPaseo ? (
              <button
                type="button"
                className={`${styles.finalizarButtonCompact} ${finalizingPaseo ? styles.finalizarButtonBusy : ""}`}
                disabled={!puedeFinalizar || finalizingPaseo}
                onClick={() => onFinalizarPaseo(solicitud)}
                title={
                  puedeFinalizar
                    ? "Finalizar paseo e iniciar liberación del pago"
                    : `Disponible al terminar el horario (${solicitud.horaFin})`
                }
              >
                {finalizingPaseo ? <span className={styles.spinner} aria-hidden="true" /> : null}
                {finalizingPaseo ? "Finalizando..." : "Finalizar"}
              </button>
            ) : null}
          </div>
        ) : (
        <div className={styles.actions}>
          <button
            type="button"
            className={styles.tutorButton}
            onClick={() => onViewTutor(solicitud)}
            disabled={isProcessing}
          >
            Ver detalle
          </button>
          
          {isPorResponder ? (
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
        )}

        {isEnCurso ? (
          <>
            <PaseoEnCursoCard
              variant="compact"
              statusMessage="Paseo iniciado correctamente"
              actorLabel="Tutor"
              actorNombre={solicitud.tutorNombre}
              actorFotoUrl={solicitud.tutorFotoUrl}
              mascotaNombre={solicitud.mascotaNombre}
              horaInicioRegistrada={solicitud.fechaInicioReal ?? solicitud.horaInicio}
              comuna={solicitud.comuna}
              locationLabel="Encuentro"
              locationValue={solicitud.direccionReferencia}
            />
            {onFinalizarPaseo && !puedeFinalizar ? (
              <p className={styles.finalizarHintCompact}>
                Podrás finalizar el paseo cuando termine el horario ({solicitud.horaFin}).
              </p>
            ) : null}
          </>
        ) : null}
      </div>

      {isAceptada && codeModalOpen ? (
        <div className={styles.modalOverlay}>
          <section className={styles.modalCard} role="dialog" aria-modal="true">
            <div className={styles.modalHeader}>
              <div>
                <p className={styles.eyebrow}>Confirmar encuentro</p>
                <h3>Ingresa el código del tutor</h3>
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