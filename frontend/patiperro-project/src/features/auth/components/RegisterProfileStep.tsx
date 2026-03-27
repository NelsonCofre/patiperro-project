import AuthInput from "./AuthInput";
import type {
  BaseRegisterForm,
  RegisterFormErrors
} from "../types/register.types";

type Props<TForm extends BaseRegisterForm> = {
  form: TForm;
  errors: RegisterFormErrors<TForm>;
  onChange: (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
  onBlur: (
    event: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => void;
  styles: Record<string, string>;
  profileTitle: string;
  biographyPlaceholder: string;
  maxBirthDate: string;
};

export default function RegisterProfileStep<TForm extends BaseRegisterForm>({
  form,
  errors,
  onChange,
  onBlur,
  styles,
  profileTitle,
  biographyPlaceholder,
  maxBirthDate
}: Props<TForm>) {
  return (
    <>
      <h3 className={styles.sectionTitle}>{profileTitle}</h3>

      <div className={styles.fieldsGrid}>
        <div className={styles.fullWidth}>
          <AuthInput
            label="RUT"
            name="rut"
            value={form.rut}
            onChange={onChange}
            onBlur={onBlur}
            placeholder="Ej: 12345678-9"
            error={errors.rut}
          />
        </div>

        <AuthInput
          label="Primer nombre"
          name="primer_nombre"
          value={form.primer_nombre}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="Ej: Mario"
          error={errors.primer_nombre}
          autoComplete="given-name"
        />

        <AuthInput
          label="Segundo nombre"
          name="segundo_nombre"
          value={form.segundo_nombre}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="Opcional"
          error={errors.segundo_nombre}
        />

        <AuthInput
          label="Apellido paterno"
          name="apellido_paterno"
          value={form.apellido_paterno}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="Ej: Perez"
          error={errors.apellido_paterno}
          autoComplete="family-name"
        />

        <AuthInput
          label="Apellido materno"
          name="apellido_materno"
          value={form.apellido_materno}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="Ej: Soto"
          error={errors.apellido_materno}
        />

        <AuthInput
          label="Fecha de nacimiento"
          name="fecha_nacimiento"
          type="date"
          value={form.fecha_nacimiento}
          onChange={onChange}
          onBlur={onBlur}
          error={errors.fecha_nacimiento}
          max={maxBirthDate}
          autoComplete="bday"
        />

        <AuthInput
          label="Telefono"
          name="telefono"
          value={form.telefono}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="56912345678"
          error={errors.telefono}
          inputMode="numeric"
          autoComplete="tel"
        />

        <p className={styles.fieldHint}>
          Selecciona tu fecha desde el calendario y escribe el telefono solo con
          numeros.
        </p>

        <div className={`${styles.textareaContainer} ${styles.fullWidth}`}>
          <label className={styles.textareaLabel} htmlFor="biografia">
            Biografia
          </label>
          <textarea
            id="biografia"
            name="biografia"
            value={form.biografia}
            onChange={onChange}
            onBlur={onBlur}
            placeholder={biographyPlaceholder}
            className={`${styles.textarea} ${
              errors.biografia ? styles.textareaError : ""
            }`}
            rows={4}
          />
          {errors.biografia ? (
            <span className={styles.errorText}>{errors.biografia}</span>
          ) : null}
        </div>
      </div>
    </>
  );
}
