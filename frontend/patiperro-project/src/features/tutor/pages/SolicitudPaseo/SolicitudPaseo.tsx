import { type FormEvent, useEffect, useMemo, useState, useRef } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import {
  crearReservaTutor,
  nowLocalDateTimeISO,
  fetchAgendaOfertaPaseador,
  fetchEstadoSolicitadaId,
  fetchMascotasTutor,
  fetchTarifasPublicasPaseador,
  readTutorIdFromSession,
  fetchPerfilPaseador,
  type AgendaBloqueOfertaDTO,
  type MascotaTutorDTO,
  type TarifaConfiguracionPublicaDTO
} from "../../services/reservaTutorApi";
import styles from "./SolicitudPaseo.module.css";
import { dispararNotificacion } from "../../services/notificacionesApi";

type FormErrors = {
  mascotaId?: string;
  bloqueId?: string;
  general?: string;
};

function formatCurrency(value: number) {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

function normalizeTamano(value: string): string {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/\s+/g, "");
}

function getPaseadorName(paseadorNombre: string | null) {
  if (!paseadorNombre?.trim()) return "Paseador seleccionado";
  return paseadorNombre;
}

function parsePositiveInt(value: string): number | null {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

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

export default function SolicitudPaseo() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  const paseadorId = parsePositiveInt(searchParams.get("paseadorId") ?? "");
  const paseadorNombre = (searchParams.get("paseadorNombre") ?? "").trim() || null;
  const agendaIdParam = parsePositiveInt(searchParams.get("agendaId") ?? "");

  const [mascotas, setMascotas] = useState<MascotaTutorDTO[]>([]);
  const [tarifasPaseador, setTarifasPaseador] = useState<TarifaConfiguracionPublicaDTO[]>([]);
  const [bloques, setBloques] = useState<AgendaBloqueOfertaDTO[]>([]);
  const [mascotaId, setMascotaId] = useState("");
  const [bloqueId, setBloqueId] = useState("");
  const [errors, setErrors] = useState<FormErrors>({});
  const [loadingData, setLoadingData] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [correoPaseador, setCorreoPaseador] = useState<string>("");

  // --- ESTADOS DEL CALENDARIO ---
  const [currentWeekOffset, setCurrentWeekOffset] = useState(0);
  const [selectedISODate, setSelectedISODate] = useState(() => {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd}`;
  });

  useEffect(() => {
    let active = true;

    async function loadData() {
      if (!paseadorId) {
        setErrors({ general: "Falta paseadorId en la URL. Vuelve al dashboard." });
        setLoadingData(false);
        return;
      }

      setLoadingData(true);
      try {
        const today = new Date();
        const until = new Date(today);
        until.setDate(today.getDate() + 28); // Rango de 28 días

        const desde = today.toLocaleDateString('sv-SE'); // YYYY-MM-DD local
        const hasta = until.toLocaleDateString('sv-SE');

        const [mascotasData, bloquesData, tarifasData, perfilPaseador] = await Promise.all([
          fetchMascotasTutor(),
          fetchAgendaOfertaPaseador(paseadorId, desde, hasta),
          fetchTarifasPublicasPaseador(paseadorId),
          fetchPerfilPaseador(paseadorId)
        ]);

        if (!active) return;
        setMascotas(mascotasData);
        setBloques(bloquesData);
        setTarifasPaseador(tarifasData);
        setCorreoPaseador(perfilPaseador.correo);

        // Si viene un bloque seleccionado por URL
        if (agendaIdParam) {
          const bloqueEncontrado = bloquesData.find(b => b.idAgenda === agendaIdParam);
          if (bloqueEncontrado) {
            setBloqueId(String(agendaIdParam));
            setSelectedISODate(bloqueEncontrado.fecha);
            
            // Calcular el offset de semana si el bloque es de una semana futura
            const bDate = new Date(bloqueEncontrado.fecha + "T00:00:00");
            const tDate = new Date();
            tDate.setHours(0,0,0,0);
            const diffDays = Math.floor((bDate.getTime() - tDate.getTime()) / (1000 * 60 * 60 * 24));
            if (diffDays >= 7) {
                setCurrentWeekOffset(Math.floor(diffDays / 7));
            }
          }
        }
      } catch (error) {
        if (!active) return;
        setErrors({ general: error instanceof Error ? error.message : "Error al cargar datos." });
      } finally {
        if (active) setLoadingData(false);
      }
    }

    loadData();
    return () => { active = false; };
  }, [agendaIdParam, paseadorId]);

  // --- LÓGICA DE DÍAS DE LA SEMANA (LOCAL) ---
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
      const isoLocal = d.toLocaleDateString('sv-SE');
      
      days.push({
        isoDate: isoLocal,
        dayNumber: d.getDate(),
        dayLabel: new Intl.DateTimeFormat("es-CL", { weekday: "short" }).format(d),
        monthLabel: new Intl.DateTimeFormat("es-CL", { month: "short" }).format(d),
        isToday: isoLocal === new Date().toLocaleDateString('sv-SE'),
        hasBlocks: bloques.some(b => b.fecha === isoLocal)
      });
    }
    return days;
  }, [currentWeekOffset, bloques]);

  const currentDayBlocks = useMemo(() => {
    const now = new Date();
    // Obtenemos la fecha local de hoy en formato YYYY-MM-DD
    const currentISODate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;

    return bloques
      .filter((b) => b.fecha === selectedISODate)
      .filter((b) => {
        // Si el día seleccionado es hoy, evaluamos la hora
        if (selectedISODate === currentISODate) {
          const blockStartTime = toDateSafe(b.fecha, b.horaInicio);
          // Si el bloque comienza antes de la hora actual, lo filtramos
          if (blockStartTime && blockStartTime < now) {
            return false;
          }
        }
        return true;
      })
      .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
  }, [selectedISODate, bloques]);

  const selectedMascota = useMemo(
    () => mascotas.find((mascota) => String(mascota.idMascota) === mascotaId) ?? null,
    [mascotaId, mascotas]
  );

  const selectedBloque = useMemo(
    () => bloques.find((bloque) => String(bloque.idAgenda) === bloqueId) ?? null,
    [bloqueId, bloques]
  );

  const selectedTarifa = useMemo(() => {
    if (!selectedMascota) return null;
    const mascotaTamanoNorm = normalizeTamano(selectedMascota.tamanoNombre);
    if (mascotaTamanoNorm) {
      const byNombre =
        tarifasPaseador.find((t) => normalizeTamano(t.tamanoNombre) === mascotaTamanoNorm) ?? null;
      if (byNombre) return byNombre;
    }
    if (selectedMascota.tamanoId != null) {
      return tarifasPaseador.find((t) => t.tamanoId === selectedMascota.tamanoId) ?? null;
    }
    return null;
  }, [selectedMascota, tarifasPaseador]);

  const desajusteTamanoTarifa = useMemo(
    () => Boolean(selectedMascota && !selectedTarifa && tarifasPaseador.length > 0),
    [selectedMascota, selectedTarifa, tarifasPaseador.length]
  );

  const paseadorSinTarifasPublicas = useMemo(
    () => Boolean(selectedMascota && !selectedTarifa && tarifasPaseador.length === 0 && !loadingData),
    [selectedMascota, selectedTarifa, tarifasPaseador.length, loadingData]
  );

  const duracionMinutos = useMemo(() => {
    if (!selectedBloque) return 0;
    const inicio = toDateSafe(selectedBloque.fecha, selectedBloque.horaInicio);
    const fin = toDateSafe(selectedBloque.fecha, selectedBloque.horaFinal);
    if (!inicio || !fin) return 0;
    return Math.max(1, Math.round((fin.getTime() - inicio.getTime()) / (1000 * 60)));
  }, [selectedBloque]);

  const total = useMemo(() => {
    if (!selectedTarifa || duracionMinutos <= 0) return 0;
    return Math.round((selectedTarifa.precioPorHora * duracionMinutos) / 60);
  }, [selectedTarifa, duracionMinutos]);

  const canSubmit = Boolean(mascotaId && bloqueId && selectedTarifa && total > 0) && !isSubmitting;

  const bloqueLabel = useMemo(() => {
    if (!selectedBloque) return "Por seleccionar";
    const inicio = toDateSafe(selectedBloque.fecha, selectedBloque.horaInicio);
    const fin = toDateSafe(selectedBloque.fecha, selectedBloque.horaFinal);
    if (!inicio || !fin) return `${selectedBloque.fecha} ${selectedBloque.horaInicio}`;
    
    const formatter = new Intl.DateTimeFormat("es-CL", {
      weekday: "short", day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit"
    });
    return `${formatter.format(inicio)} - ${formatter.format(fin)}`;
  }, [selectedBloque]);

  const handleScroll = (direction: 'left' | 'right') => {
    if (scrollContainerRef.current) {
      const scrollAmount = scrollContainerRef.current.offsetWidth * 0.8;
      scrollContainerRef.current.scrollBy({
        left: direction === 'left' ? -scrollAmount : scrollAmount,
        behavior: 'smooth'
      });
    }
  };

  function validateForm(): boolean {
    const nextErrors: FormErrors = {};
    if (!mascotaId) nextErrors.mascotaId = "Debes seleccionar una mascota";
    if (!bloqueId) nextErrors.bloqueId = "Debes seleccionar un horario";
    if (selectedMascota && !selectedTarifa) {
      nextErrors.general = "No hay tarifa aplicable para este tamaño de mascota.";
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSubmitting || !validateForm()) return;

    setIsSubmitting(true);
    try {
      const idTutor = readTutorIdFromSession();
      const idEstadoSolicitada = await fetchEstadoSolicitadaId();
      const nombreTutorRaw = sessionStorage.getItem("patiperro_nombre_usuario");
      const nombreTutorFinal = nombreTutorRaw ? nombreTutorRaw.split(" ")[0] : "Un tutor";
      
      const reservaCreada = await crearReservaTutor({
        idTutorUsuario: idTutor,
        idMascota: parsePositiveInt(mascotaId)!,
        idAgendaBloque: parsePositiveInt(bloqueId)!,
        idTarifa: selectedTarifa!.idTarifa,
        fechaSolicitud: nowLocalDateTimeISO(),
        montoTotal: total,
        idEstadoReserva: idEstadoSolicitada
      });

      try {
        await dispararNotificacion({
          emailDestino: correoPaseador, 
          tipoEvento: "SOLICITUD_PASEO", 
          variables: {
            nombrePaseador: getPaseadorName(paseadorNombre).split(" ")[0],
            nombreMascota: selectedMascota?.nombre || "tu mascota",
            montoTotal: total.toString(),
            nombreTutor: nombreTutorFinal,
            urlReserva: "http://localhost:5173/login/paseador"
          }
        });
      } catch (e) { console.error("Fallo notificación", e); }
      
      const bloqueResumen = `${selectedBloque?.fecha ?? ""} ${selectedBloque?.horaInicio ?? ""}-${selectedBloque?.horaFinal ?? ""}`;
      navigate(`/tutor/pago-reserva?idReserva=${reservaCreada.idReserva}&total=${total}&paseador=${getPaseadorName(paseadorNombre)}&mascota=${selectedMascota?.nombre}&bloque=${bloqueResumen}`);

    } catch (error) {
      setErrors({ general: error instanceof Error ? error.message : "Error al crear reserva." });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className={styles.page}>
      <TutorNavbar />

      <section className={styles.shell}>
        <div className={styles.heading}>
          <p>Confirmacion de solicitud</p>
          <h1>Revisa los detalles antes de enviar tu solicitud</h1>
          <span>{getPaseadorName(paseadorNombre)}</span>
        </div>

        {errors.general && <p className={styles.errorText}>{errors.general}</p>}

        <form className={styles.contentGrid} onSubmit={handleSubmit} noValidate>
          <section className={styles.formCard}>
            <h2>Datos del paseo</h2>

            <label className={styles.field}>
              <span>Mascota</span>
              <select
                value={mascotaId}
                onChange={(e) => setMascotaId(e.target.value)}
                className={errors.mascotaId ? styles.fieldError : ""}
              >
                <option value="">Selecciona una mascota</option>
                {mascotas.map((m) => (
                  <option key={m.idMascota} value={m.idMascota}>{m.nombre}</option>
                ))}
              </select>
              {errors.mascotaId && <small>{errors.mascotaId}</small>}
            </label>

            <div className={styles.calendarSection}>
              <div className={styles.calendarHeader}>
                <h3>Bloque horario disponible</h3>
                <div className={styles.weekNav}>
                  <button type="button" onClick={() => setCurrentWeekOffset(o => o - 1)} disabled={currentWeekOffset <= 0}>Anterior</button>
                  <button type="button" onClick={() => setCurrentWeekOffset(o => o + 1)}>Siguiente</button>
                </div>
              </div>

              {/* TABS DE DÍAS */}
              <div className={styles.dayTabs}>
                {weekDays.map((day) => (
                  <button
                    key={day.isoDate}
                    type="button"
                    className={`${styles.dayTab} ${selectedISODate === day.isoDate ? styles.dayTabActive : ""} ${!day.hasBlocks ? styles.dayDisabled : ""}`}
                    onClick={() => setSelectedISODate(day.isoDate)}
                  >
                    <span className={styles.dayName}>{day.dayLabel}</span>
                    <strong className={styles.dayNum}>{day.dayNumber}</strong>
                    <span className={styles.dayMonth}>{day.monthLabel}</span>
                  </button>
                ))}
              </div>

              {/* LISTADO HORIZONTAL DE BLOQUES (5-6 visible) */}
<div className={styles.blocksWrapper}>
  <button 
    type="button" 
    className={styles.navArrow} 
    onClick={() => handleScroll('left')}
  >
    ‹
  </button>
  
  <div className={styles.blocksScroll} ref={scrollContainerRef}>
    {currentDayBlocks.length > 0 ? (
      currentDayBlocks.map((b) => {
        // Intentamos convertir a objeto Date para un formateo limpio y seguro
        const dateInicio = toDateSafe(b.fecha, b.horaInicio);
        const dateFin = toDateSafe(b.fecha, b.horaFinal);
        
        // Configurador de formato: fuerza 24 horas (HH:mm)
        const fmt = new Intl.DateTimeFormat("es-CL", {
          hour: "2-digit",
          minute: "2-digit",
          hour12: false
        });

        // Si la conversión falla, usamos slice como respaldo para evitar el "2026-"
        const hInicio = dateInicio ? fmt.format(dateInicio) : (b.horaInicio ?? "").slice(0, 5);
        const hFin = dateFin ? fmt.format(dateFin) : (b.horaFinal ?? "").slice(0, 5);

        return (
          <button
            key={b.idAgenda}
            type="button"
            className={`${styles.timeCard} ${bloqueId === String(b.idAgenda) ? styles.timeCardSelected : ""}`}
            onClick={() => {
              setBloqueId(String(b.idAgenda));
              setErrors((prev) => ({ ...prev, bloqueId: undefined }));
            }}
          >
            <span className={styles.cardDate}>{b.fecha}</span>
            <div className={styles.cardHours}>
              <strong>{hInicio}</strong>
              <span>-</span>
              <strong>{hFin}</strong>
            </div>
            <small>Disponible</small>
          </button>
        );
      })
    ) : (
      <p className={styles.noBlocks}>No hay bloques para este día.</p>
    )}
  </div>

  <button 
    type="button" 
    className={styles.navArrow} 
    onClick={() => handleScroll('right')}
  >
    ›
  </button>
</div>
              {errors.bloqueId && <small className={styles.errorSmall}>{errors.bloqueId}</small>}
            </div>
          </section>

          <aside className={styles.checkoutCard}>
            <p className={styles.checkoutEyebrow}>Checkout</p>
            <h2>Resumen de costos</h2>

            <div className={styles.checkoutList}>
              <div><span>Paseador</span><strong>{getPaseadorName(paseadorNombre)}</strong></div>
              <div><span>Mascota</span><strong>{selectedMascota?.nombre ?? "--"}</strong></div>
              <div><span>Bloque</span><strong>{bloqueLabel}</strong></div>
              <div>
                <span>Tarifa</span>
                <strong>{selectedTarifa ? `${selectedTarifa.tamanoNombre} · ${formatCurrency(selectedTarifa.precioPorHora)}/h` : "--"}</strong>
              </div>
              <div><span>Duración</span><strong>{duracionMinutos > 0 ? `${duracionMinutos} min` : "--"}</strong></div>
            </div>

            <div className={styles.totalBox}>
              <span>Total a pagar</span>
              <strong>{total ? formatCurrency(total) : "--"}</strong>
            </div>

            <div className={styles.actions}>
              <Link to="/tutor/dashboard">Volver</Link>
              <button type="submit" disabled={!canSubmit}>
                {isSubmitting ? "Procesando..." : "Continuar al pago"}
              </button>
            </div>
          </aside>
        </form>
      </section>
    </main>
  );
}