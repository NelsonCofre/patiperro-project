import AuthInput from "./AuthInput";
import type {
  BaseRegisterForm,
  RegisterFormErrors
} from "../types/register.types";

type Props<TForm extends BaseRegisterForm> = {
  form: TForm;
  errors: RegisterFormErrors<TForm>;
  photoPreview: string;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onBlur: (event: React.FocusEvent<HTMLInputElement>) => void;
  onPhotoChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  styles: Record<string, string>;
  roleLabel: string;
  uploadInputId: string;
  photoDescription: string;
};

export default function RegisterLocationPhotoStep<TForm extends BaseRegisterForm>({
  form,
  errors,
  photoPreview,
  onChange,
  onBlur,
  onPhotoChange,
  styles,
  roleLabel,
  uploadInputId,
  photoDescription
}: Props<TForm>) {
  return (
    <>
      <h3 className={styles.sectionTitle}>Ubicacion y foto</h3>

      <div className={styles.fieldsGrid}>
        <AuthInput
          label="Pais"
          name="pais"
          value={form.pais}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="Ej: Chile"
          error={errors.pais}
        />

        <AuthInput
          label="Region"
          name="region"
          value={form.region}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="Ej: Region Metropolitana"
          error={errors.region}
        />

        <AuthInput
          label="Ciudad"
          name="ciudad"
          value={form.ciudad}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="Ej: Santiago"
          error={errors.ciudad}
        />

        <AuthInput
          label="Comuna"
          name="comuna"
          value={form.comuna}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="Ej: Maipu"
          error={errors.comuna}
        />

        <AuthInput
          label="Calle"
          name="calle"
          value={form.calle}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="Ej: Av. Siempre Viva"
          error={errors.calle}
        />

        <AuthInput
          label="Numeracion"
          name="numeracion"
          value={form.numeracion}
          onChange={onChange}
          onBlur={onBlur}
          placeholder="Ej: 742"
          error={errors.numeracion}
          inputMode="numeric"
        />

        <div className={styles.fullWidth}>
          <AuthInput
            label="Casa o departamento"
            name="casa_departamento"
            value={form.casa_departamento}
            onChange={onChange}
            onBlur={onBlur}
            placeholder="Opcional"
            error={errors.casa_departamento}
          />
        </div>
      </div>

      <div className={styles.photoUpload}>
        {photoPreview ? (
          <img
            className={styles.preview}
            src={photoPreview}
            alt={`Vista previa de la foto del ${roleLabel}`}
          />
        ) : null}

        <p>{photoDescription}</p>

        <label className={styles.uploadButton} htmlFor={uploadInputId}>
          Seleccionar foto
        </label>

        <input
          id={uploadInputId}
          className={styles.hiddenInput}
          type="file"
          accept="image/*"
          onChange={onPhotoChange}
        />

        {form.foto_perfil ? (
          <p className={styles.fileName}>{form.foto_perfil.name}</p>
        ) : (
          <p className={styles.fileName}>Aun no has seleccionado una imagen</p>
        )}

        {errors.foto_perfil ? (
          <p className={styles.errorText}>{errors.foto_perfil}</p>
        ) : null}
      </div>
    </>
  );
}
