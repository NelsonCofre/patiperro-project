// Registro del tutor.
// Usa un hook compartido para el flujo multi-step y mantiene reglas propias del rol.
import { useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import RegisterAccessStep from "../../components/RegisterAccessStep";
import RegisterLocationPhotoStep from "../../components/RegisterLocationPhotoStep";
import RegisterProfileStep from "../../components/RegisterProfileStep";
import { useRegisterForm } from "../../hooks/useRegisterForm";
import { registerTutor, uploadTutorProfilePhoto } from "../../services/authServices";
import authStyles from "../../styles/auth.module.css";
import type {
  BaseRegisterForm,
  RegisterFormErrors
} from "../../types/register.types";
import {
  getMaxBirthDate,
  getPasswordSecurityError,
  hasOnlyLetters,
  isValidEmail,
  isValidRut,
  keepOnlyDigits,
  normalizeRut
} from "../../utils/validators";
import styles from "./RegisterTutor.module.css";

type TutorRegisterForm = BaseRegisterForm;
type FormErrors = RegisterFormErrors<TutorRegisterForm>;

const INITIAL_FORM: TutorRegisterForm = {
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
  { title: "Paso 2", text: "Perfil del tutor" },
  { title: "Paso 3", text: "Ubicacion y foto" }
];

function validateTutorField(
  name: keyof TutorRegisterForm,
  value: string | File | null,
  form: TutorRegisterForm
): string | undefined {
  if (name === "correo") {
    if (!String(value).trim()) {
      return "El correo es obligatorio";
    }

    if (!isValidEmail(String(value))) {
      return "Formato de correo invalido";
    }
  }

  if (name === "contrasena") {
    return getPasswordSecurityError(String(value)) ?? undefined;
  }

  if (name === "confirmar_contrasena") {
    if (!String(value)) {
      return "Debes confirmar tu contrasena";
    }

    if (String(value) !== form.contrasena) {
      return "Las contrasenas no coinciden";
    }
  }

  if (name === "rut") {
    if (!String(value).trim()) {
      return "El RUT es obligatorio";
    }

    if (!isValidRut(String(value))) {
      return "Ingresa un RUT valido";
    }
  }

  if (
    name === "primer_nombre" ||
    name === "segundo_nombre" ||
    name === "apellido_paterno" ||
    name === "apellido_materno"
  ) {
    const fieldValue = String(value).trim();
    const isOptional = name === "segundo_nombre";

    if (!fieldValue && !isOptional) {
      return "Este campo es obligatorio";
    }

    if (fieldValue && !hasOnlyLetters(fieldValue)) {
      return "Solo se permiten letras";
    }
  }

  if (name === "fecha_nacimiento" && !String(value)) {
    return "La fecha de nacimiento es obligatoria";
  }

  if (name === "telefono") {
    const phone = String(value);

    if (!phone) {
      return "El telefono es obligatorio";
    }

    if (phone.length < 8 || phone.length > 11) {
      return "Ingresa un telefono valido";
    }
  }

  if (name === "biografia" && !String(value).trim()) {
    return "La biografia es obligatoria";
  }

  if (
    (name === "pais" ||
      name === "region" ||
      name === "ciudad" ||
      name === "calle" ||
      name === "comuna") &&
    !String(value).trim()
  ) {
    return "Este campo es obligatorio";
  }

  if (name === "numeracion" && !String(value).trim()) {
    return "La numeracion es obligatoria";
  }

  if (name === "foto_perfil" && !value) {
    return "Debes subir una foto del tutor";
  }

  return undefined;
}

export default function RegisterTutor() {
  const navigate = useNavigate();
  const {
    currentStep,
    form,
    errors,
    photoPreview,
    submitError,
    isSubmitting,
    setSubmitError,
    setIsSubmitting,
    setFieldValue,
    handleBlur,
    handlePhotoChange,
    validateStep,
    handleNextStep,
    handlePrevStep,
    isCurrentStepDisabled
  } = useRegisterForm({
    initialForm: INITIAL_FORM,
    validateField: validateTutorField
  });

  const currentTitle = useMemo(() => STEP_LABELS[currentStep].text, [currentStep]);

  const handleChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name } = event.target;
    let { value } = event.target;

    if (name === "telefono" || name === "numeracion") {
      value = keepOnlyDigits(value);
    }

    if (name === "rut") {
      value = normalizeRut(value);
    }

    if (
      name === "primer_nombre" ||
      name === "segundo_nombre" ||
      name === "apellido_paterno" ||
      name === "apellido_materno"
    ) {
      value = value.replace(/[0-9]/g, "");
    }

    setFieldValue(name as keyof TutorRegisterForm, value as never);
  };

  const buildRegisterPayload = (fotoPerfil: string) => {
    return {
      rut: form.rut.trim(),
      primerNombre: form.primer_nombre.trim(),
      segundoNombre: form.segundo_nombre.trim(),
      apellidoPaterno: form.apellido_paterno.trim(),
      apellidoMaterno: form.apellido_materno.trim(),
      fechaNacimiento: form.fecha_nacimiento,
      telefono: form.telefono.trim(),
      correo: form.correo.trim(),
      contrasena: form.contrasena,
      fotoPerfil,
      biografia: form.biografia.trim(),
      pais: form.pais.trim(),
      region: form.region.trim(),
      ciudad: form.ciudad.trim(),
      calle: form.calle.trim(),
      comuna: form.comuna.trim(),
      numeracion: Number(form.numeracion),
      casaDepartamento: form.casa_departamento.trim(),
      fotos: [] as string[]
    };
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const validationErrors = validateStep(currentStep) as FormErrors;
    setSubmitError("");

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    if (!form.foto_perfil) {
      setSubmitError("Debes seleccionar una foto de perfil.");
      return;
    }

    setIsSubmitting(true);

    try {
      const fotoPath = await uploadTutorProfilePhoto(form.foto_perfil);
      await registerTutor(buildRegisterPayload(fotoPath));
      navigate("/login/tutor");
    } catch (error) {
      setSubmitError(
        error instanceof Error
          ? error.message
          : "Ocurrio un error al registrar al tutor."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderStepContent = () => {
    if (currentStep === 0) {
      return (
        <RegisterAccessStep
          form={form}
          errors={errors}
          onChange={handleChange}
          onBlur={handleBlur}
          styles={styles}
        />
      );
    }

    if (currentStep === 1) {
      return (
        <RegisterProfileStep
          form={form}
          errors={errors}
          onChange={handleChange}
          onBlur={handleBlur}
          styles={styles}
          profileTitle="Completa tu perfil"
          biographyPlaceholder="Cuentanos un poco sobre ti, tu experiencia y tu relacion con las mascotas."
          maxBirthDate={getMaxBirthDate()}
        />
      );
    }

    return (
      <RegisterLocationPhotoStep
        form={form}
        errors={errors}
        photoPreview={photoPreview}
        onChange={handleChange}
        onBlur={handleBlur}
        onPhotoChange={handlePhotoChange}
        styles={styles}
        roleLabel="tutor"
        uploadInputId="tutor-photo"
        photoDescription="Sube una imagen (jpg, png, gif o webp). Al crear la cuenta se sube al servidor y se guarda la URL en tu perfil."
      />
    );
  };

  return (
    <div className={authStyles.container}>
      <div className={authStyles.left}>
        <div className={authStyles.leftContent}>
          <h1 className={authStyles.logo}>Patiperro</h1>
          <p className={authStyles.subtitle}>
            Registro exclusivo para tutores. Completa tus datos para crear tu
            cuenta.
          </p>
        </div>
      </div>

      <div className={authStyles.right}>
        <div className={authStyles.card}>
          <div className={styles.header}>
            <h2 className={authStyles.title}>Registro de tutor</h2>
            <p>
              Completa este flujo de 3 pasos para dejar lista tu cuenta de tutor
              con informacion personal, direccion y foto de perfil.
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

            {submitError ? <p className={styles.submitError}>{submitError}</p> : null}

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
                  disabled={isCurrentStepDisabled}
                >
                  Continuar
                </button>
              ) : (
                <button
                  type="submit"
                  className={styles.primaryButton}
                  disabled={isCurrentStepDisabled}
                >
                  {isSubmitting ? "Creando cuenta..." : "Crear cuenta"}
                </button>
              )}
            </div>
          </form>

          <p className={styles.footerText}>
            Ya tienes cuenta? <Link to="/login/tutor">Inicia sesion</Link>
          </p>

          <p className={styles.stepMeta}>Paso actual: {currentTitle}</p>
        </div>
      </div>
    </div>
  );
}
