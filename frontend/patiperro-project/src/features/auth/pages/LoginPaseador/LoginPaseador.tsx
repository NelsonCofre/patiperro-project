// Vista de inicio de sesion exclusiva para el rol paseador.
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthForm from "../../components/AuthForm";
import AuthInput from "../../components/AuthInput";
import { loginPaseador } from "../../services/authServices";
import type { FormErrors } from "../../types/auth.types";
import { getPasswordSecurityError, isValidEmail } from "../../utils/validators";
import styles from "./LoginPaseador.module.css";

type PaseadorLoginForm = {
  email: string;
  password: string;
};

export default function LoginPaseador() {
  const navigate = useNavigate();
  const [form, setForm] = useState<PaseadorLoginForm>({
    email: "",
    password: ""
  });

  const [errors, setErrors] = useState<FormErrors>({});
  const [feedback, setFeedback] = useState<{ type: "success" | "error"; text: string } | null>(
    null
  );
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;

    setForm((prev) => ({
      ...prev,
      [name]: value
    }));

    setErrors((prev) => ({
      ...prev,
      [name]: undefined
    }));
  };

  const handleBlur = (event: React.FocusEvent<HTMLInputElement>) => {
    const { name } = event.target;
    const validationErrors = validate();

    setErrors((prev) => ({
      ...prev,
      [name]: validationErrors[name as keyof FormErrors]
    }));
  };

  const validate = (): FormErrors => {
    const nextErrors: FormErrors = {};

    if (!isValidEmail(form.email)) {
      nextErrors.email = "Formato de correo invalido";
    }

    const passwordError = getPasswordSecurityError(form.password);
    if (passwordError) {
      nextErrors.password = passwordError;
    }

    return nextErrors;
  };

  const isSubmitDisabled =
    isSubmitting ||
    !form.email.trim() ||
    !form.password ||
    Object.keys(validate()).length > 0;

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setFeedback(null);

    const validationErrors = validate();
    setErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setIsSubmitting(true);

    try {
      await loginPaseador({
        correo: form.email.trim(),
        contrasena: form.password
      });
      navigate("/paseador/dashboard", { replace: true });
    } catch (error) {
      setFeedback({
        type: "error",
        text: error instanceof Error ? error.message : "No se pudo iniciar sesion."
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthForm
      title="Iniciar sesion como paseador"
      onSubmit={handleSubmit}
      buttonText="Ingresar"
      isSubmitting={isSubmitting}
      isSubmitDisabled={isSubmitDisabled}
      reverseLayout
    >
      <div className={styles.header}>
        <p>Accede con tu cuenta de paseador para gestionar servicios y reservas.</p>
      </div>

      {feedback ? (
        <div
          className={feedback.type === "success" ? styles.alertSuccess : styles.alertError}
          role="alert"
        >
          {feedback.text}
        </div>
      ) : null}

      <AuthInput
        label="Correo electronico"
        name="email"
        value={form.email}
        onChange={handleChange}
        onBlur={handleBlur}
        placeholder="correo@ejemplo.com"
        error={errors.email}
        inputMode="email"
        autoComplete="email"
      />

      <AuthInput
        label="Contrasena"
        name="password"
        type="password"
        value={form.password}
        onChange={handleChange}
        onBlur={handleBlur}
        placeholder="******"
        error={errors.password}
        autoComplete="current-password"
      />

      <p className={styles.helperText}>
        <a href="/">Olvidaste tu contrasena?</a>
      </p>

      <p className={styles.footerText}>
        No tienes cuenta? <Link to="/register/paseador">Registrate como paseador</Link>
      </p>
    </AuthForm>
  );
}
