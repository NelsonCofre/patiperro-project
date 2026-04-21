import { type FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
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
import { dispararNotificacion } from "../../services/notificacionesApi"; // <-- Agrega esto

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
  const [searchParams] = useSearchParams();
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
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [correoPaseador, setCorreoPaseador] = useState<string>("");

  useEffect(() => {
    let active = true;

    async function loadData() {
      if (!paseadorId) {
        setErrors({ general: "Falta paseadorId en la URL. Vuelve al dashboard y selecciona un perfil." });
        setLoadingData(false);
        return;
      }

      setLoadingData(true);
      setErrors({});

      try {
        const today = new Date();
        const until = new Date(today);
        until.setDate(until.getDate() + 14);
        const desde = today.toISOString().slice(0, 10);
        const hasta = until.toISOString().slice(0, 10);

        // 👇 Modificamos el Promise.all para incluir fetchPerfilPaseador 👇
        const [mascotasData, bloquesData, tarifasData, perfilPaseador] = await Promise.all([
          fetchMascotasTutor(),
          fetchAgendaOfertaPaseador(paseadorId, desde, hasta),
          fetchTarifasPublicasPaseador(paseadorId),
          fetchPerfilPaseador(paseadorId) // <-- NUEVO
        ]);

        if (!active) return;
        setMascotas(mascotasData);
        setBloques(bloquesData);
        setTarifasPaseador(tarifasData);
        setCorreoPaseador(perfilPaseador.correo); // <-- NUEVO: Guardamos el correo

        if (agendaIdParam && bloquesData.some((b) => b.idAgenda === agendaIdParam)) {
          setBloqueId(String(agendaIdParam));
        }
      } catch (error) {
        if (!active) return;
        const message =
          error instanceof Error ? error.message : "No se pudieron cargar los datos de la solicitud.";
        setErrors({ general: message });
      } finally {
        if (active) setLoadingData(false);
      }
    }

    loadData();
    return () => {
      active = false;
    };
  }, [agendaIdParam, paseadorId]);

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

  /** Mascota elegida pero su tamaño no coincide con ninguna tarifa del paseador (y hay tarifas cargadas). */
  const desajusteTamanoTarifa = useMemo(
    () =>
      Boolean(selectedMascota && !selectedTarifa && tarifasPaseador.length > 0),
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

  const canSubmit = Boolean(mascotaId && bloqueId && selectedTarifa && total > 0) && !isSubmitting && !isSubmitted;

  const bloqueLabel = useMemo(() => {
    if (!selectedBloque) return "Por seleccionar";
    const inicio = toDateSafe(selectedBloque.fecha, selectedBloque.horaInicio);
    const fin = toDateSafe(selectedBloque.fecha, selectedBloque.horaFinal);
    if (!inicio || !fin) {
      return `${selectedBloque.fecha} ${selectedBloque.horaInicio} - ${selectedBloque.horaFinal}`;
    }
    const formatter = new Intl.DateTimeFormat("es-CL", {
      weekday: "short",
      day: "2-digit",
      month: "short",
      hour: "2-digit",
      minute: "2-digit"
    });
    return `${formatter.format(inicio)} - ${formatter.format(fin)}`;
  }, [selectedBloque]);

  function validateForm(): boolean {
    const nextErrors: FormErrors = {};

    if (!mascotaId) {
      nextErrors.mascotaId = "Debes seleccionar una mascota para continuar";
    }

    if (!bloqueId) {
      nextErrors.bloqueId = "Debes seleccionar un horario disponible para continuar";
    }
    if (selectedMascota && !selectedTarifa) {
      nextErrors.general = desajusteTamanoTarifa
        ? "El tamaño de tu mascota no coincide con las tarifas que ofrece este paseador. Revisa el recuadro de tarifas disponibles."
        : paseadorSinTarifasPublicas
          ? "Este paseador aún no tiene tarifas publicadas. No puedes completar la reserva hasta que configure precios."
          : `No hay tarifa aplicable para ${selectedMascota.tamanoNombre || "este tamaño"}.`;
    } else if (
      mascotaId &&
      bloqueId &&
      selectedTarifa &&
      !(Number.isFinite(total) && total > 0)
    ) {
      nextErrors.general = "No se pudo calcular el monto total de la reserva.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting || isSubmitted) return;

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    setErrors({});

    try {
      const idTutor = readTutorIdFromSession();
      const idEstadoSolicitada = await fetchEstadoSolicitadaId();
      const agenda = parsePositiveInt(bloqueId);
      const mascota = parsePositiveInt(mascotaId);

      // 👇 SOLUCIÓN ERROR 2: Declaramos el nombreTutor leyendo la sesión.
      // (Si en tu login no guardas el nombre, dirá "Tutor" por defecto)
      const nombreTutor = sessionStorage.getItem("nombreTutor") || "Tutor"; 
      
      if (!selectedTarifa || !agenda || !mascota) {
        throw new Error("Hay campos inválidos para crear la reserva.");
      }

      // 1. CREA LA RESERVA
      await crearReservaTutor({
        idTutorUsuario: idTutor,
        idMascota: mascota,
        idAgendaBloque: agenda,
        idTarifa: selectedTarifa.idTarifa,
        fechaSolicitud: nowLocalDateTimeISO(),
        montoTotal: total,
        idEstadoReserva: idEstadoSolicitada
      });

      // 2. DISPARA EL CORREO AL PASEADOR 🚀
      try {
        await dispararNotificacion({
          emailDestino: correoPaseador, 
          tipoEvento: "SOLICITUD_PASEO", 
          variables: {
            nombrePaseador: getPaseadorName(paseadorNombre),
            nombreMascota: selectedMascota?.nombre || "tu mascota",
            montoTotal: total.toString(),
            nombreTutor: nombreTutor // <-- ¡Ahora sí existe y no dará error!
          }
        });
        console.log("Notificación enviada con éxito a Brevo");
      } catch (emailError) {
        console.error("Reserva creada, pero la notificación falló:", emailError);
      }
      
      setIsSubmitted(true);

    // 👇 SOLUCIÓN ERROR 1: Aquí está el catch y finally que faltaba para cerrar el try principal
    } catch (error) {
      const message = error instanceof Error ? error.message : "No se pudo crear la reserva.";
      setErrors({ general: message });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className={styles.page}>
      {isSubmitted ? (
        <div className={styles.toastOverlay}>
          <div className={styles.toastCard} role="dialog" aria-modal="true">
            <span className={styles.toastEyebrow}>Solicitud enviada</span>
            <h2>Tu solicitud ha sido enviada con exito</h2>
            <p>
              Tu reserva fue creada con estado SOLICITADA. El paseador ya puede verla para
              aceptarla o rechazarla.
            </p>
            <Link to="/tutor/dashboard" className={styles.toastButton}>
              Volver al home
            </Link>
          </div>
        </div>
      ) : null}

      <TutorNavbar />

      <section className={styles.shell}>
        <div className={styles.heading}>
          <p>Confirmacion de solicitud</p>
          <h1>Revisa los detalles antes de enviar tu solicitud</h1>
          <span>
            {paseadorId
              ? `Paseador seleccionado: ${getPaseadorName(paseadorNombre)}`
              : "Selecciona un paseador desde el home del tutor para completar este flujo."}
          </span>
        </div>
        {errors.general ? <p className={styles.errorText}>{errors.general}</p> : null}

        <form className={styles.contentGrid} onSubmit={handleSubmit} noValidate>
          <section className={styles.formCard}>
            <h2>Datos del paseo</h2>

            <label className={styles.field}>
              <span>Mascota</span>
              <select
                value={mascotaId}
                onChange={(event) => {
                  setMascotaId(event.target.value);
                  setErrors((prev) => ({ ...prev, mascotaId: undefined, general: undefined }));
                }}
                className={errors.mascotaId ? styles.fieldError : ""}
                disabled={loadingData}
              >
                <option value="">Selecciona una mascota</option>
                {mascotas.map((mascota) => (
                  <option key={mascota.idMascota} value={mascota.idMascota}>
                    {mascota.nombre}
                  </option>
                ))}
              </select>
              {errors.mascotaId ? <small>{errors.mascotaId}</small> : null}
            </label>

            {desajusteTamanoTarifa ? (
              <div className={styles.tarifaMismatchBox} role="status">
                <strong>Tarifas por tamaño de este paseador</strong>
                <p>
                  El paseador solo ofrece tarifa por los tamaños que aparecen abajo. El tamaño de tu
                  mascota no coincide con ninguna de esas tarifas.
                </p>
                <ul className={styles.tarifaMismatchList}>
                  {tarifasPaseador.map((t) => (
                    <li key={`${t.tamanoId}-${t.tamanoNombre}`}>
                      {t.tamanoNombre}: {formatCurrency(t.precioPorHora)} / hora
                    </li>
                  ))}
                </ul>
                <p>
                  Tu mascota está registrada como{" "}
                  <strong>{selectedMascota?.tamanoNombre?.trim() || "sin tamaño indicado"}</strong>.
                  Prueba con otra mascota o contacta al paseador.
                </p>
              </div>
            ) : null}

            {paseadorSinTarifasPublicas ? (
              <div className={styles.tarifaMismatchBox} role="alert">
                <strong>Sin tarifas publicadas</strong>
                <p>
                  Este paseador aún no tiene tarifas configuradas. No podrás completar la reserva hasta
                  que publique precios por tamaño.
                </p>
              </div>
            ) : null}

            <label className={styles.field}>
              <span>Bloque horario disponible</span>
              <div
                className={`${styles.blockGrid} ${errors.bloqueId ? styles.blockGridError : ""}`}
                role="radiogroup"
                aria-label="Bloques horarios disponibles"
              >
                {bloques.map((bloque) => {
                  const isSelected = String(bloque.idAgenda) === bloqueId;
                  const inicio = toDateSafe(bloque.fecha, bloque.horaInicio);
                  const fin = toDateSafe(bloque.fecha, bloque.horaFinal);
                  const duracionMinutos =
                    inicio && fin
                      ? Math.max(1, Math.round((fin.getTime() - inicio.getTime()) / (1000 * 60)))
                      : 60;
                  const dia = inicio
                    ? new Intl.DateTimeFormat("es-CL", {
                        weekday: "short",
                        day: "2-digit",
                        month: "short"
                      }).format(inicio)
                    : bloque.fecha;

                  return (
                    <button
                      key={bloque.idAgenda}
                      type="button"
                      className={`${styles.timeBlock} ${isSelected ? styles.timeBlockSelected : ""}`}
                      onClick={() => {
                        setBloqueId(String(bloque.idAgenda));
                        setErrors((prev) => ({ ...prev, bloqueId: undefined }));
                      }}
                      role="radio"
                      aria-checked={isSelected}
                      disabled={loadingData}
                    >
                      <span className={styles.timeBlockDay}>{dia}</span>
                      <strong>
                        {bloque.horaInicio} - {bloque.horaFinal}
                      </strong>
                      <span>{duracionMinutos} minutos</span>
                      <small>Disponible</small>
                    </button>
                  );
                })}
              </div>
              {errors.bloqueId ? <small>{errors.bloqueId}</small> : null}
            </label>
          </section>

          <aside className={styles.checkoutCard}>
            <p className={styles.checkoutEyebrow}>Checkout</p>
            <h2>Resumen de costos</h2>

            <div className={styles.checkoutList}>
              <div>
                <span>Paseador</span>
                <strong>{getPaseadorName(paseadorNombre)}</strong>
              </div>
              <div>
                <span>Mascota</span>
                <strong>{selectedMascota?.nombre ?? "Por seleccionar"}</strong>
              </div>
              <div>
                <span>Bloque</span>
                <strong>{bloqueLabel}</strong>
              </div>
              <div>
                <span>Tarifa aplicada</span>
                <strong>
                  {selectedTarifa
                    ? `${selectedTarifa.tamanoNombre} · ${formatCurrency(selectedTarifa.precioPorHora)}/hora`
                    : desajusteTamanoTarifa
                      ? "No aplica (revisa el recuadro de arriba)"
                      : paseadorSinTarifasPublicas
                        ? "Sin tarifas publicadas"
                        : "Por seleccionar mascota y bloque"}
                </strong>
              </div>
              <div>
                <span>Duración estimada</span>
                <strong>{duracionMinutos > 0 ? `${duracionMinutos} min` : "--"}</strong>
              </div>
            </div>

            <div className={styles.totalBox}>
              <span>Total a pagar</span>
              <strong>{total ? formatCurrency(total) : "--"}</strong>
            </div>

            <p className={styles.checkoutNote}>
              Al enviar, se crea la reserva real en backend con estado SOLICITADA y el bloque de
              agenda pasa a RESERVADO.
            </p>

            <div className={styles.actions}>
              <Link to="/tutor/dashboard">Volver</Link>
              <button type="submit" disabled={!canSubmit}>
                {isSubmitting
                  ? "Enviando solicitud..."
                  : isSubmitted
                    ? "Solicitud enviada"
                    : "Enviar solicitud de paseo"}
              </button>
            </div>
          </aside>
        </form>
      </section>
    </main>
  );
}
