import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { PaseadorHome } from "../../types/paseadorHome.types";
import { fetchAgendaOfertaPaseador, type AgendaBloqueOfertaDTO } from "../../services/reservaTutorApi";
import styles from "./PerfilPaseadorModal.module.css";

type PerfilPaseadorModalProps = {
  paseador: PaseadorHome;
  onClose: () => void;
};

function toDateSafe(fecha: string, hora: string): Date | null {
  const normalizedFecha = (fecha ?? "").trim();
  const normalizedHora = (hora ?? "").trim();
  const candidates = [
    normalizedHora,
    normalizedFecha,
    normalizedFecha && normalizedHora && !normalizedHora.includes("T")
      ? `${normalizedFecha}T${normalizedHora}`
      : ""
  ].filter(Boolean);

  for (const candidate of candidates) {
    const parsed = new Date(candidate);
    if (!Number.isNaN(parsed.getTime())) return parsed;
  }

  return null;
}

export default function PerfilPaseadorModal({ paseador, onClose }: PerfilPaseadorModalProps) {
  const navigate = useNavigate();
  const [bloques, setBloques] = useState<AgendaBloqueOfertaDTO[]>([]);
  const [loadingBloques, setLoadingBloques] = useState(false);
  const [bloquesError, setBloquesError] = useState<string | null>(null);

  // Bloquear el scroll del body cuando el modal está abierto
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = 'unset';
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose]);

  useEffect(() => {
    let active = true;

    async function loadBloques() {
      setLoadingBloques(true);
      setBloquesError(null);

      try {
        const today = new Date();
        const until = new Date(today);
        until.setDate(until.getDate() + 14);
        const desde = today.toISOString().slice(0, 10);
        const hasta = until.toISOString().slice(0, 10);
        const idPaseador = Number.parseInt(paseador.id, 10);
        if (!Number.isFinite(idPaseador)) {
          throw new Error("No se pudo identificar el paseador seleccionado.");
        }
        const agenda = await fetchAgendaOfertaPaseador(idPaseador, desde, hasta);
        if (!active) return;
        setBloques(agenda);
      } catch (error) {
        if (!active) return;
        const message =
          error instanceof Error ? error.message : "No se pudo cargar la disponibilidad del paseador.";
        setBloquesError(message);
      } finally {
        if (active) setLoadingBloques(false);
      }
    }

    loadBloques();
    return () => {
      active = false;
    };
  }, [paseador.id]);

  const agendaOrdenada = useMemo(
    () => [...bloques].sort((a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime()),
    [bloques]
  );

  function handleReservar(idAgenda: number) {
    navigate(
      `/tutor/solicitud-paseo?paseadorId=${encodeURIComponent(paseador.id)}&paseadorNombre=${encodeURIComponent(
        paseador.nombre
      )}&agendaId=${encodeURIComponent(String(idAgenda))}`
    );
  }

  return (
    <div className={styles.overlay} onMouseDown={onClose}>
      <section
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="perfil-paseador-title"
        onMouseDown={(event) => event.stopPropagation()} // Evita cerrar al hacer click dentro
      >
        <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Cerrar perfil">
          &times;
        </button>

        <div className={styles.header}>
          <img src={paseador.fotoUrl} alt={`Foto de ${paseador.nombre}`} className={styles.photo} />
          <div className={styles.headerInfo}>
            <span className={paseador.perfilCompleto ? styles.verifiedBadge : styles.unverifiedBadge}>
              {paseador.perfilCompleto ? "Perfil completo" : "Perfil en progreso"}
            </span>
            <h2 id="perfil-paseador-title">{paseador.nombre}</h2>
            <p>{paseador.bio}</p>
          </div>
        </div>

        <div className={styles.statsGrid} aria-label="Resumen del paseador">
          <article>
            <span>Distancia</span>
            <strong>{paseador.distanciaKm.toFixed(1)} km</strong>
          </article>
          <article>
            <span>Calificacion</span>
            <strong>{paseador.calificacionPromedio.toFixed(1)}</strong>
          </article>
          <article className={styles.statItem}>
            <span>Cobertura</span>
            <strong>{paseador.radioCoberturaKm} km</strong>
          </article>
        </div>

        <section className={styles.section}>
          <h3>Agendas disponibles (próximos 14 días)</h3>
          {loadingBloques ? <p className={styles.emptyReviews}>Cargando bloques disponibles...</p> : null}
          {bloquesError ? <p className={styles.errorText}>{bloquesError}</p> : null}
          {!loadingBloques && !bloquesError && agendaOrdenada.length === 0 ? (
            <p className={styles.emptyReviews}>Este paseador no tiene bloques disponibles por ahora.</p>
          ) : null}
          {!loadingBloques && !bloquesError && agendaOrdenada.length > 0 ? (
            <div className={styles.agendaGrid}>
              {agendaOrdenada.map((bloque) => {
                const inicio = toDateSafe(bloque.fecha, bloque.horaInicio);
                const fin = toDateSafe(bloque.fecha, bloque.horaFinal);
                const diaSemana = inicio
                  ? new Intl.DateTimeFormat("es-CL", { weekday: "long" }).format(inicio)
                  : "Dia no disponible";
                const fechaVisual = inicio
                  ? new Intl.DateTimeFormat("es-CL", {
                      day: "2-digit",
                      month: "long",
                      year: "numeric"
                    }).format(inicio)
                  : bloque.fecha;
                const horaInicioVisual = inicio
                  ? new Intl.DateTimeFormat("es-CL", { hour: "2-digit", minute: "2-digit" }).format(inicio)
                  : bloque.horaInicio;
                const horaFinVisual = fin
                  ? new Intl.DateTimeFormat("es-CL", { hour: "2-digit", minute: "2-digit" }).format(fin)
                  : bloque.horaFinal;
                const duracionMin = inicio && fin ? Math.max(1, Math.round((fin.getTime() - inicio.getTime()) / 60000)) : null;
                return (
                  <article key={bloque.idAgenda} className={styles.agendaCard}>
                    <header className={styles.agendaCardHeader}>
                      <span className={styles.agendaChip}>Bloque disponible</span>
                      <span className={styles.agendaState}>{bloque.estadoBloque?.nombre ?? "Disponible"}</span>
                    </header>
                    <div className={styles.agendaDateWrap}>
                      <strong className={styles.agendaDay}>{diaSemana}</strong>
                      <span className={styles.agendaDate}>{fechaVisual}</span>
                    </div>
                    <div className={styles.agendaTimeRow}>
                      <div className={styles.agendaTimeItem}>
                        <span>Inicio</span>
                        <strong>{horaInicioVisual}</strong>
                      </div>
                      <div className={styles.agendaTimeSeparator}>→</div>
                      <div className={styles.agendaTimeItem}>
                        <span>Fin</span>
                        <strong>{horaFinVisual}</strong>
                      </div>
                    </div>
                    <p className={styles.agendaMeta}>
                      {duracionMin != null ? `Duracion aproximada: ${duracionMin} min` : "Duracion no disponible"}
                    </p>
                    <button
                      type="button"
                      className={styles.agendaButton}
                      onClick={() => handleReservar(bloque.idAgenda)}
                    >
                      Reservar este bloque
                    </button>
                  </article>
                );
              })}
            </div>
          ) : null}
        </section>
      </section>
    </div>
  );
}