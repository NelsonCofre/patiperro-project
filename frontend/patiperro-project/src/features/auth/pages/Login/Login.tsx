import { useState } from "react";
import AuthForm from "../../components/AuthForm";
import AuthInput from "../../components/AuthInput";
import type { FormErrors } from "../../types/auth.types";

// 🧠 Tipo del formulario
type LoginForm = {
  email: string;
  password: string;
};

export default function Login() {

  // 🧠 Estado del formulario
  const [form, setForm] = useState<LoginForm>({
    email: "",
    password: ""
  });

  // 🧠 Estado de errores
  const [errors, setErrors] = useState<FormErrors>({});

  // 🧠 Maneja cambios
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    setForm(prev => ({
      ...prev,
      [name]: value
    }));
  };

  // 🧠 Validaciones
  const validate = (): FormErrors => {
    const newErrors: FormErrors = {};

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

    console.log("Login:", form);
  };

  return (
    <AuthForm
      title="Iniciar sesión"
      onSubmit={handleSubmit}
      buttonText="Ingresar"
    >

      {/* 🧩 EMAIL */}
      <AuthInput
        label="Correo electrónico"
        name="email"
        value={form.email}
        onChange={handleChange}
        placeholder="correo@ejemplo.com"
        error={errors.email}
      />

      {/* 🧩 PASSWORD */}
      <AuthInput
        label="Contraseña"
        name="password"
        type="password"
        value={form.password}
        onChange={handleChange}
        placeholder="******"
        error={errors.password}
      />

      {/* 🔗 LINK A REGISTER */}
      <p style={{ marginTop: "15px", textAlign: "center" }}>
        ¿No tienes cuenta? <a href="/register">Regístrate</a>
      </p>

    </AuthForm>
  );
}