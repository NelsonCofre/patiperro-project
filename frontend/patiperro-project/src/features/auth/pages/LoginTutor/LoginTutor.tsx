import { useState } from "react";
import { Link } from "react-router-dom";
import AuthForm from "../../components/AuthForm";
import AuthInput from "../../components/AuthInput";
import type { FormErrors } from "../../types/auth.types";
import styles from "./LoginTutor.module.css";

type TutorLoginForm = {
  email: string;
  password: string;
};

export default function LoginTutor() {
  const [form, setForm] = useState<TutorLoginForm>({
    email: "",
    password: ""
  });

  const [errors, setErrors] = useState<FormErrors>({});

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

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();

    const validationErrors = validate();
    setErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    console.log("Login tutor listo para conectar:", form);
  };

  return (
    <AuthForm title="Iniciar sesion como tutor" onSubmit={handleSubmit} buttonText="Ingresar">
      <div className={styles.header}>
        <p>Accede con tu cuenta de tutor para gestionar reservas y paseos.</p>
      </div>

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
        No tienes cuenta? <Link to="/register/tutor">Registrate como tutor</Link>
      </p>
    </AuthForm>
  );
}
