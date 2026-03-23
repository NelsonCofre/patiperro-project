import { useState } from "react";
import styles from "./AuthInput.module.css";

type Props = {
  label: string;
  name: string;
  value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  type?: string;
  placeholder?: string;
  error?: string;
  icon?: string;
};

export default function AuthInput({
  label,
  name,
  value,
  onChange,
  type = "text",
  placeholder,
  error,
  icon
}: Props) {
  const [showPassword, setShowPassword] = useState(false);

  const isPassword = type === "password";

  return (
    <div className={styles.container}>
      <label className={styles.label}>{label}</label>

      <div className={`${styles.wrapper} ${error ? styles.errorInput : ""}`}>
        
        {/* ICONO */}
        {icon && <span className={styles.icon}>{icon}</span>}

        {/* INPUT */}
        <input
          name={name}
          value={value}
          onChange={onChange}
          type={isPassword && showPassword ? "text" : type}
          placeholder={placeholder}
          className={styles.input}
        />

        {/* 👁 PASSWORD */}
        {isPassword && (
          <span
            className={styles.eye}
            onClick={() => setShowPassword(!showPassword)}
          >
            👁
          </span>
        )}
      </div>

      {/* ERROR */}
      {error && <span className={styles.errorText}>{error}</span>}
    </div>
  );
}