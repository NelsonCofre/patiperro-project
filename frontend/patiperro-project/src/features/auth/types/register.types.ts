export type BaseRegisterForm = {
  rut: string;
  primer_nombre: string;
  segundo_nombre: string;
  apellido_paterno: string;
  apellido_materno: string;
  fecha_nacimiento: string;
  telefono: string;
  correo: string;
  biografia: string;
  contrasena: string;
  confirmar_contrasena: string;
  pais: string;
  region: string;
  ciudad: string;
  calle: string;
  comuna: string;
  numeracion: string;
  casa_departamento: string;
  foto_perfil: File | null;
};

export type RegisterFormErrors<TForm> = Partial<Record<keyof TForm, string>>;
