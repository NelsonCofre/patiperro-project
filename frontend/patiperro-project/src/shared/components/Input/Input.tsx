// Input generico reutilizable fuera del modulo auth.
import React from "react";

// Definimos el tipo de las props (lo que el componente recibe)
type Props = {
  label: string; // Texto que aparece arriba del input
  type?: string; // Tipo de input (text, password, email, etc.)
  name: string; // Nombre del input (clave para manejar formularios)
  value: string; // Valor actual del input
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void; // Función al escribir
  error?: string; // Mensaje de error (opcional)
  placeholder?: string; // Texto dentro del input
};

// Componente funcional
export default function Input({
  label,
  type = "text", // valor por defecto
  name,
  value,
  onChange,
  error,
  placeholder
}: Props) {
  return (
    <div style={{ marginBottom: "15px" }}>
      
      {/* Label del input */}
      <label>{label}</label>

      {/* Campo de entrada */}
      <input
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        style={{
          display: "block",
          width: "100%",
          padding: "10px",
          border: error ? "2px solid red" : "1px solid #ccc",
          borderRadius: "5px"
        }}
      />

      {/* Mensaje de error */}
      {error && (
        <span style={{ color: "red", fontSize: "12px" }}>
          {error}
        </span>
      )}
    </div>
  );
}
