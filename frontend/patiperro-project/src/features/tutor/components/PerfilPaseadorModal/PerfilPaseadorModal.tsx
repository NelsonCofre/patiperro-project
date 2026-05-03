import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { PaseadorHome } from "../../types/paseadorHome.types";
import {
  fetchAgendaOfertaPaseador,
  type AgendaBloqueOfertaDTO,
} from "../../services/reservaTutorApi";
import { resenaApi } from "../../services/resenaApi"; // Importado nuevo servicio
import type { ResenaDetalleDTO } from "../../types/resena.types"; // Importado nuevo tipo
import { formatDistanceFromKm } from "../../utils/distanceFormat";
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
      : "",
  ].filter(Boolean);

  for (const candidate of candidates) {
    const parsed = new Date(candidate);
    if (!Number.isNaN(parsed.getTime())) return parsed;
  }

  return null;
}

export default function PerfilPaseadorModal({
  paseador,
  onClose,
}: PerfilPaseadorModalProps) {
  const navigate = useNavigate();

  // Estados de Agenda (Existentes)
  const [bloques, setBloques] = useState<AgendaBloqueOfertaDTO[]>([]);
  const [loadingBloques, setLoadingBloques] = useState(false);
  const [bloquesError, setBloquesError] = useState<string | null>(null);

  // Estados de Reseñas (Nuevos para Historia de Usuario)
  const [resenas, setResenas] = useState<ResenaDetalleDTO[]>([]);
  const [promedioReal, setPromedioReal] = useState<number>(
    paseador.calificacionPromedio,
  );
  const [loadingResenas, setLoadingResenas] = useState(false);

  // Bloquear el scroll del body (Existente)
  useEffect(() => {
    document.body.style.overflow = "hidden";
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = "unset";
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose]);

  // Carga de Bloques y Reseñas Reales
  useEffect(() => {
    let active = true;
    const idPaseadorNum = Number.parseInt(paseador.id, 10);

    async function loadData() {
      setLoadingBloques(true);
      setLoadingResenas(true);
      setBloquesError(null);

      try {
        // Carga de Agenda
        const today = new Date();
        const until = new Date(today);
        until.setDate(until.getDate() + 14);
        const desde = today.toISOString().slice(0, 10);
        const hasta = until.toISOString().slice(0, 10);

        if (!Number.isFinite(idPaseadorNum))
          throw new Error("ID de paseador no válido.");

        // Ejecución en paralelo de Agenda, Reseñas y Promedio
        const [agenda, listaResenas, promedio] = await Promise.all([
          fetchAgendaOfertaPaseador(idPaseadorNum, desde, hasta),
          resenaApi.obtenerResenasPorPaseador(idPaseadorNum),
          resenaApi.obtenerPromedioPaseador(idPaseadorNum),
        ]);

        if (!active) return;

        setBloques(agenda);
        // AC #3: Ordenar por ID descendente (asumiendo cronología por creación)
        setResenas(listaResenas.reverse());
        setPromedioReal(promedio || 0);
      } catch (error) {
        if (!active) return;
        setBloquesError(
          error instanceof Error
            ? error.message
            : "Error al cargar información.",
        );
      } finally {
        if (active) {
          setLoadingBloques(false);
          setLoadingResenas(false);
        }
      }
    }

    loadData();
    return () => {
      active = false;
    };
  }, [paseador.id]);

  const agendaOrdenada = useMemo(
    () =>
      [...bloques].sort(
        (a, b) => new Date(a.fecha).getTime() - new Date(b.fecha).getTime(),
      ),
    [bloques],
  );

  function handleReservar(idAgenda: number) {
    navigate(
      `/tutor/solicitud-paseo?paseadorId=${encodeURIComponent(paseador.id)}&paseadorNombre=${encodeURIComponent(
        paseador.nombre,
      )}&agendaId=${encodeURIComponent(String(idAgenda))}`,
    );
  }

  return (
    <div className={styles.overlay} onMouseDown={onClose}>
      <section
        className={styles.modal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="perfil-paseador-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button
          type="button"
          className={styles.closeButton}
          onClick={onClose}
          aria-label="Cerrar perfil"
        >
          &times;
        </button>

        <div className={styles.header}>
          <img
            src={paseador.fotoUrl}
            alt={`Foto de ${paseador.nombre}`}
            className={styles.photo}
          />
          <div className={styles.headerInfo}>
            <span
              className={
                paseador.perfilCompleto
                  ? styles.verifiedBadge
                  : styles.unverifiedBadge
              }
            >
              {paseador.perfilCompleto
                ? "Perfil completo"
                : "Perfil en progreso"}
            </span>
            <h2 id="perfil-paseador-title">{paseador.nombre}</h2>
            <p>{paseador.bio}</p>
          </div>
        </div>

        <div className={styles.statsGrid} aria-label="Resumen del paseador">
          <article>
            <span>Distancia</span>
            <strong>{formatDistanceFromKm(paseador.distanciaKm)}</strong>
          </article>
          <article>
            <span>Calificación</span>
            {/* AC #4: Promedio general dinámico */}
            <strong>{promedioReal.toFixed(1)} ★</strong>
          </article>
          <article className={styles.statItem}>
            <span>Cobertura</span>
            <strong>{paseador.radioCoberturaKm} km</strong>
          </article>
        </div>

        {/* Sección de Agendas (Lógica intacta) */}
        <section className={styles.section}>
          <h3>Agendas disponibles (próximos 14 días)</h3>
          {loadingBloques ? (
            <p className={styles.emptyReviews}>
              Cargando bloques disponibles...
            </p>
          ) : null}
          {bloquesError ? (
            <p className={styles.errorText}>{bloquesError}</p>
          ) : null}
          {!loadingBloques && !bloquesError && agendaOrdenada.length === 0 ? (
            <p className={styles.emptyReviews}>
              Este paseador no tiene bloques disponibles por ahora.
            </p>
          ) : null}
          {!loadingBloques && !bloquesError && agendaOrdenada.length > 0 ? (
            <div className={styles.agendaGrid}>
              {agendaOrdenada.map((bloque) => {
                const inicio = toDateSafe(bloque.fecha, bloque.horaInicio);
                const fin = toDateSafe(bloque.fecha, bloque.horaFinal);
                const diaSemana = inicio
                  ? new Intl.DateTimeFormat("es-CL", {
                      weekday: "long",
                    }).format(inicio)
                  : "Día no disponible";
                const fechaVisual = inicio
                  ? new Intl.DateTimeFormat("es-CL", {
                      day: "2-digit",
                      month: "long",
                      year: "numeric",
                    }).format(inicio)
                  : bloque.fecha;
                const horaInicioVisual = inicio
                  ? new Intl.DateTimeFormat("es-CL", {
                      hour: "2-digit",
                      minute: "2-digit",
                    }).format(inicio)
                  : bloque.horaInicio;
                const horaFinVisual = fin
                  ? new Intl.DateTimeFormat("es-CL", {
                      hour: "2-digit",
                      minute: "2-digit",
                    }).format(fin)
                  : bloque.horaFinal;
                const duracionMin =
                  inicio && fin
                    ? Math.max(
                        1,
                        Math.round((fin.getTime() - inicio.getTime()) / 60000),
                      )
                    : null;

                return (
                  <article key={bloque.idAgenda} className={styles.agendaCard}>
                    <header className={styles.agendaCardHeader}>
                      <span className={styles.agendaChip}>
                        Bloque disponible
                      </span>
                      <span className={styles.agendaState}>
                        {bloque.estadoBloque?.nombre ?? "Disponible"}
                      </span>
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
                      {duracionMin != null
                        ? `Duración aproximada: ${duracionMin} min`
                        : "Duración no disponible"}
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

        {/* Sección de Reseñas Reales (Actualizada para Historia de Usuario) */}
        <section className={styles.section}>
          <h3>Reseñas de otros tutores</h3>
          {loadingResenas ? (
            <p className={styles.emptyReviews}>Cargando opiniones...</p>
          ) : resenas.length === 0 ? (
            /* AC #6: Manejo de Estado Vacío */
            <p className={styles.emptyReviews}>
              Este paseador aún no tiene reseñas.
            </p>
          ) : (
            <div className={styles.reviewList}>
              {/* AC #5: Límite inicial de 5 reseñas */}
              {resenas.slice(0, 5).map((resena) => (
                <article key={resena.id} className={styles.review}>
                  <div className={styles.reviewHeader}>
                    {/* Verifica que esta línea apunte a nombreTutor */}
                    <strong>{resena.nombreTutor || "Tutor Anónimo"}</strong>
                    <div className={styles.estrellas}>
                      {"★".repeat(resena.estrellas)}
                      {"☆".repeat(5 - resena.estrellas)}
                    </div>
                  </div>
                  <p>
                    {resena.comentario ||
                      "El tutor no dejó un comentario escrito."}
                  </p>
                  <span className={styles.reviewScore}>
                    {resena.estrellas.toFixed(1)}
                  </span>
                </article>
              ))}

              {resenas.length > 5 && (
                <button type="button" className={styles.loadMoreButton}>
                  Ver todas las reseñas ({resenas.length})
                </button>
              )}
            </div>
          )}
        </section>
      </section>
    </div>
  );
}
