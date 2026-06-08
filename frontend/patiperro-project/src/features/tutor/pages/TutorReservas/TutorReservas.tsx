import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { TUTOR_ID_SESSION_KEY } from "../../../../config/api";
import EstadoFilterTabs from "../../../shared/components/EstadoFilterTabs/EstadoFilterTabs";
import type { EstadoFilterTab } from "../../../shared/components/EstadoFilterTabs/EstadoFilterTabs";
import ChatWindow from "../../../chat/components/ChatWindow/ChatWindow";
import PaseoGaleria from "../../../chat/components/PaseoGaleria/PaseoGaleria";
import { usePushNotifications } from "../../../chat/hooks/usePushNotifications";
import CodigoEncuentro from "../../components/CodigoEncuentro/CodigoEncuentro";
import PagoReservaButton from "../../components/PagoReservaButton/PagoReservaButton";
import PaymentSummaryModal from "../../components/PaymentSummaryModal/PaymentSummaryModal";
import ReservaCard from "../../components/ReservaCard/ReservaCard";
import ReservaStepper from "../../components/ReservaStepper/ReservaStepper";
import SaldoRetenidoNotice from "../../components/SaldoRetenidoNotice/SaldoRetenidoNotice";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import { useTutorReservas } from "../../hooks/useTutorReservas";
import type { ReservaTutorDetalleDTO } from "../../types/reservaTutor.types";
import PaseoEnCursoCard from "../../../shared/components/PaseoEnCursoCard/PaseoEnCursoCard";
import { subscribeEncuentroTopic } from "../../../shared/services/encuentroWs";
import { tituloPaseoReserva } from "../../../shared/utils/displayLabels";
import { ResenaModal } from "../../components/ResenaForm/ResenaModal"; // Importación nueva
import {
  formatReservaDate,
  formatReservaMoney,
  formatReservaTime,
  getReservaEstadoMeta,
  matchesTutorReservaFilter,
  type TutorReservaFilterKey
} from "../../utils/reservaEstadoUtils";
import styles from "./TutorReservas.module.css";

const TUTOR_FILTER_CONFIG: Record<
  TutorReservaFilterKey,
  { label: string; description: string; emptyTitle: string; emptyText: string }
> = {
  solicitadas: {
    label: "Solicitadas",
    description: "Reservas enviadas o pagadas que esperan respuesta del paseador.",
    emptyTitle: "No tienes solicitudes pendientes",
    emptyText: "Las reservas nuevas o pagadas aparecerán aquí hasta que el paseador responda."
  },
  aceptadas: {
    label: "Aceptadas",
    description: "Paseos confirmados listos para el encuentro.",
    emptyTitle: "No tienes reservas aceptadas",
    emptyText: "Cuando un paseador acepte tu solicitud, la verás aquí con el código de encuentro."
  },
  en_curso: {
    label: "En curso",
    description: "Paseos que se están realizando en este momento.",
    emptyTitle: "No tienes paseos en curso",
    emptyText: "Cuando el paseo comience, podrás seguirlo desde esta vista."
  },
  finalizadas: {
    label: "Finalizadas",
    description: "Servicios completados listos para calificar o revisar.",
    emptyTitle: "No tienes paseos finalizados",
    emptyText: "Los paseos completados quedarán aquí para consulta y reseñas."
  },
  cerradas: {
    label: "Cerradas",
    description: "Solicitudes rechazadas, canceladas o expiradas.",
    emptyTitle: "No tienes reservas cerradas",
    emptyText: "Las solicitudes que no siguieron adelante aparecerán en este historial."
  }
};

