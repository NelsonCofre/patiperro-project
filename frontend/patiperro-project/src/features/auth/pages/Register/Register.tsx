import { useState } from "react";
import styles from "./Register.module.css";

export default function Register() {
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: ""
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    console.log(form);
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>🐶 Patiperro</h1>
        <p className={styles.subtitle}>Crea tu cuenta</p>

        <form onSubmit={handleSubmit} className={styles.form}>
          <input name="name" placeholder="Nombre" onChange={handleChange} className={styles.input} />
          <input name="email" placeholder="Email" onChange={handleChange} className={styles.input} />
          <input name="password" type="password" placeholder="Password" onChange={handleChange} className={styles.input} />
          <input name="confirmPassword" type="password" placeholder="Confirmar password" onChange={handleChange} className={styles.input} />

          <button className={styles.button}>Registrarse</button>
        </form>

        <p className={styles.footer}>
          ¿Ya tienes cuenta? <a href="/">Inicia sesión</a>
        </p>
      </div>
    </div>
  );
}