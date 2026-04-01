// Formulario principal para registrar una mascota en el espacio del tutor.
// Usa un hook por pasos para separar datos basicos, perfil y foto.
import type {
  ChangeEvent,
  FocusEvent,
  FormEvent
} from "react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { uploadTutorProfilePhoto } from "../../../auth/services/authServices";
import TutorNavbar from "../../../tutor/components/TutorNavbar/TutorNavbar";
import { useMascotaForm } from "../../hooks/useMascotaForm";
import {
  buildCreateMascotaPayload,
  createMascota,
  fetchEspecies,
  fetchRazas,
  fetchTamanos,
  type EspecieDTO,
  type RazaDTO,
  type TamanoDTO
} from "../../services/mascotaService";
import type { MascotaForm } from "../../types/mascota.types";
import {
  getMascotaAgeLabel,
  getTodayDate,
  keepDecimalWeight,
  keepOnlyDigits
} from "../../utils/mascotaValidators";
import styles from "./AddMascota.module.css";

const STEP_LABELS = [
  { title: "Paso 1", text: "Datos basicos" },
  { title: "Paso 2", text: "Perfil y cuidados" },
  { title: "Paso 3", text: "Foto" }
];

const INITIAL_FORM: MascotaForm = {
  nombre: "",
  especie: "",
  raza: "",
  sexo: "",
  fecha_nacimiento: "",
  peso: "",
  tamano: "",
  comportamiento: "",
  descripcion: "",
  cuidados_especiales: "",
  esterilizado: "",
  numero_chip: "",
  foto: null
};

const SEXOS = ["Macho", "Hembra"];
const ESTERILIZADO_OPTIONS = ["Si", "No"];

