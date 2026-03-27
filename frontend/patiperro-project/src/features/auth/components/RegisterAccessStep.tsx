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
  styles: Record<string, string>;
};

export default function RegisterAccessStep<TForm extends BaseRegisterForm>({
  form,
  errors,
  onChange,
  onBlur,
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
        </div>

        <AuthInput
          label="Contrasena"
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
          label="Confirmar contrasena"
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
