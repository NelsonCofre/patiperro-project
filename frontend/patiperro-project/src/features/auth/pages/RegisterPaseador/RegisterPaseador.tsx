import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthInput from "../../components/AuthInput";
import { registerPaseador } from "../../services/authServices";
import authStyles from "../../styles/auth.module.css";
import styles from "./RegisterPaseador.module.css";

type PaseadorRegisterForm = {
  rut: string;
  primer_nombre: string;
  segundo_nombre: string;
  apellido_paterno: string;
  apellido_materno: string;
  fecha_nacimiento: string;
  telefono: string;
  correo: string;
  biografia: string;
  contrasena: string;
  confirmar_contrasena: string;
  pais: string;
  region: string;
  ciudad: string;
  calle: string;
  comuna: string;
  numeracion: string;
  casa_departamento: string;
  foto_perfil: File | null;
};

type FormErrors = Partial<Record<keyof PaseadorRegisterForm, string>>;

const INITIAL_FORM: PaseadorRegisterForm = {
  rut: "",
  primer_nombre: "",
  segundo_nombre: "",
  apellido_paterno: "",
  apellido_materno: "",
  fecha_nacimiento: "",
  telefono: "",
  correo: "",
  biografia: "",
  contrasena: "",
  confirmar_contrasena: "",
  pais: "",
  region: "",
  ciudad: "",
  calle: "",
  comuna: "",
  numeracion: "",
  casa_departamento: "",
  foto_perfil: null
};

const STEP_LABELS = [
  { title: "Paso 1", text: "Acceso" },
  { title: "Paso 2", text: "Perfil del paseador" },
  { title: "Paso 3", text: "Ubicacion y foto" }
];

const DEFAULT_PASEADOR_PHOTO_URL =
  "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=400&q=80";

