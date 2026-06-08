// Paso 1 del registro multi-step.
// Agrupa los campos de acceso compartidos entre tutor y paseador.
import AuthInput from "./AuthInput";
import type {
  BaseRegisterForm,
  RegisterFormErrors
} from "../types/register.types";

type Props<TForm extends BaseRegisterForm> = {
  form: TForm;
  errors: RegisterFormErrors<TForm>;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onBlur: (event: React.FocusEvent<HTMLInputElement>) => void;
  correoHint?: string;
  styles: Record<string, string>;
};

export default function RegisterAccessStep<TForm extends BaseRegisterForm>({
  form,
  errors,
  onChange,
  onBlur,
  correoHint,
  styles
}: Props<TForm>) {
  return (
    <>
      <h3 className={styles.sectionTitle}>Crea tu acceso</h3>

      <div className={styles.fieldsGrid}>
        <div className={styles.fullWidth}>
          <AuthInput
            label="Correo"
            name="correo"
            value={form.correo}
            onChange={onChange}
            onBlur={onBlur}
            placeholder="correo@ejemplo.com"
            error={errors.correo}
            inputMode="email"
            autoComplete="email"
          />
          {correoHint && !errors.correo ? (
            <p className={styles.fieldHint}>{correoHint}</p>
          ) : null}
        </div>

        <AuthInput
          label="Contraseña"
          name="contrasena"
          type="password"
          value={form.contrasena}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="******"
          error={errors.contrasena}
          autoComplete="new-password"
        />

        <AuthInput
          label="Confirmar contraseña"
          name="confirmar_contrasena"
          type="password"
          value={form.confirmar_contrasena}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="******"
          error={errors.confirmar_contrasena}
          autoComplete="new-password"
        />
      </div>
    </>
  );
}