function parseTutorFilter(value: string | null): TutorReservaFilterKey {
  if (
    value === "solicitadas" ||
    value === "aceptadas" ||
    value === "en_curso" ||
    value === "finalizadas" ||
    value === "cerradas"
  ) {
    return value;
  }
  return "solicitadas";
}

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
      helper: "La transacción ya fue confirmada y el dinero sigue resguardado hasta el cierre del paseo.",
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
  const { requestPermission } = usePushNotifications();
  const [searchParams, setSearchParams] = useSearchParams();
  const [reservaParaCalificar, setReservaParaCalificar] = useState<ReservaTutorDetalleDTO | null>(null); // Estado nuevo
  const [selectedReservaId, setSelectedReservaId] = useState<number | null>(null);
  const [activeChatReservaId, setActiveChatReservaId] = useState<number | null>(null);
  const [paymentSummaryReserva, setPaymentSummaryReserva] = useState<ReservaTutorDetalleDTO | null>(null);
  const [showRetencionInfo, setShowRetencionInfo] = useState(false);
  
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

  const activeFilter = parseTutorFilter(searchParams.get("estado"));
  const activeFilterMeta = TUTOR_FILTER_CONFIG[activeFilter];

  const filterTabs: EstadoFilterTab[] = useMemo(
    () => [
      { key: "solicitadas", label: "Solicitadas", count: stats.solicitadas },
      { key: "aceptadas", label: "Aceptadas", count: stats.aceptadas },
      { key: "en_curso", label: "En curso", count: stats.enCurso },
      { key: "finalizadas", label: "Finalizadas", count: stats.finalizadas },
      { key: "cerradas", label: "Cerradas", count: stats.cerradas }
    ],
    [stats]
  );

  const visibleReservas = useMemo(
    () => reservas.filter((reserva) => matchesTutorReservaFilter(activeFilter, reserva)),
    [activeFilter, reservas]
  );

  function handleFilterChange(nextFilter: string) {
    const parsed = parseTutorFilter(nextFilter);
    if (parsed === "solicitadas") {
      setSearchParams({}, { replace: true });
      return;
    }
    setSearchParams({ estado: parsed }, { replace: true });
  }

  const selectedReserva =
    selectedReservaId == null
      ? null
      : reservas.find((r) => r.idReserva === selectedReservaId) ?? null;
  const activeChatReserva =
    activeChatReservaId == null
      ? null
      : reservas.find((r) => r.idReserva === activeChatReservaId) ?? null;
  const selectedEstado = selectedReserva ? getReservaEstadoMeta(selectedReserva) : null;
  const selectedPaymentMeta = selectedReserva ? getPaymentStatusMeta(selectedReserva) : null;
  const tutorIdRaw = sessionStorage.getItem(TUTOR_ID_SESSION_KEY);
  const currentTutorId = tutorIdRaw ? Number(tutorIdRaw) : 0;
  const currentTutorName =
    sessionStorage.getItem("patiperro_nombre_usuario")?.trim() || "Tutor";

  function handleOpenChat(reservaId: number): void {
    void requestPermission("chat-entry");
    setActiveChatReservaId(reservaId);
  }

  useEffect(() => {
    if (!reservas.length) {
      return;
    }
    const reservaIdRaw = searchParams.get("reservaId");
    const openComprobante = searchParams.get("openComprobante") === "1";
    const reservaId = reservaIdRaw && /^\d+$/.test(reservaIdRaw) ? Number(reservaIdRaw) : null;
    if (!reservaId) {
      return;
    }
    const reservaObjetivo = reservas.find((r) => r.idReserva === reservaId);
    if (!reservaObjetivo) {
      return;
    }
    setSelectedReservaId(reservaObjetivo.idReserva);
    if (openComprobante) {
      setPaymentSummaryReserva(reservaObjetivo);
    }
    // Limpiamos query params para que no se reabra en cada refresco.
    setSearchParams({}, { replace: true });
  }, [reservas, searchParams, setSearchParams]);

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
                <h2>{tituloPaseoReserva(selectedReserva)}</h2>
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

            {selectedEstado?.key === "en_curso" ? (
              <>
                <PaseoEnCursoCard
                  variant="compact"
                  statusMessage="El paseo ha comenzado. Tu mascota esta en buenas manos"
                  actorLabel="Paseador"
                  actorNombre={selectedReserva.paseadorNombre}
                  mascotaNombre={selectedReserva.mascotaNombre}
                  horaInicioRegistrada={selectedReserva.fechaInicioReal ?? selectedReserva.horaInicio}
                  comuna={selectedReserva.comuna}
                  locationLabel="Tu dirección de encuentro"
                  locationValue={selectedReserva.direccionReferencia}
                />
                <button
                  type="button"
                  className={styles.modalChatButton}
                  onClick={() => handleOpenChat(selectedReserva.idReserva)}
                >
                  Abrir chat del paseo
                </button>
              </>
            ) : null}

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
                  isPaid={
                    normalizePaymentStatus(selectedReserva.paymentStatus).includes("pagad") ||
                    selectedReserva.idPago != null
                  }
                  amountLabel={formatReservaMoney(selectedReserva.montoTotal)}
                  onUnavailable={() =>
                    setNotice(
                      "El backend aún debe entregar el preferenceId o el enlace seguro de Mercado Pago para completar este checkout."
                    )
                  }
                />

                <SaldoRetenidoNotice
                  message={selectedReserva.mensajeRetencionFondos}
                  onOpenInfo={() => setShowRetencionInfo(true)}
                />

                {(normalizePaymentStatus(selectedReserva.paymentStatus).includes("pagad") ||
                  selectedReserva.idPago != null) ? (
                  <div className={styles.paymentSummaryRow}>
                    <span>Resumen disponible para consulta historica y descarga.</span>
                    <button
                      type="button"
                      className={styles.summaryButton}
                      onClick={() => setPaymentSummaryReserva(selectedReserva)}
                    >
                      Ver resumen de transacción
                    </button>
                  </div>
                ) : null}
              </section>
            ) : null}

            {selectedEstado?.key === "aceptada" ? (
              <CodigoEncuentro codigo={selectedReserva.codigoEncuentro} />
            ) : null}

            {selectedEstado?.key === "finalizada" ? (
              <PaseoGaleria
                reservaId={selectedReserva.idReserva}
                idUsuario={currentTutorId > 0 ? currentTutorId : selectedReserva.idTutorUsuario}
              />
            ) : null}

            <ReservaStepper reserva={selectedReserva} />

            <div className={styles.modalFooter}>
              <span>{formatReservaMoney(selectedReserva.montoTotal)}</span>
            </div>
          </section>
        </div>
      ) : null}

      <section className={styles.reservasSection}>
        <div className={styles.sectionHeader}>
          <div>
            <p className={styles.cardEyebrow}>Mis reservas</p>
            <h2>{activeFilterMeta.label}</h2>
            <p className={styles.sectionSubtext}>{activeFilterMeta.description}</p>
            <p className={styles.refreshMeta}>
              Actualización automática {isRefreshing ? "sincronizando..." : "activa"} · Última revisión:{" "}
              {lastUpdatedLabel}
            </p>
          </div>
          <div className={styles.sectionActions}>
            <button type="button" className={styles.refreshButton} onClick={() => void reload()} disabled={isRefreshing}>
              Actualizar ahora
            </button>
            {error ? <span className={styles.errorText}>{error}</span> : null}
          </div>
        </div>

        <EstadoFilterTabs
          tabs={filterTabs}
          ariaLabel="Estados de reservas del tutor"
          mode="button"
          activeKey={activeFilter}
          onSelect={handleFilterChange}
        />

        {isLoading ? (
          <div className={styles.loadingState}>Cargando reservas...</div>
        ) : visibleReservas.length > 0 ? (
          <div className={styles.reservasList}>
            {visibleReservas.map((reserva) => (
              <ReservaCard
                key={reserva.idReserva}
                reserva={reserva}
                onDetalle={(item) => setSelectedReservaId(item.idReserva)}
                onCancelar={(item) => void cancelarReserva(item)}
                onVerResumenPago={setPaymentSummaryReserva}
                onCalificar={(item) => setReservaParaCalificar(item)}
                onOpenChat={(item) => handleOpenChat(item.idReserva)}
              />
            ))}
          </div>
        ) : reservas.length > 0 ? (
          <article className={styles.emptyState}>
            <strong>{activeFilterMeta.emptyTitle}</strong>
            <p>{activeFilterMeta.emptyText}</p>
          </article>
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
            <strong>{activeFilterMeta.emptyTitle}</strong>
            <p>{activeFilterMeta.emptyText}</p>
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

      {paymentSummaryReserva ? (
        <PaymentSummaryModal
          reserva={paymentSummaryReserva}
          onClose={() => setPaymentSummaryReserva(null)}
        />
      ) : null}

      {activeChatReserva ? (
        <ChatWindow
          isOpen
          reservaId={activeChatReserva.idReserva}
          currentUserId={
            Number.isFinite(currentTutorId) && currentTutorId > 0
              ? currentTutorId
              : activeChatReserva.idTutorUsuario
          }
          currentUserRole="tutor"
          currentUserName={currentTutorName}
          counterpartUserId={activeChatReserva.idPaseador ?? undefined}
          counterpartName={activeChatReserva.paseadorNombre}
          mascotaNombre={activeChatReserva.mascotaNombre}
          onClose={() => setActiveChatReservaId(null)}
        />
      ) : null}

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
