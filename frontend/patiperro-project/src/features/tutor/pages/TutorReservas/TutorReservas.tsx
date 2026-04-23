import { useState } from "react";
import CodigoEncuentro from "../../components/CodigoEncuentro/CodigoEncuentro";
import ReservaCard from "../../components/ReservaCard/ReservaCard";
import ReservaStepper from "../../components/ReservaStepper/ReservaStepper";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import { useTutorReservas } from "../../hooks/useTutorReservas";
import type { ReservaTutorDetalleDTO } from "../../types/reservaTutor.types";
import PaseoEnCursoCard from "../../../shared/components/PaseoEnCursoCard/PaseoEnCursoCard";
import {
  formatReservaDate,
  formatReservaMoney,
  formatReservaTime,
  getReservaEstadoMeta
} from "../../utils/reservaEstadoUtils";
import styles from "./TutorReservas.module.css";

export default function TutorReservas() {
  const [selectedReserva, setSelectedReserva] = useState<ReservaTutorDetalleDTO | null>(null);
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

  const selectedEstado = selectedReserva ? getReservaEstadoMeta(selectedReserva) : null;

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

      {selectedReserva ? (
        <div className={styles.modalOverlay}>
          <section className={styles.modalCard} role="dialog" aria-modal="true">
            <div className={styles.modalHeader}>
              <div>
                <p className={styles.cardEyebrow}>Detalle de reserva</p>
                <h2>Reserva #{selectedReserva.idReserva}</h2>
              </div>
              <button type="button" onClick={() => setSelectedReserva(null)}>
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
                onCalificar={() => setNotice("La calificacion del paseador quedara para una siguiente etapa del MVP.")}
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
    </main>
  );
}
