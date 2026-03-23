import { useState } from "react";
import AuthForm from "../../components/AuthForm";
import AuthInput from "../../components/AuthInput";
import type { FormErrors } from "../../types/auth.types";
import styles from "../../styles/auth.module.css";

// 🧠 Tipo del formulario (mejora escalabilidad)
type RegisterForm = {
  name: string;
  email: string;
  password: string;
  role: "TUTOR" | "PASEADOR";
};

export default function Register() {

  // 🧠 Estado del formulario tipado
  const [form, setForm] = useState<RegisterForm>({
    name: "",
    email: "",
    password: "",
    role: "TUTOR"
  });

  // 🧠 Estado de errores
  const [errors, setErrors] = useState<FormErrors>({});

  // 🧠 Maneja cambios dinámicos en inputs
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    setForm(prev => ({
      ...prev,
      [name]: value
    }));
  };

  // 🧠 Cambia el rol
  const handleRoleChange = (role: RegisterForm["role"]) => {
    setForm(prev => ({
      ...prev,
      role
    }));
  };

  // 🧠 Validaciones
  const validate = (): FormErrors => {
    const newErrors: FormErrors = {};

    if (!form.name.trim()) {
      newErrors.name = "El nombre es obligatorio";
    }

    if (!form.email.includes("@")) {
      newErrors.email = "Email inválido";
    }

    if (form.password.length < 6) {
      newErrors.password = "Mínimo 6 caracteres";
    }

    return newErrors;
  };

  // 🧠 Submit
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const validationErrors = validate();
    setErrors(validationErrors);

    if (Object.keys(validationErrors).length > 0) return;

    console.log("Datos del registro:", form);
  };

  return (
    <AuthForm
      title="Crear cuenta"
      onSubmit={handleSubmit}
      buttonText="Registrarse"
    >

      {/* 🧩 INPUT NOMBRE */}
      <AuthInput
        label="Nombre"
        name="name"
        value={form.name}
        onChange={handleChange}
        placeholder="Tu nombre"
        error={errors.name}
      />

      {/* 🧩 INPUT EMAIL */}
      <AuthInput
        label="Correo electrónico"
        name="email"
        value={form.email}
        onChange={handleChange}
        placeholder="correo@ejemplo.com"
        error={errors.email}
      />

      {/* 🧩 INPUT PASSWORD */}
      <AuthInput
        label="Contraseña"
        name="password"
        type="password"
        value={form.password}
        onChange={handleChange}
        placeholder="******"
        error={errors.password}
      />

      {/* 🧩 ROLES */}
      <div className={styles.roleContainer}>
        <p>Selecciona tu rol:</p>

        <div className={styles.roleButtons}>
          <button
            type="button"
            className={`${styles.roleButton} ${
              form.role === "TUTOR" ? styles.activeRole : ""
            }`}
            onClick={() => handleRoleChange("TUTOR")}
          >
            🐶 Soy Tutor
          </button>

          <button
            type="button"
            className={`${styles.roleButton} ${
              form.role === "PASEADOR" ? styles.activeRole : ""
            }`}
            onClick={() => handleRoleChange("PASEADOR")}
          >
            🚶 Soy Paseador
          </button>
        </div>
      </div>

      {/* 🔗 LINK A LOGIN */}
      <p style={{ marginTop: "15px", textAlign: "center" }}>
        ¿Ya tienes cuenta? <a href="/login">Inicia sesión</a>
      </p>

    </AuthForm>
  );
}