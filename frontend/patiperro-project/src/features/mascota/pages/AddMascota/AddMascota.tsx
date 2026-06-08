import type {
  ChangeEvent,
  FocusEvent,
  FormEvent
} from "react";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { resolveApiUrl } from "../../../../config/api";
import TutorNavbar from "../../../tutor/components/TutorNavbar/TutorNavbar";
import { useMascotaForm } from "../../hooks/useMascotaForm";
import {
  buildCreateMascotaPayload,
  createMascota,
  fetchEspecies,
  fetchMascotaById,
  fetchRazas,
  fetchTamanos,
  updateMascota,
  uploadMascotaFotoPerfil,
  type EspecieDTO,
  type RazaDTO,
  type TamanoDTO
} from "../../services/mascotaService";
import type { MascotaForm } from "../../types/mascota.types";
import {
  getMascotaAgeLabel,
  getTodayDate,
  keepDecimalWeight,
  keepOnlyDigits,
  validateMascotaPhoto
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

type SuccessState = {
  eyebrow: string;
  title: string;
  message: string;
  buttonLabel: string;
  redirectTo: string;
};

export default function AddMascota() {
  const navigate = useNavigate();
  const { idMascota: idMascotaParam } = useParams();
  const mascotaId = Number.parseInt(idMascotaParam ?? "", 10);
  const isEditMode = Number.isFinite(mascotaId) && mascotaId > 0;

  const [successState, setSuccessState] = useState<SuccessState | null>(null);
  const [especies, setEspecies] = useState<EspecieDTO[]>([]);
  const [razas, setRazas] = useState<RazaDTO[]>([]);
  const [tamanos, setTamanos] = useState<TamanoDTO[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState("");
  const [mascotaLoading, setMascotaLoading] = useState(isEditMode);
  const [fotoActualPath, setFotoActualPath] = useState("");
  const [fotoActualUrl, setFotoActualUrl] = useState("");

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
    hydrateForm,
    handleBlur,
    handlePhotoChange,
    validateEntireForm,
    handleNextStep,
    handlePrevStep,
    resetForm,
    isCurrentStepDisabled
  } = useMascotaForm({
    initialForm: INITIAL_FORM,
    requirePhoto: !isEditMode || !fotoActualPath
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
      .catch((error: unknown) => {
        if (!cancelled) {
          setCatalogError(
            error instanceof Error ? error.message : "No se pudieron cargar los catálogos."
          );
        }
      })
      .finally(() => {
        if (!cancelled) {
          setCatalogLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!isEditMode) {
      setMascotaLoading(false);
      return;
    }

    let cancelled = false;
    setMascotaLoading(true);
    setSubmitError("");

    fetchMascotaById(mascotaId)
      .then((mascota) => {
        if (cancelled) return;
        hydrateForm(mascota.form);
        setFotoActualPath(mascota.fotoPerfilPath);
        setFotoActualUrl(mascota.fotoPerfilUrl);
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setSubmitError(
            error instanceof Error ? error.message : "No se pudo cargar la ficha de la mascota."
          );
        }
      })
      .finally(() => {
        if (!cancelled) {
          setMascotaLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [hydrateForm, isEditMode, mascotaId, setSubmitError]);

  useEffect(() => {
    const id = Number(form.especie);
    if (!Number.isFinite(id) || id <= 0) {
      setRazas([]);
      return;
    }

    let cancelled = false;
    fetchRazas(id)
      .then((list) => {
        if (!cancelled) {
          setRazas(list);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRazas([]);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [form.especie]);

  const mascotaAge = getMascotaAgeLabel(form.fecha_nacimiento);
  const displayedPhotoUrl = photoPreview || fotoActualUrl;
  const pageEyebrow = isEditMode ? "Editar mascota" : "Nueva mascota";
  const pageTitle = isEditMode
    ? "Actualiza la ficha de tu mascota"
    : "Crea la ficha de tu mascota";
  const pageDescription = isEditMode
    ? "Puedes reemplazar la foto principal o ajustar sus datos sin perder la información actual."
    : "Este formulario está organizado por pasos para que puedas registrar la información importante sin sentir el proceso pesado.";

  const fileMessage = useMemo(() => {
    if (form.foto) {
      return form.foto.name;
    }
    if (fotoActualPath) {
      return "Mantendras la foto actual hasta guardar una nueva.";
    }
    return "Aún no has seleccionado una foto";
  }, [form.foto, fotoActualPath]);

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
    setSuccessState(null);

    const validationErrors = validateEntireForm();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      setSubmitError("Revisa los campos de todos los pasos antes de guardar.");
      return;
    }

    setSubmitError("");
    setIsSubmitting(true);

    try {
      const isReplacingPhoto = Boolean(form.foto);
      const payload = buildCreateMascotaPayload(form);

      if (isEditMode) {
        await updateMascota(mascotaId, payload);
        if (form.foto) {
          const photoError = validateMascotaPhoto(form.foto, { required: true });
          if (photoError) {
            setErrors((prev) => ({ ...prev, foto: photoError }));
            setSubmitError(photoError);
            return;
          }
          const fotoResult = await uploadMascotaFotoPerfil(mascotaId, form.foto);
          setFotoActualPath(fotoResult.fotoPerfil);
          setFotoActualUrl(resolveApiUrl(fotoResult.fotoPerfil));
        }
        setSuccessState({
          eyebrow: isReplacingPhoto ? "Foto actualizada" : "Cambios guardados",
          title: isReplacingPhoto ? "Foto de mascota actualizada" : "Mascota actualizada",
          message: isReplacingPhoto
            ? "La nueva foto quedó guardada y ya reemplaza la anterior en la ficha de tu mascota."
            : "La información de tu mascota quedó actualizada correctamente.",
          buttonLabel: "Ver mis mascotas",
          redirectTo: "/tutor/mascotas"
        });
      } else {
        const created = await createMascota(payload);
        if (form.foto) {
          const photoError = validateMascotaPhoto(form.foto, { required: true });
          if (photoError) {
            setErrors((prev) => ({ ...prev, foto: photoError }));
            setSubmitError(photoError);
            return;
          }
          await uploadMascotaFotoPerfil(created.idMascota, form.foto);
        }
        resetForm();
        setFotoActualPath("");
        setFotoActualUrl("");
        setSuccessState({
          eyebrow: "Registro exitoso",
          title: "Mascota registrada exitosamente",
          message: "Tu mascota quedó registrada en el sistema y su foto principal ya está lista para futuras reservas.",
          buttonLabel: "Ver mis mascotas",
          redirectTo: "/tutor/mascotas"
        });
      }
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
      {successState ? (
        <div className={styles.toastOverlay}>
          <div className={styles.toastCard} role="dialog" aria-modal="true">
            <span className={styles.toastEyebrow}>{successState.eyebrow}</span>
            <h2 className={styles.toastTitle}>{successState.title}</h2>
            <p className={styles.toastText}>{successState.message}</p>
            <button
              type="button"
              className={styles.toastCloseButton}
              onClick={() => {
                const nextRoute = successState.redirectTo;
                setSuccessState(null);
                navigate(nextRoute, { replace: true });
              }}
            >
              {successState.buttonLabel}
            </button>
          </div>
        </div>
      ) : null}

      <TutorNavbar />

      <section className={styles.shell}>
        <header className={styles.header}>
          <p className={styles.eyebrow}>{pageEyebrow}</p>
          <h1 className={styles.title}>{pageTitle}</h1>
          <p className={styles.description}>{pageDescription}</p>
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
            {catalogError} Asegurate de estar logueado como tutor y de que el servicio de
            mascotas este disponible.
          </p>
        ) : null}

        <form className={styles.formShell} onSubmit={handleSubmit}>
          {mascotaLoading ? (
            <p className={styles.fieldHint}>Cargando la ficha actual de tu mascota...</p>
          ) : null}

          {!mascotaLoading && currentStep === 0 ? (
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
                    {especies.map((especie) => (
                      <option key={especie.idEspecie} value={String(especie.idEspecie)}>
                        {especie.nombre}
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
                      catalogLoading || !!catalogError || !form.especie || razas.length === 0
                    }
                  >
                    <option value="">
                      {!form.especie
                        ? "Primero elige especie"
                        : razas.length === 0
                          ? "Sin razas para esta especie"
                          : "Selecciona la raza"}
                    </option>
                    {razas.map((raza) => (
                      <option key={raza.idRaza} value={String(raza.idRaza)}>
                        {raza.nombre}
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

          {!mascotaLoading && currentStep === 1 ? (
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
                    <p className={styles.fieldHint}>Ingresa un número mayor a 0 kg.</p>
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
                    {tamanos.map((tamano) => (
                      <option key={tamano.idTamano} value={String(tamano.idTamano)}>
                        {tamano.nombre}
                        {tamano.descripcion ? ` - ${tamano.descripcion}` : ""}
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
                  {errors.comportamiento ? <small>{errors.comportamiento}</small> : null}
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
                    <option value="">Selecciona una opción</option>
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
                    placeholder="Solo números"
                  />
                  {errors.numero_chip ? <small>{errors.numero_chip}</small> : null}
                </label>
              </div>
            </>
          ) : null}

          {!mascotaLoading && currentStep === 2 ? (
            <>
              <h2 className={styles.sectionTitle}>Foto principal</h2>

              <div className={styles.photoUpload}>
                {displayedPhotoUrl ? (
                  <img
                    className={styles.preview}
                    src={displayedPhotoUrl}
                    alt="Vista previa de la mascota"
                  />
                ) : (
                  <div className={styles.placeholder}>
                    La foto principal ayudara a reconocer mejor a tu mascota.
                  </div>
                )}

                <p className={styles.photoText}>
                  Sube una foto clara en JPG, JPEG o PNG para que el paseador pueda reconocer
                  facilmente a tu mascota.
                </p>

                <label className={styles.uploadButton} htmlFor="mascota-foto">
                  {fotoActualPath ? "Reemplazar foto" : "Seleccionar foto"}
                </label>

                <input
                  id="mascota-foto"
                  className={styles.hiddenInput}
                  type="file"
                  accept="image/png,image/jpeg,.jpg,.jpeg,.png"
                  onChange={handlePhotoChange}
                />

                <p className={styles.fileName}>{fileMessage}</p>

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
                disabled={mascotaLoading}
              >
                Volver
              </button>
            ) : null}

            {currentStep < STEP_LABELS.length - 1 ? (
              <button
                type="button"
                className={styles.primaryButton}
                onClick={handleNextStep}
                disabled={isCurrentStepDisabled || mascotaLoading}
              >
                Continuar
              </button>
            ) : (
              <button
                type="submit"
                className={styles.primaryButton}
                disabled={isCurrentStepDisabled || isSubmitting || mascotaLoading}
              >
                {isSubmitting
                  ? "Guardando..."
                  : isEditMode
                    ? "Guardar cambios"
                    : "Guardar mascota"}
              </button>
            )}
          </div>
        </form>
      </section>
    </main>
  );
}