export default function AddMascota() {
  const navigate = useNavigate();
  const [successMessage, setSuccessMessage] = useState("");
  const [especies, setEspecies] = useState<EspecieDTO[]>([]);
  const [razas, setRazas] = useState<RazaDTO[]>([]);
  const [tamanos, setTamanos] = useState<TamanoDTO[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState("");

  const {
    currentStep,
    form,
    errors,
    photoPreview,
    submitError,
    isSubmitting,
    setSubmitError,
    setIsSubmitting,
    setErrors,
    setFieldValue,
    handleBlur,
    handlePhotoChange,
    validateEntireForm,
    handleNextStep,
    handlePrevStep,
    resetForm,
    isCurrentStepDisabled
  } = useMascotaForm({
    initialForm: INITIAL_FORM
  });

  useEffect(() => {
    let cancelled = false;
    setCatalogLoading(true);
    setCatalogError("");
    Promise.all([fetchEspecies(), fetchTamanos()])
      .then(([esp, tam]) => {
        if (!cancelled) {
          setEspecies(esp);
          setTamanos(tam);
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setCatalogError(e instanceof Error ? e.message : "No se pudieron cargar los catálogos.");
        }
      })
      .finally(() => {
        if (!cancelled) setCatalogLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const id = Number(form.especie);
    if (!Number.isFinite(id) || id <= 0) {
      setRazas([]);
      return;
    }
    let cancelled = false;
    fetchRazas(id)
      .then((list) => {
        if (!cancelled) setRazas(list);
      })
      .catch(() => {
        if (!cancelled) setRazas([]);
      });
    return () => {
      cancelled = true;
    };
  }, [form.especie]);

  const mascotaAge = getMascotaAgeLabel(form.fecha_nacimiento);

  const handleChange = (
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name } = event.target;
    let { value } = event.target;

    if (name === "especie") {
      setFieldValue("especie", value as never);
      setFieldValue("raza", "" as never);
      return;
    }

    if (name === "peso") {
      value = keepDecimalWeight(value);
    }

    if (name === "numero_chip") {
      value = keepOnlyDigits(value);
    }

    setFieldValue(name as keyof MascotaForm, value as never);
  };

  const handleFieldBlur = (
    event: FocusEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    handleBlur(event);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSuccessMessage("");

    const validationErrors = validateEntireForm();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      setSubmitError("Revisa los campos de todos los pasos antes de guardar.");
      return;
    }

    setSubmitError("");
    setIsSubmitting(true);

    try {
      let fotoPerfilPath: string | undefined;
      if (form.foto) {
        fotoPerfilPath = await uploadTutorProfilePhoto(form.foto);
      }
      const payload = buildCreateMascotaPayload(form, fotoPerfilPath);
      await createMascota(payload);
      resetForm();
      setSuccessMessage("Tu mascota quedó registrada en el sistema.");
    } catch (err) {
      let msg = err instanceof Error ? err.message : "No se pudo guardar la mascota.";
      if (err instanceof Error && /401|403|sesi|autentic/i.test(msg)) {
        msg += " Si perdiste la sesión, vuelve a iniciar sesión como tutor.";
      }
      setSubmitError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className={styles.page}>
      {successMessage ? (
        <div className={styles.toastOverlay}>
          <div className={styles.toastCard} role="dialog" aria-modal="true">
            <span className={styles.toastEyebrow}>Registro exitoso</span>
            <h2 className={styles.toastTitle}>Mascota registrada exitosamente</h2>
            <p className={styles.toastText}>{successMessage}</p>
            <button
              type="button"
              className={styles.toastCloseButton}
              onClick={() => {
                setSuccessMessage("");
                navigate("/tutor/dashboard", { replace: true });
              }}
            >
              Ir al panel
            </button>
          </div>
        </div>
      ) : null}

      <TutorNavbar />

      <section className={styles.shell}>
        <header className={styles.header}>
          <p className={styles.eyebrow}>Nueva mascota</p>
          <h1 className={styles.title}>Crea la ficha de tu mascota</h1>
          <p className={styles.description}>
            Este formulario esta organizado por pasos para que puedas registrar
            la informacion importante sin sentir el proceso pesado.
          </p>
        </header>

        <div className={styles.stepper}>
          {STEP_LABELS.map((step, index) => {
            const isActive = index === currentStep;
            const isDone = index < currentStep;

            return (
              <div
                key={step.title}
                className={`${styles.stepItem} ${isActive ? styles.stepActive : ""} ${
                  isDone ? styles.stepDone : ""
                }`}
              >
                <strong>{step.title}</strong>
                <span>{step.text}</span>
              </div>
            );
          })}
        </div>

        {catalogError ? (
          <p className={styles.submitError} role="alert">
            {catalogError} Asegúrate de estar logueado como tutor y de que el servicio de mascotas
            esté disponible.
          </p>
        ) : null}

        <form className={styles.formShell} onSubmit={handleSubmit}>
          {currentStep === 0 ? (
            <>
              <h2 className={styles.sectionTitle}>Datos basicos</h2>

              <div className={styles.fieldsGrid}>
                <label className={styles.field}>
                  <span>Nombre</span>
                  <input
                    name="nombre"
                    value={form.nombre}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                    placeholder="Ej: Rocky"
                  />
                  {errors.nombre ? <small>{errors.nombre}</small> : null}
                </label>

                <label className={styles.field}>
                  <span>Especie</span>
                  <select
                    name="especie"
                    value={form.especie}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                    disabled={catalogLoading || !!catalogError}
                  >
                    <option value="">
                      {catalogLoading ? "Cargando..." : "Selecciona la especie"}
                    </option>
                    {especies.map((e) => (
                      <option key={e.idEspecie} value={String(e.idEspecie)}>
                        {e.nombre}
                      </option>
                    ))}
                  </select>
                  {errors.especie ? <small>{errors.especie}</small> : null}
                </label>

                <label className={styles.field}>
                  <span>Raza</span>
                  <select
                    name="raza"
                    value={form.raza}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                    disabled={
                      catalogLoading ||
                      !!catalogError ||
                      !form.especie ||
                      razas.length === 0
                    }
                  >
                    <option value="">
                      {!form.especie
                        ? "Primero elige especie"
                        : razas.length === 0
                          ? "Sin razas para esta especie"
                          : "Selecciona la raza"}
                    </option>
                    {razas.map((r) => (
                      <option key={r.idRaza} value={String(r.idRaza)}>
                        {r.nombre}
                      </option>
                    ))}
                  </select>
                  {errors.raza ? <small>{errors.raza}</small> : null}
                </label>

                <label className={styles.field}>
                  <span>Sexo</span>
                  <select
                    name="sexo"
                    value={form.sexo}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                  >
                    <option value="">Selecciona el sexo</option>
                    {SEXOS.map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                  {errors.sexo ? <small>{errors.sexo}</small> : null}
                </label>

                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span>Fecha de nacimiento</span>
                  <input
                    name="fecha_nacimiento"
                    type="date"
                    max={getTodayDate()}
                    value={form.fecha_nacimiento}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                  />
                  {errors.fecha_nacimiento ? (
                    <small>{errors.fecha_nacimiento}</small>
                  ) : null}
                  {mascotaAge ? (
                    <p className={styles.fieldHint}>Edad estimada: {mascotaAge}</p>
                  ) : null}
                </label>
              </div>
            </>
          ) : null}

          {currentStep === 1 ? (
            <>
              <h2 className={styles.sectionTitle}>Perfil y cuidados</h2>

              <div className={styles.fieldsGrid}>
                <label className={styles.field}>
                  <span>Peso (kg)</span>
                  <input
                    name="peso"
                    value={form.peso}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                    placeholder="Ej: 12.5"
                    inputMode="decimal"
                  />
                  {errors.peso ? <small>{errors.peso}</small> : null}
                  {!errors.peso ? (
                    <p className={styles.fieldHint}>Ingresa un numero mayor a 0 kg.</p>
                  ) : null}
                </label>

                <label className={styles.field}>
                  <span>Tamano</span>
                  <select
                    name="tamano"
                    value={form.tamano}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                    disabled={catalogLoading || !!catalogError}
                  >
                    <option value="">
                      {catalogLoading ? "Cargando..." : "Selecciona el tamaño"}
                    </option>
                    {tamanos.map((t) => (
                      <option key={t.idTamano} value={String(t.idTamano)}>
                        {t.nombre}
                        {t.descripcion ? ` — ${t.descripcion}` : ""}
                      </option>
                    ))}
                  </select>
                  {errors.tamano ? <small>{errors.tamano}</small> : null}
                </label>

                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span>Comportamiento</span>
                  <input
                    name="comportamiento"
                    value={form.comportamiento}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                    placeholder="Ej: Jugueton, tranquilo, sociable"
                  />
                  {errors.comportamiento ? (
                    <small>{errors.comportamiento}</small>
                  ) : null}
                </label>

                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span>Descripcion</span>
                  <textarea
                    name="descripcion"
                    value={form.descripcion}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                    rows={4}
                    placeholder="Describe a tu mascota y su personalidad general"
                  />
                  {errors.descripcion ? <small>{errors.descripcion}</small> : null}
                  {!errors.descripcion ? (
                    <p className={styles.fieldHint}>Campo opcional.</p>
                  ) : null}
                </label>

                <label className={`${styles.field} ${styles.fullWidth}`}>
                  <span>Cuidados especiales</span>
                  <textarea
                    name="cuidados_especiales"
                    value={form.cuidados_especiales}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                    rows={3}
                    placeholder="Ej: medicamentos, alergias, necesidades especiales"
                  />
                  {errors.cuidados_especiales ? (
                    <small>{errors.cuidados_especiales}</small>
                  ) : null}
                  {!errors.cuidados_especiales ? (
                    <p className={styles.fieldHint}>Campo opcional.</p>
                  ) : null}
                </label>

                <label className={styles.field}>
                  <span>Esterilizado</span>
                  <select
                    name="esterilizado"
                    value={form.esterilizado}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                  >
                    <option value="">Selecciona una opcion</option>
                    {ESTERILIZADO_OPTIONS.map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>
                  {errors.esterilizado ? <small>{errors.esterilizado}</small> : null}
                </label>

                <label className={styles.field}>
                  <span>Numero de chip</span>
                  <input
                    name="numero_chip"
                    value={form.numero_chip}
                    onChange={handleChange}
                    onBlur={handleFieldBlur}
                    inputMode="numeric"
                    placeholder="Solo numeros"
                  />
                  {errors.numero_chip ? <small>{errors.numero_chip}</small> : null}
                </label>
              </div>
            </>
          ) : null}

          {currentStep === 2 ? (
            <>
              <h2 className={styles.sectionTitle}>Foto principal</h2>

              <div className={styles.photoUpload}>
                {photoPreview ? (
                  <img
                    className={styles.preview}
                    src={photoPreview}
                    alt="Vista previa de la mascota"
                  />
                ) : (
                  <div className={styles.placeholder}>
                    La foto principal ayudara a reconocer mejor a tu mascota.
                  </div>
                )}

                <p className={styles.photoText}>
                  Sube una foto clara de tu mascota para completar su ficha.
                </p>

                <label className={styles.uploadButton} htmlFor="mascota-foto">
                  Seleccionar foto
                </label>

                <input
                  id="mascota-foto"
                  className={styles.hiddenInput}
                  type="file"
                  accept="image/*"
                  onChange={handlePhotoChange}
                />

                {form.foto ? (
                  <p className={styles.fileName}>{form.foto.name}</p>
                ) : (
                  <p className={styles.fileName}>Aun no has seleccionado una foto</p>
                )}

                {errors.foto ? <small className={styles.photoError}>{errors.foto}</small> : null}
              </div>
            </>
          ) : null}

          {submitError ? <p className={styles.submitError}>{submitError}</p> : null}
          <div className={styles.actions}>
            {currentStep > 0 ? (
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={handlePrevStep}
              >
                Volver
              </button>
            ) : null}

            {currentStep < STEP_LABELS.length - 1 ? (
              <button
                type="button"
                className={styles.primaryButton}
                onClick={handleNextStep}
                disabled={isCurrentStepDisabled}
              >
                Continuar
              </button>
            ) : (
              <button
                type="submit"
                className={styles.primaryButton}
                disabled={isCurrentStepDisabled || isSubmitting}
              >
                {isSubmitting ? "Guardando..." : "Guardar mascota"}
              </button>
            )}
          </div>
        </form>
      </section>
    </main>
  );
}
