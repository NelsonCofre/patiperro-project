import { type FormEvent, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import styles from "./SolicitudPaseo.module.css";

type MascotaMock = {
  id: string;
  nombre: string;
  tamano: "Pequeno" | "Mediano" | "Grande";
};

type BloqueMock = {
  id: string;
  label: string;
  inicio: string;
  fin: string;
  duracionMinutos: number;
};

type FormErrors = {
  mascotaId?: string;
  bloqueId?: string;
};

const MASCOTAS_MOCK: MascotaMock[] = [
  { id: "mascota-1", nombre: "Pepe", tamano: "Mediano" },
  { id: "mascota-2", nombre: "Luna", tamano: "Pequeno" },
  { id: "mascota-3", nombre: "Rocky", tamano: "Grande" }
];

const BLOQUES_MOCK: BloqueMock[] = [
  {
    id: "bloque-1",
    label: "Hoy · 17:30 a 18:30",
    inicio: "17:30",
    fin: "18:30",
    duracionMinutos: 60
  },
  {
    id: "bloque-2",
    label: "Manana · 10:00 a 11:00",
    inicio: "10:00",
    fin: "11:00",
    duracionMinutos: 60
  },
  {
    id: "bloque-3",
    label: "Viernes · 09:00 a 10:30",
    inicio: "09:00",
    fin: "10:30",
    duracionMinutos: 90
  }
];

const PRICE_BY_SIZE: Record<MascotaMock["tamano"], number> = {
  Pequeno: 8000,
  Mediano: 9500,
  Grande: 11500
};

function formatCurrency(value: number) {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

function getPaseadorName(paseadorId: string) {
  if (!paseadorId) return "Paseador seleccionado";
  return `Paseador #${paseadorId}`;
}

export default function SolicitudPaseo() {
  const [searchParams] = useSearchParams();
  const paseadorId = searchParams.get("paseadorId") ?? "";
  const [mascotaId, setMascotaId] = useState("");
  const [bloqueId, setBloqueId] = useState("");
  const [comentarios, setComentarios] = useState("");
  const [errors, setErrors] = useState<FormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);

  const selectedMascota = useMemo(
    () => MASCOTAS_MOCK.find((mascota) => mascota.id === mascotaId) ?? null,
    [mascotaId]
  );

  const selectedBloque = useMemo(
    () => BLOQUES_MOCK.find((bloque) => bloque.id === bloqueId) ?? null,
    [bloqueId]
  );

  const precioBase = selectedMascota ? PRICE_BY_SIZE[selectedMascota.tamano] : 0;
  const duracionFactor = selectedBloque ? selectedBloque.duracionMinutos / 60 : 1;
  const total = selectedMascota && selectedBloque ? Math.round(precioBase * duracionFactor) : 0;
  const canSubmit = Boolean(mascotaId && bloqueId) && !isSubmitting && !isSubmitted;

  function validateForm() {
    const nextErrors: FormErrors = {};

    if (!mascotaId) {
      nextErrors.mascotaId = "Debes seleccionar una mascota para continuar";
    }

    if (!bloqueId) {
      nextErrors.bloqueId = "Debes seleccionar un horario disponible para continuar";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting || isSubmitted) return;

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    window.setTimeout(() => {
      setIsSubmitting(false);
      setIsSubmitted(true);
    }, 850);
  }

  return (
    <main className={styles.page}>
      {isSubmitted ? (
        <div className={styles.toastOverlay}>
          <div className={styles.toastCard} role="dialog" aria-modal="true">
            <span className={styles.toastEyebrow}>Solicitud enviada</span>
            <h2>Tu solicitud ha sido enviada con exito</h2>
            <p>
              El paseador recibira la solicitud pendiente de revision cuando este flujo se
              conecte al backend.
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
              ? `Paseador seleccionado: ${getPaseadorName(paseadorId)}`
              : "Selecciona un paseador desde el home del tutor para completar este flujo."}
          </span>
        </div>

        <form className={styles.contentGrid} onSubmit={handleSubmit} noValidate>
          <section className={styles.formCard}>
            <h2>Datos del paseo</h2>

            <label className={styles.field}>
              <span>Mascota</span>
              <select
                value={mascotaId}
                onChange={(event) => {
                  setMascotaId(event.target.value);
                  setErrors((prev) => ({ ...prev, mascotaId: undefined }));
                }}
                className={errors.mascotaId ? styles.fieldError : ""}
              >
                <option value="">Selecciona una mascota</option>
                {MASCOTAS_MOCK.map((mascota) => (
                  <option key={mascota.id} value={mascota.id}>
                    {mascota.nombre} · {mascota.tamano}
                  </option>
                ))}
              </select>
              {errors.mascotaId ? <small>{errors.mascotaId}</small> : null}
            </label>

            <label className={styles.field}>
              <span>Bloque horario disponible</span>
              <div
                className={`${styles.blockGrid} ${errors.bloqueId ? styles.blockGridError : ""}`}
                role="radiogroup"
                aria-label="Bloques horarios disponibles"
              >
                {BLOQUES_MOCK.map((bloque) => {
                  const isSelected = bloque.id === bloqueId;

                  return (
                    <button
                      key={bloque.id}
                      type="button"
                      className={`${styles.timeBlock} ${isSelected ? styles.timeBlockSelected : ""}`}
                      onClick={() => {
                        setBloqueId(bloque.id);
                        setErrors((prev) => ({ ...prev, bloqueId: undefined }));
                      }}
                      role="radio"
                      aria-checked={isSelected}
                    >
                      <span className={styles.timeBlockDay}>{bloque.label.split(" · ")[0]}</span>
                      <strong>
                        {bloque.inicio} - {bloque.fin}
                      </strong>
                      <span>{bloque.duracionMinutos} minutos</span>
                      <small>Disponible</small>
                    </button>
                  );
                })}
              </div>
              {errors.bloqueId ? <small>{errors.bloqueId}</small> : null}
            </label>

            <label className={styles.field}>
              <span>Comentarios para el paseador</span>
              <textarea
                value={comentarios}
                onChange={(event) => setComentarios(event.target.value)}
                placeholder="Ej: Mi perro se asusta con motos o prefiere paseos tranquilos."
              />
            </label>
          </section>

          <aside className={styles.checkoutCard}>
            <p className={styles.checkoutEyebrow}>Checkout</p>
            <h2>Resumen de costos</h2>

            <div className={styles.checkoutList}>
              <div>
                <span>Paseador</span>
                <strong>{getPaseadorName(paseadorId)}</strong>
              </div>
              <div>
                <span>Mascota</span>
                <strong>{selectedMascota ? selectedMascota.nombre : "Por seleccionar"}</strong>
              </div>
              <div>
                <span>Duracion</span>
                <strong>
                  {selectedBloque ? `${selectedBloque.duracionMinutos} minutos` : "Por seleccionar"}
                </strong>
              </div>
              <div>
                <span>Precio base segun tamano</span>
                <strong>{selectedMascota ? formatCurrency(precioBase) : "--"}</strong>
              </div>
            </div>

            <div className={styles.totalBox}>
              <span>Total a pagar</span>
              <strong>{total ? formatCurrency(total) : "--"}</strong>
            </div>

            <p className={styles.checkoutNote}>
              Al enviar, la solicitud quedara en estado pendiente y este bloque se reservara
              temporalmente cuando exista la conexion con backend.
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
