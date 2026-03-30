// Tipos base del formulario de mascota.
// Definen la estructura de datos y errores del flujo de creacion.
export type MascotaForm = {
  nombre: string;
  especie: string;
  raza: string;
  sexo: string;
  fecha_nacimiento: string;
  peso: string;
  tamano: string;
  comportamiento: string;
  descripcion: string;
  cuidados_especiales: string;
  esterilizado: string;
  numero_chip: string;
  foto: File | null;
};

export type MascotaFormErrors = Partial<Record<keyof MascotaForm, string>>;
