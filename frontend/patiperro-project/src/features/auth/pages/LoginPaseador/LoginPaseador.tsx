import { useState } from "react";
import { Link } from "react-router-dom";
import AuthForm from "../../components/AuthForm";
import AuthInput from "../../components/AuthInput";
import { loginPaseador } from "../../services/authServices";
import type { FormErrors } from "../../types/auth.types";
import styles from "./LoginPaseador.module.css";

type PaseadorLoginForm = {
  email: string;
  password: string;
};

export default function LoginPaseador() {
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
  };

  const validate = (): FormErrors => {
    const nextErrors: FormErrors = {};

    if (!form.email.includes("@")) {
      nextErrors.email = "Ingresa un correo valido";
    }

    if (form.password.length < 6) {
      nextErrors.password = "La contrasena debe tener al menos 6 caracteres";
    }

    return nextErrors;
  };

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
      const res = await loginPaseador({
        correo: form.email.trim(),
        contrasena: form.password
      });
      setFeedback({
        type: "success",
        text: res.mensaje ?? "Sesion iniciada correctamente."
      });
      setForm({ email: "", password: "" });
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
        placeholder="correo@ejemplo.com"
        error={errors.email}
      />

      <AuthInput
        label="Contrasena"
        name="password"
        type="password"
        value={form.password}
        onChange={handleChange}
        placeholder="******"
        error={errors.password}
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