export default function RegisterPaseador() {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(0);
  const [form, setForm] = useState<PaseadorRegisterForm>(INITIAL_FORM);
  const [errors, setErrors] = useState<FormErrors>({});
  const [photoPreview, setPhotoPreview] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!form.foto_perfil) {
      setPhotoPreview("");
      return;
    }

    const previewUrl = URL.createObjectURL(form.foto_perfil);
    setPhotoPreview(previewUrl);

    return () => {
      URL.revokeObjectURL(previewUrl);
    };
  }, [form.foto_perfil]);

  const currentTitle = useMemo(() => STEP_LABELS[currentStep].text, [currentStep]);

  const handleChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: value
    }));
  };

  const handlePhotoChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null;

    setForm((prev) => ({
      ...prev,
      foto_perfil: file
    }));
  };

  const validateStep = (step: number): FormErrors => {
    const nextErrors: FormErrors = {};

    if (step === 0) {
      if (!form.correo.includes("@")) {
        nextErrors.correo = "Ingresa un correo valido";
      }

      if (form.contrasena.length < 6) {
        nextErrors.contrasena = "La contrasena debe tener al menos 6 caracteres";
      }

      if (form.confirmar_contrasena !== form.contrasena) {
        nextErrors.confirmar_contrasena = "Las contrasenas no coinciden";
      }
    }

    if (step === 1) {
      if (!form.rut.trim()) {
        nextErrors.rut = "El RUT es obligatorio";
      }

      if (!form.primer_nombre.trim()) {
        nextErrors.primer_nombre = "El primer nombre es obligatorio";
      }

      if (!form.apellido_paterno.trim()) {
        nextErrors.apellido_paterno = "El apellido paterno es obligatorio";
      }

      if (!form.apellido_materno.trim()) {
        nextErrors.apellido_materno = "El apellido materno es obligatorio";
      }

      if (!form.fecha_nacimiento) {
        nextErrors.fecha_nacimiento = "La fecha de nacimiento es obligatoria";
      }

      if (form.telefono.trim().length < 8) {
        nextErrors.telefono = "Ingresa un telefono valido";
      }

      if (!form.biografia.trim()) {
        nextErrors.biografia = "La biografia es obligatoria";
      }
    }

    if (step === 2) {
      if (!form.pais.trim()) {
        nextErrors.pais = "El pais es obligatorio";
      }

      if (!form.region.trim()) {
        nextErrors.region = "La region es obligatoria";
      }

      if (!form.ciudad.trim()) {
        nextErrors.ciudad = "La ciudad es obligatoria";
      }

      if (!form.calle.trim()) {
        nextErrors.calle = "La calle es obligatoria";
      }

      if (!form.comuna.trim()) {
        nextErrors.comuna = "La comuna es obligatoria";
      }

      if (!form.numeracion.trim()) {
        nextErrors.numeracion = "La numeracion es obligatoria";
      }

      if (!form.foto_perfil) {
        nextErrors.foto_perfil = "Debes subir una foto del paseador";
      }
    }

    return nextErrors;
  };

  const handleNextStep = () => {
    const validationErrors = validateStep(currentStep);
    setErrors(validationErrors);
    setSubmitError("");

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setCurrentStep((prev) => prev + 1);
  };

  const handlePrevStep = () => {
    setErrors({});
    setSubmitError("");
    setCurrentStep((prev) => prev - 1);
  };

  const buildRegisterPayload = () => ({
    rut: form.rut.trim(),
    primerNombre: form.primer_nombre.trim(),
    segundoNombre: form.segundo_nombre.trim(),
    apellidoPaterno: form.apellido_paterno.trim(),
    apellidoMaterno: form.apellido_materno.trim(),
    fechaNacimiento: form.fecha_nacimiento,
    telefono: Number(form.telefono),
    correo: form.correo.trim(),
    contrasena: form.contrasena,
    fotoPerfil: DEFAULT_PASEADOR_PHOTO_URL,
    biografia: form.biografia.trim(),
    pais: form.pais.trim(),
    region: form.region.trim(),
    ciudad: form.ciudad.trim(),
    calle: form.calle.trim(),
    comuna: form.comuna.trim(),
    numeracion: Number(form.numeracion),
    casaDepartamento: form.casa_departamento.trim(),
    fotos: []
  });

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const validationErrors = validateStep(currentStep);
    setErrors(validationErrors);
    setSubmitError("");

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setIsSubmitting(true);

    try {
      await registerPaseador(buildRegisterPayload());
      navigate("/login/paseador");
    } catch (error) {
      setSubmitError(
        error instanceof Error
          ? error.message
          : "Ocurrio un error al registrar al paseador."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderStepContent = () => {
    if (currentStep === 0) {
      return (
        <>
          <h3 className={styles.sectionTitle}>Crea tu acceso</h3>

          <div className={styles.fieldsGrid}>
            <div className={styles.fullWidth}>
              <AuthInput
                label="Correo"
                name="correo"
                value={form.correo}
                onChange={handleChange}
                placeholder="correo@ejemplo.com"
                error={errors.correo}
              />
            </div>

            <AuthInput
              label="Contrasena"
              name="contrasena"
              type="password"
              value={form.contrasena}
              onChange={handleChange}
              placeholder="******"
              error={errors.contrasena}
            />

            <AuthInput
              label="Confirmar contrasena"
              name="confirmar_contrasena"
              type="password"
              value={form.confirmar_contrasena}
              onChange={handleChange}
              placeholder="******"
              error={errors.confirmar_contrasena}
            />
          </div>
        </>
      );
    }

    if (currentStep === 1) {
      return (
        <>
          <h3 className={styles.sectionTitle}>Completa tu perfil</h3>

          <div className={styles.fieldsGrid}>
            <div className={styles.fullWidth}>
              <AuthInput
                label="RUT"
                name="rut"
                value={form.rut}
                onChange={handleChange}
                placeholder="Ej: 12345678-9"
                error={errors.rut}
              />
            </div>

            <AuthInput
              label="Primer nombre"
              name="primer_nombre"
              value={form.primer_nombre}
              onChange={handleChange}
              placeholder="Ej: Mario"
              error={errors.primer_nombre}
            />

            <AuthInput
              label="Segundo nombre"
              name="segundo_nombre"
              value={form.segundo_nombre}
              onChange={handleChange}
              placeholder="Opcional"
              error={errors.segundo_nombre}
            />

            <AuthInput
              label="Apellido paterno"
              name="apellido_paterno"
              value={form.apellido_paterno}
              onChange={handleChange}
              placeholder="Ej: Perez"
              error={errors.apellido_paterno}
            />

            <AuthInput
              label="Apellido materno"
              name="apellido_materno"
              value={form.apellido_materno}
              onChange={handleChange}
              placeholder="Ej: Soto"
              error={errors.apellido_materno}
            />

            <AuthInput
              label="Fecha de nacimiento"
              name="fecha_nacimiento"
              type="date"
              value={form.fecha_nacimiento}
              onChange={handleChange}
              error={errors.fecha_nacimiento}
            />

            <AuthInput
              label="Telefono"
              name="telefono"
              value={form.telefono}
              onChange={handleChange}
              placeholder="+56 9 1234 5678"
              error={errors.telefono}
            />

            <div className={`${styles.textareaContainer} ${styles.fullWidth}`}>
              <label className={styles.textareaLabel} htmlFor="biografia">
                Biografia
              </label>
              <textarea
                id="biografia"
                name="biografia"
                value={form.biografia}
                onChange={handleChange}
                placeholder="Cuentanos un poco sobre ti, tu experiencia y tu relacion con las mascotas."
                className={`${styles.textarea} ${
                  errors.biografia ? styles.textareaError : ""
                }`}
                rows={4}
              />
              {errors.biografia ? (
                <span className={styles.errorText}>{errors.biografia}</span>
              ) : null}
            </div>
          </div>
        </>
      );
    }

    return (
      <>
        <h3 className={styles.sectionTitle}>Ubicacion y foto</h3>

        <div className={styles.fieldsGrid}>
          <AuthInput
            label="Pais"
            name="pais"
            value={form.pais}
            onChange={handleChange}
            placeholder="Ej: Chile"
            error={errors.pais}
          />

          <AuthInput
            label="Region"
            name="region"
            value={form.region}
            onChange={handleChange}
            placeholder="Ej: Region Metropolitana"
            error={errors.region}
          />

          <AuthInput
            label="Ciudad"
            name="ciudad"
            value={form.ciudad}
            onChange={handleChange}
            placeholder="Ej: Santiago"
            error={errors.ciudad}
          />

          <AuthInput
            label="Comuna"
            name="comuna"
            value={form.comuna}
            onChange={handleChange}
            placeholder="Ej: Maipu"
            error={errors.comuna}
          />

          <AuthInput
            label="Calle"
            name="calle"
            value={form.calle}
            onChange={handleChange}
            placeholder="Ej: Av. Siempre Viva"
            error={errors.calle}
          />

          <AuthInput
            label="Numeracion"
            name="numeracion"
            value={form.numeracion}
            onChange={handleChange}
            placeholder="Ej: 742"
            error={errors.numeracion}
          />

          <div className={styles.fullWidth}>
            <AuthInput
              label="Casa o departamento"
              name="casa_departamento"
              value={form.casa_departamento}
              onChange={handleChange}
              placeholder="Opcional"
              error={errors.casa_departamento}
            />
          </div>
        </div>

        <div className={styles.photoUpload}>
          {photoPreview ? (
            <img
              className={styles.preview}
              src={photoPreview}
              alt="Vista previa de la foto del paseador"
            />
          ) : null}

          <p>
            Sube una foto clara de perfil para completar el registro y generar una
            mejor presentacion de tu cuenta.
          </p>

          <label className={styles.uploadButton} htmlFor="paseador-photo">
            Seleccionar foto
          </label>

          <input
            id="paseador-photo"
            className={styles.hiddenInput}
            type="file"
            accept="image/*"
            onChange={handlePhotoChange}
          />

          {form.foto_perfil ? (
            <p className={styles.fileName}>{form.foto_perfil.name}</p>
          ) : (
            <p className={styles.fileName}>Aun no has seleccionado una imagen</p>
          )}

          {errors.foto_perfil ? (
            <p className={styles.errorText}>{errors.foto_perfil}</p>
          ) : null}
        </div>
      </>
    );
  };

  return (
    <div className={authStyles.container}>
      <div className={authStyles.left}>
        <div className={authStyles.leftContent}>
          <h1 className={authStyles.logo}>Patiperro</h1>
          <p className={authStyles.subtitle}>
            Registro exclusivo para paseadores. Completa tus datos para ofrecer tus servicios.
          </p>
        </div>
      </div>

      <div className={authStyles.right}>
        <div className={authStyles.card}>
          <div className={styles.header}>
            <h2 className={authStyles.title}>Registro de paseador</h2>
            <p>
              Completa este flujo de 3 pasos para dejar lista tu cuenta profesional
              de paseador con perfil, ubicacion y foto.
            </p>
          </div>

          <div className={styles.stepper}>
            {STEP_LABELS.map((step, index) => (
              <div
                key={step.title}
                className={`${styles.stepItem} ${
                  index === currentStep
                    ? styles.stepActive
                    : index < currentStep
                      ? styles.stepDone
                      : ""
                }`}
              >
                <strong>{step.title}</strong>
                <span>{step.text}</span>
              </div>
            ))}
          </div>

          <form onSubmit={handleSubmit}>
            <div className={styles.formShell}>{renderStepContent()}</div>

            {submitError ? (
              <p className={styles.submitError}>{submitError}</p>
            ) : null}

            <div className={styles.actions}>
              {currentStep > 0 ? (
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={handlePrevStep}
                  disabled={isSubmitting}
                >
                  Volver
                </button>
              ) : null}

              {currentStep < STEP_LABELS.length - 1 ? (
                <button
                  type="button"
                  className={styles.primaryButton}
                  onClick={handleNextStep}
                  disabled={isSubmitting}
                >
                  Continuar
                </button>
              ) : (
                <button
                  type="submit"
                  className={styles.primaryButton}
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "Creando cuenta..." : "Crear cuenta"}
                </button>
              )}
            </div>
          </form>

          <p className={styles.footerText}>
            Ya tienes cuenta? <Link to="/login/paseador">Inicia sesion</Link>
          </p>

          <p className={styles.stepMeta}>Paso actual: {currentTitle}</p>
        </div>
      </div>
    </div>
  );
}
