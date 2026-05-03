import { useEffect, useState } from "react";
import CodigoEncuentro from "../../components/CodigoEncuentro/CodigoEncuentro";
import PagoReservaButton from "../../components/PagoReservaButton/PagoReservaButton";
import ReservaCard from "../../components/ReservaCard/ReservaCard";
import ReservaStepper from "../../components/ReservaStepper/ReservaStepper";
import SaldoRetenidoNotice from "../../components/SaldoRetenidoNotice/SaldoRetenidoNotice";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import { useTutorReservas } from "../../hooks/useTutorReservas";
import type { ReservaTutorDetalleDTO } from "../../types/reservaTutor.types";
import PaseoEnCursoCard from "../../../shared/components/PaseoEnCursoCard/PaseoEnCursoCard";
import { subscribeEncuentroTopic } from "../../../shared/services/encuentroWs";
import { ResenaModal } from "../../components/ResenaForm/ResenaModal"; // Importación nueva
import {
  formatReservaDate,
  formatReservaMoney,
  formatReservaTime,
  getReservaEstadoMeta
} from "../../utils/reservaEstadoUtils";
import styles from "./TutorReservas.module.css";
import { useResena } from "../../hooks/useResena";

function normalizePaymentStatus(value?: string | null): string {
  return (value ?? "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, "_");
}

function getPaymentStatusMeta(reserva: ReservaTutorDetalleDTO): {
  title: string;
  helper: string;
  tag: string;
} {
  const normalized = normalizePaymentStatus(reserva.paymentStatus);

  if (normalized.includes("pagad") || reserva.idPago != null) {
    return {
      title: "Pagada",
      helper: "La transaccion ya fue confirmada y el dinero sigue resguardado hasta el cierre del paseo.",
      tag: "statusPaid"
    };
  }

  if (normalized.includes("pendiente_pago") || normalized.includes("pending_payment")) {
    return {
      title: "Pendiente de pago",
      helper: "La reserva esta lista para que completes el checkout seguro.",
      tag: "statusPending"
    };
  }

  if (normalized.includes("fall")) {
    return {
      title: "Pago no completado",
      helper: "Puedes volver a intentarlo sin perder tu reserva.",
      tag: "statusFailed"
    };
  }

  return {
    title: "Pago disponible",
    helper: "Puedes iniciar el pago desde este detalle cuando quieras confirmar la reserva.",
    tag: "statusReady"
  };
}

export default function TutorReservas() {
  const [setSelectedReserva] = useState<ReservaTutorDetalleDTO | null>(null);
  const [reservaParaCalificar, setReservaParaCalificar] = useState<ReservaTutorDetalleDTO | null>(null); // Estado nuevo
  const [selectedReservaId, setSelectedReservaId] = useState<number | null>(null);
  const [showRetencionInfo, setShowRetencionInfo] = useState(false);
  const { enviarResena } = useResena();
  
  const {
    reservas,
    isLoading,
    isRefreshing,
    error,
    notice,
    setNotice,
    lastUpdatedLabel,
    stats,
    reload,
    cancelarReserva
  } = useTutorReservas();

  const selectedReserva =
    selectedReservaId == null
      ? null
      : reservas.find((r) => r.idReserva === selectedReservaId) ?? null;
  const selectedEstado = selectedReserva ? getReservaEstadoMeta(selectedReserva) : null;
  const selectedPaymentMeta = selectedReserva ? getPaymentStatusMeta(selectedReserva) : null;

  useEffect(() => {
    if (!selectedReserva) return;
    return subscribeEncuentroTopic({
      topic: `/topic/reservas/${selectedReserva.idReserva}/encuentro`,
      onEvent: (event) => {
        setNotice(
          event.mensajeTutor?.trim() ||
            "El paseo ha comenzado. Tu mascota esta en buenas manos."
        );
        void reload();
      }
    });
  }, [reload, selectedReserva, setNotice]);

  return (
    <main className={styles.page}>
      <TutorNavbar />

      {notice ? (
        <div className={styles.notice} role="status">
          <span>{notice}</span>
          <button type="button" onClick={() => setNotice(null)}>
            Cerrar
          </button>
        </div>
      ) : null}

      {/* MODAL DE DETALLE (Existente) */}
      {selectedReserva ? (
        <div className={styles.modalOverlay}>
          <section className={styles.modalCard} role="dialog" aria-modal="true">
            <div className={styles.modalHeader}>
              <div>
                <p className={styles.cardEyebrow}>Detalle de reserva</p>
                <h2>Reserva #{selectedReserva.idReserva}</h2>
              </div>
              <button type="button" onClick={() => setSelectedReservaId(null)}>
                Cerrar
              </button>
            </div>

            <div className={styles.detailGrid}>
              <div>
                <span>Estado</span>
                <strong>{selectedEstado?.label}</strong>
                <p>{selectedEstado?.helper}</p>
              </div>
              <div>
                <span>Paseador</span>
                <strong>{selectedReserva.paseadorNombre}</strong>
              </div>
              <div>
                <span>Mascota</span>
                <strong>{selectedReserva.mascotaNombre}</strong>
              </div>
              <div>
                <span>Fecha y horario</span>
                <strong>{formatReservaDate(selectedReserva.fecha ?? selectedReserva.horaInicio)}</strong>
                <p>
                  {formatReservaTime(selectedReserva.horaInicio)} -{" "}
                  {formatReservaTime(selectedReserva.horaFinal)}
                </p>
              </div>
            </div>

            {selectedPaymentMeta ? (
              <section className={styles.paymentSection}>
                <div className={styles.paymentHeader}>
                  <div>
                    <p className={styles.cardEyebrow}>Pago de la reserva</p>
                    <h3>Checkout y respaldo del servicio</h3>
                  </div>
                  <span className={`${styles.paymentBadge} ${styles[selectedPaymentMeta.tag]}`}>
                    {selectedPaymentMeta.title}
                  </span>
                </div>

                <p className={styles.paymentHelper}>{selectedPaymentMeta.helper}</p>

                <PagoReservaButton
                  preferenceId={selectedReserva.paymentPreferenceId}
                  initPoint={selectedReserva.paymentInitPoint}
                  paymentStatus={selectedReserva.paymentStatus}
                  amountLabel={formatReservaMoney(selectedReserva.montoTotal)}
                  onUnavailable={() =>
                    setNotice(
                      "El backend aun debe entregar el preferenceId o el enlace seguro de Mercado Pago para completar este checkout."
                    )
                  }
                />

                <SaldoRetenidoNotice
                  message={selectedReserva.mensajeRetencionFondos}
                  onOpenInfo={() => setShowRetencionInfo(true)}
                />
              </section>
            ) : null}

            {selectedEstado?.key === "en_curso" ? (
              <PaseoEnCursoCard
                statusMessage="¡El paseo ha comenzado! Tu mascota esta en buenas manos"
                actorLabel="Paseador"
                actorNombre={selectedReserva.paseadorNombre}
                mascotaNombre={selectedReserva.mascotaNombre}
                horaInicioRegistrada={selectedReserva.fechaInicioReal ?? selectedReserva.horaInicio}
                chatLabel="Abrir chat del paseo"
                onOpenChat={() =>
                  setNotice("El chat durante el paseo quedara disponible en una siguiente etapa del MVP.")
                }
              />
            ) : null}

            {selectedEstado?.key === "aceptada" ? (
              <CodigoEncuentro codigo={selectedReserva.codigoEncuentro} />
            ) : null}

            <ReservaStepper reserva={selectedReserva} />

            <div className={styles.modalFooter}>
              <span>{formatReservaMoney(selectedReserva.montoTotal)}</span>
            </div>
          </section>
        </div>
      ) : null}

      <section className={styles.hero}>
        <div>
          <p className={styles.eyebrow}>Mis Reservas</p>
          <h1 className={styles.title}>Estado de tus paseos</h1>
          <p className={styles.description}>
            Revisa tus solicitudes pasadas y futuras, con estado actualizado automaticamente
            durante la sesion.
          </p>
        </div>

        <aside className={styles.refreshPanel}>
          <span>Actualizacion automatica</span>
          <strong>{isRefreshing ? "Sincronizando..." : "Activa"}</strong>
          <p>Ultima revision: {lastUpdatedLabel}. Se consulta el servidor cada 15 segundos.</p>
          <button type="button" onClick={() => void reload()} disabled={isRefreshing}>
            Actualizar ahora
          </button>
        </aside>
      </section>

      <section className={styles.statsGrid}>
        <article>
          <span>Total</span>
          <strong>{stats.total}</strong>
        </article>
        <article>
          <span>Solicitadas</span>
          <strong>{stats.solicitadas}</strong>
        </article>
        <article>
          <span>Aceptadas</span>
          <strong>{stats.aceptadas}</strong>
        </article>
        <article>
          <span>En curso</span>
          <strong>{stats.enCurso}</strong>
        </article>
        <article>
          <span>Finalizadas</span>
          <strong>{stats.finalizadas}</strong>
        </article>
      </section>

      <section className={styles.reservasSection}>
        <div className={styles.sectionHeader}>
          <div>
            <p className={styles.cardEyebrow}>Listado cronologico</p>
            <h2>Todas tus solicitudes</h2>
            <p className={styles.sectionSubtext}>
              El historial conserva tambien la referencia del pago y la politica de saldo retenido.
            </p>
          </div>
          {error ? <span className={styles.errorText}>{error}</span> : null}
        </div>

        {isLoading ? (
          <div className={styles.loadingState}>Cargando reservas...</div>
        ) : reservas.length > 0 ? (
          <div className={styles.reservasList}>
            {reservas.map((reserva) => (
              <ReservaCard
                key={reserva.idReserva}
                reserva={reserva}
                onDetalle={setSelectedReserva}
                onCancelar={(item) => void cancelarReserva(item)}
                onCalificar={(item) => setReservaParaCalificar(item)} // Implementación nueva: abre el modal real
              />
            ))}
          </div>
        ) : error ? (
          <article className={`${styles.emptyState} ${styles.errorState}`}>
            <strong>No se pudieron cargar tus reservas</strong>
            <p>{error}</p>
            <button type="button" onClick={() => void reload()} disabled={isRefreshing}>
              Reintentar
            </button>
          </article>
        ) : (
          <article className={styles.emptyState}>
            <strong>Aun no tienes reservas</strong>
            <p>Cuando solicites un paseo, aparecera en este listado con su estado actual.</p>
          </article>
        )}
      </section>

      {/* MODAL DE RESEÑA EN TUTORRESERVAS */}
      {reservaParaCalificar && (
        <ResenaModal 
          reserva={reservaParaCalificar} 
          onClose={() => {
            setReservaParaCalificar(null);
            void reload(); // Refrescamos la lista para actualizar el booleano 'calificada'
          }} 
        />
      )}

      {/* MODAL DE INFO RETENCION (Existente) */}
      {showRetencionInfo ? (
        <div className={styles.modalOverlay}>
          <section className={styles.infoModalCard} role="dialog" aria-modal="true">
            <div className={styles.modalHeader}>
              <div>
                <p className={styles.cardEyebrow}>Pago seguro</p>
                <h2>Como funciona el saldo retenido</h2>
              </div>
              <button type="button" onClick={() => setShowRetencionInfo(false)}>
                Cerrar
              </button>
            </div>

            <div className={styles.infoModalContent}>
              <p>
                Patiperro retiene temporalmente el dinero del pago para proteger a ambas
                partes durante la ejecucion del servicio.
              </p>
              <ul className={styles.infoList}>
                <li>El cobro se inicia desde una pasarela de pago segura.</li>
                <li>El dinero no se libera al paseador mientras el paseo siga en curso.</li>
                <li>Cuando el servicio finaliza satisfactoriamente, se habilita la liberacion del saldo.</li>
                <li>Si el pago falla o se cancela, puedes reintentar sin perder la reserva.</li>
              </ul>
            </div>
          </section>
        </div>
      ) : null}
    </main>
  );
}