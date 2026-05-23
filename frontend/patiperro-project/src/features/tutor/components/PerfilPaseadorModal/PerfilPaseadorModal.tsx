import { useEffect, useMemo, useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import type { PaseadorHome } from "../../types/paseadorHome.types";
import {
  fetchAgendaOfertaPaseador,
  type AgendaBloqueOfertaDTO,
} from "../../services/reservaTutorApi";
import { resenaApi } from "../../services/resenaApi";
import type { ResenaDetalleDTO } from "../../types/resena.types";
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
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  // --- ESTADOS ---
  const [bloques, setBloques] = useState<AgendaBloqueOfertaDTO[]>([]);
  const [currentWeekOffset, setCurrentWeekOffset] = useState(0);

  // Inicialización con fecha local YYYY-MM-DD
  const [selectedISODate, setSelectedISODate] = useState(() => {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd}`;
  });

  const [resenas, setResenas] = useState<ResenaDetalleDTO[]>([]);
  const [promedioReal, setPromedioReal] = useState<number>(paseador.calificacionPromedio);
  const [loadingResenas, setLoadingResenas] = useState(false);

  useEffect(() => {
    document.body.style.overflow = "hidden";
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = "unset";
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [onClose]);

  useEffect(() => {
    let active = true;
    const idPaseadorNum = Number.parseInt(paseador.id, 10);

    async function loadData() {
      setLoadingResenas(true);

      try {
        const today = new Date();
        const until = new Date(today);
        
        // CORRECCIÓN: Ampliamos a 28 días para asegurar cobertura de semanas futuras
        until.setDate(today.getDate() + 28); 

        // Formato local para evitar desfases UTC
        const yStart = today.getFullYear();
        const mStart = String(today.getMonth() + 1).padStart(2, "0");
        const dStart = String(today.getDate()).padStart(2, "0");
        const desde = `${yStart}-${mStart}-${dStart}`;

        const yEnd = until.getFullYear();
        const mEnd = String(until.getMonth() + 1).padStart(2, "0");
        const dEnd = String(until.getDate()).padStart(2, "0");
        const hasta = `${yEnd}-${mEnd}-${dEnd}`;

        if (!Number.isFinite(idPaseadorNum)) throw new Error("ID de paseador no válido.");

        const [agenda, listaResenas, promedio] = await Promise.all([
          fetchAgendaOfertaPaseador(idPaseadorNum, desde, hasta),
          resenaApi.obtenerResenasPorPaseador(idPaseadorNum),
          resenaApi.obtenerPromedioPaseador(idPaseadorNum),
        ]);

        if (!active) return;
        setBloques(agenda);
        setResenas(listaResenas.reverse());
        setPromedioReal(promedio || 0);
      } catch (error) {
        if (!active) return;
      } finally {
        if (active) {
          setLoadingResenas(false);
        }
      }
    }
    loadData();
    return () => { active = false; };
  }, [paseador.id]);

  // --- LÓGICA DE CÁLCULO DE SEMANA CORREGIDA (FORZANDO FECHA LOCAL) ---
  const weekDays = useMemo(() => {
    const days = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const dayOfWeek = today.getDay();
    const diffToMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1;

    const monday = new Date(today);
    monday.setDate(today.getDate() - diffToMonday + (currentWeekOffset * 7));

    for (let i = 0; i < 7; i++) {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      
      const yyyy = d.getFullYear();
      const mm = String(d.getMonth() + 1).padStart(2, '0');
      const dd = String(d.getDate()).padStart(2, '0');
      const isoLocal = `${yyyy}-${mm}-${dd}`;
      
      days.push({
        isoDate: isoLocal,
        dayNumber: d.getDate(),
        dayLabel: new Intl.DateTimeFormat("es-CL", { weekday: "short" }).format(d),
        monthLabel: new Intl.DateTimeFormat("es-CL", { month: "short" }).format(d),
        isToday: isoLocal === new Date().toLocaleDateString('sv-SE'),
        isBlocked: !bloques.some(b => b.fecha === isoLocal)
      });
    }
    return days;
  }, [currentWeekOffset, bloques]);

  const selectedDayBlocks = useMemo(() => {
    const now = new Date();
    // Obtenemos la fecha de hoy en el mismo formato YYYY-MM-DD
    const currentISODate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;

    return bloques
      .filter((b) => b.fecha === selectedISODate)
      .filter((b) => {
        // Si el día seleccionado es hoy, filtramos por hora
        if (selectedISODate === currentISODate) {
          const blockStartTime = toDateSafe(b.fecha, b.horaInicio);
          // Si blockStartTime es válido y es menor a la hora actual, lo ocultamos
          if (blockStartTime && blockStartTime < now) {
            return false;
          }
        }
        return true; // Para días futuros, mostramos todos los bloques
      })
      .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
  }, [selectedISODate, bloques]);

  const selectedDateBlocked = !bloques.some(b => b.fecha === selectedISODate);
  const selectedDayLabel = useMemo(() => {
    const found = weekDays.find(d => d.isoDate === selectedISODate);
    return found ? `${found.dayLabel} ${found.dayNumber} de ${found.monthLabel}` : selectedISODate;
  }, [selectedISODate, weekDays]);

  const handleScroll = (direction: 'left' | 'right') => {
    if (scrollContainerRef.current) {
      const scrollAmount = scrollContainerRef.current.offsetWidth * 0.8; 
      scrollContainerRef.current.scrollBy({
        left: direction === 'left' ? -scrollAmount : scrollAmount,
        behavior: 'smooth'
      });
    }
  };

  const handleReservar = (idAgenda: number) => {
    navigate(
      `/tutor/solicitud-paseo?paseadorId=${encodeURIComponent(paseador.id)}&paseadorNombre=${encodeURIComponent(
        paseador.nombre,
      )}&agendaId=${encodeURIComponent(String(idAgenda))}`,
    );
  };

  return (
    <div className={styles.overlay} onMouseDown={onClose}>
      <section className={styles.modal} role="dialog" onMouseDown={(e) => e.stopPropagation()}>
        <button type="button" className={styles.closeButton} onClick={onClose}>&times;</button>

        {/* HEADER */}
        <div className={styles.header}>
          <img src={paseador.fotoUrl} alt={paseador.nombre} className={styles.photo} />
          <div className={styles.headerInfo}>
            <span
              className={paseador.verificado ? styles.verifiedBadge : styles.unverifiedBadge}
            >
              {paseador.verificado ? "✓ Identidad verificada" : "Identidad no verificada"}
            </span>
            <h2>{paseador.nombre}</h2>
            <p>{paseador.bio}</p>
          </div>
        </div>

        <div className={styles.statsGrid}>
          <article><span>Distancia</span><strong>{formatDistanceFromKm(paseador.distanciaKm)}</strong></article>
          <article><span>Calificación</span><strong>{promedioReal.toFixed(1)} ★</strong></article>
          <article><span>Cobertura</span><strong>{paseador.radioCoberturaKm} km</strong></article>
        </div>

        {/* CALENDARIO SEMANAL */}
        <section className={styles.section}>
          <div className={styles.calendarNavHeader}>
            <div>
              <span className={styles.agendaChip}>CALENDARIO</span>
              <h3 className={styles.calendarTitle}>Semana visible</h3>
            </div>
            <div className={styles.weekControls}>
              <button type="button" className={styles.weekBtn} onClick={() => setCurrentWeekOffset(p => p - 1)} disabled={currentWeekOffset <= 0}>Semana anterior</button>
              <button type="button" className={styles.weekBtn} onClick={() => setCurrentWeekOffset(p => p + 1)}>Semana siguiente</button>
            </div>
          </div>

          <div className={styles.dayTabs}>
            {weekDays.map((cell) => (
              <button
                key={cell.isoDate}
                type="button"
                className={`${styles.dayTab} ${selectedISODate === cell.isoDate ? styles.dayTabActive : ""} ${cell.isBlocked ? styles.dayBlocked : ""}`}
                onClick={() => setSelectedISODate(cell.isoDate)}
              >
                <span className={styles.dayLabelShort}>{cell.dayLabel.replace('.', '')}</span>
                <span className={styles.dayTabDate}>{cell.dayNumber}</span>
                <small className={styles.dayTabMonth}>{cell.monthLabel.replace('.', '')}</small>
                {cell.isBlocked && <em className={styles.blockedBadge}>No disponible</em>}
              </button>
            ))}
          </div>

          {/* LISTADO HORIZONTAL DE BLOQUES */}
          <div className={styles.timelineSection}>
            <div className={styles.timelineHeader}>
              <div><p className={styles.cardEyebrow}>Día seleccionado</p><h3>{selectedDayLabel}</h3></div>
              <span className={styles.timelineMeta}>{selectedDateBlocked ? "Día bloqueado" : `${selectedDayBlocks.length} bloque(s) disponible(s)`}</span>
            </div>

            <div className={styles.horizontalScrollWrapper}>
              {selectedDayBlocks.length > 3 && (
                <button type="button" className={`${styles.scrollArrow} ${styles.left}`} onClick={() => handleScroll('left')}>‹</button>
              )}
              
              <div className={styles.blocksHorizontalList} ref={scrollContainerRef}>
                {selectedDayBlocks.length > 0 ? (
                  selectedDayBlocks.map((bloque) => {
                    const dateInicio = toDateSafe(bloque.fecha, bloque.horaInicio);
                    const dateFin = toDateSafe(bloque.fecha, bloque.horaFinal);
                    const hInicio = dateInicio ? new Intl.DateTimeFormat("es-CL", { hour: "2-digit", minute: "2-digit", hour12: false }).format(dateInicio) : bloque.horaInicio.slice(0, 5);
                    const hFin = dateFin ? new Intl.DateTimeFormat("es-CL", { hour: "2-digit", minute: "2-digit", hour12: false }).format(dateFin) : bloque.horaFinal.slice(0, 5);

                    return (
                      <article key={bloque.idAgenda} className={styles.agendaCardHorizontal}>
                        <header className={styles.agendaCardHeader}>
                          <span className={styles.agendaChipSmall}>Bloque disponible</span>
                          <span className={styles.agendaStateSmall}>{bloque.estadoBloque?.nombre ?? "Disponible"}</span>
                        </header>
                        <div className={styles.agendaTimeRow}>
                          <div className={styles.agendaTimeItem}><span>Inicio</span><strong>{hInicio}</strong></div>
                          <div className={styles.agendaTimeSeparator}>→</div>
                          <div className={styles.agendaTimeItem}><span>Fin</span><strong>{hFin}</strong></div>
                        </div>
                        <button type="button" className={styles.agendaButton} onClick={() => handleReservar(bloque.idAgenda)}>Reservar este bloque</button>
                      </article>
                    );
                  })
                ) : (
                  <div className={styles.emptyDayNotice}><p>Este paseador no tiene bloques disponibles para este día.</p></div>
                )}
              </div>

              {selectedDayBlocks.length > 3 && (
                <button type="button" className={`${styles.scrollArrow} ${styles.right}`} onClick={() => handleScroll('right')}>›</button>
              )}
            </div>
          </div>
        </section>

        {/* RESEÑAS */}
        <section className={styles.section}>
          <h3>Reseñas de otros tutores</h3>
          {loadingResenas ? <p className={styles.emptyReviews}>Cargando opiniones...</p> : resenas.length === 0 ? <p className={styles.emptyReviews}>Este paseador aún no tiene reseñas.</p> : (
            <div className={styles.reviewList}>
              {resenas.slice(0, 5).map((resena) => (
                <article key={resena.id} className={styles.review}>
                  <div className={styles.reviewHeader}><strong>{resena.nombreTutor || "Tutor Anónimo"}</strong><div className={styles.estrellas}>{"★".repeat(resena.estrellas)}{"☆".repeat(5 - resena.estrellas)}</div></div>
                  <p>{resena.comentario || "El tutor no dejó un comentario escrito."}</p>
                </article>
              ))}
            </div>
          )}
        </section>
      </section>
    </div>
  );
}