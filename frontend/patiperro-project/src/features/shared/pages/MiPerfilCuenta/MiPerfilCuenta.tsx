import { useEffect, useMemo, useRef, useState, type ChangeEvent, type FormEvent } from "react";
import AuthInput from "../../../auth/components/AuthInput";
import {
  changePaseadorPassword,
  changeTutorPassword,
  fetchPaseadorMiPerfil,
  fetchTutorMiPerfil,
  resolveFotoPerfilUrl,
  updatePaseadorFotoPerfil,
  updateTutorFotoPerfil,
  type MiPerfilDTO
} from "../../../auth/services/perfilCuentaService";
import { getPasswordSecurityError } from "../../../auth/utils/validators";
import { validateProfilePhoto } from "../../../auth/utils/profilePhotoValidators";
import PaseadorNavbar from "../../../paseador/components/PaseadorNavbar/PaseadorNavbar";
import TutorNavbar from "../../../tutor/components/TutorNavbar/TutorNavbar";
import styles from "./MiPerfilCuenta.module.css";

type Role = "tutor" | "paseador";

type Props = {
  role: Role;
};

type PasswordForm = {
  contrasenaActual: string;
  contrasenaNueva: string;
  contrasenaConfirmacion: string;
};

const EMPTY_PASSWORD_FORM: PasswordForm = {
  contrasenaActual: "",
  contrasenaNueva: "",
  contrasenaConfirmacion: ""
};

function buildInitials(nombre?: string | null): string {
  const parts = String(nombre ?? "")
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  if (parts.length === 0) return "PP";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0] ?? ""}${parts[1][0] ?? ""}`.toUpperCase();
}

export default function MiPerfilCuenta({ role }: Props) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [perfil, setPerfil] = useState<MiPerfilDTO | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [photoError, setPhotoError] = useState("");
  const [photoSuccess, setPhotoSuccess] = useState("");
  const [isUploadingPhoto, setIsUploadingPhoto] = useState(false);
  const [passwordForm, setPasswordForm] = useState<PasswordForm>(EMPTY_PASSWORD_FORM);
  const [passwordErrors, setPasswordErrors] = useState<Partial<Record<keyof PasswordForm, string>>>({});
  const [passwordSubmitError, setPasswordSubmitError] = useState("");
  const [passwordSuccess, setPasswordSuccess] = useState("");
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  const Navbar = role === "tutor" ? TutorNavbar : PaseadorNavbar;
  const fetchPerfil = role === "tutor" ? fetchTutorMiPerfil : fetchPaseadorMiPerfil;
  const updateFoto = role === "tutor" ? updateTutorFotoPerfil : updatePaseadorFotoPerfil;
  const changePassword = role === "tutor" ? changeTutorPassword : changePaseadorPassword;
  const roleLabel = role === "tutor" ? "Tutor" : "Paseador";

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setLoadError("");

    fetchPerfil()
      .then((data) => {
        if (!cancelled) setPerfil(data);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setLoadError(err instanceof Error ? err.message : "No se pudo cargar tu perfil.");
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [fetchPerfil]);

  const fotoUrl = useMemo(() => resolveFotoPerfilUrl(perfil?.fotoPerfil), [perfil?.fotoPerfil]);
  const initials = useMemo(() => buildInitials(perfil?.nombreCompleto), [perfil?.nombreCompleto]);

  async function handlePhotoChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    event.target.value = "";
    setPhotoError("");
    setPhotoSuccess("");

    const validationError = validateProfilePhoto(file);
    if (validationError || !file) {
      setPhotoError(validationError ?? "Debes seleccionar una imagen");
      return;
    }

    setIsUploadingPhoto(true);
    try {
      const hadPhoto = Boolean(perfil?.fotoPerfil);
      const updated = await updateFoto(file);
      setPerfil(updated);
      setPhotoSuccess(
        hadPhoto
          ? "Foto actualizada. La imagen anterior fue reemplazada."
          : "Foto de perfil guardada correctamente."
      );
    } catch (err) {
      setPhotoError(err instanceof Error ? err.message : "No se pudo guardar la foto.");
    } finally {
      setIsUploadingPhoto(false);
    }
  }

  function validatePasswordForm(): Partial<Record<keyof PasswordForm, string>> {
    const nextErrors: Partial<Record<keyof PasswordForm, string>> = {};
    if (!passwordForm.contrasenaActual.trim()) {
      nextErrors.contrasenaActual = "Ingresa tu contraseña actual";
    }
    const nuevaError = getPasswordSecurityError(passwordForm.contrasenaNueva);
    if (nuevaError) {
      nextErrors.contrasenaNueva = nuevaError;
    }
    if (!passwordForm.contrasenaConfirmacion.trim()) {
      nextErrors.contrasenaConfirmacion = "Confirma la nueva contraseña";
    } else if (passwordForm.contrasenaNueva !== passwordForm.contrasenaConfirmacion) {
      nextErrors.contrasenaConfirmacion = "Las contraseñas no coinciden";
    }
    return nextErrors;
  }

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPasswordSubmitError("");
    setPasswordSuccess("");

    const nextErrors = validatePasswordForm();
    setPasswordErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setIsChangingPassword(true);
    try {
      await changePassword({
        contrasenaActual: passwordForm.contrasenaActual,
        contrasenaNueva: passwordForm.contrasenaNueva
      });
      setPasswordForm(EMPTY_PASSWORD_FORM);
      setPasswordSuccess("Tu contraseña fue actualizada correctamente.");
    } catch (err) {
      setPasswordSubmitError(err instanceof Error ? err.message : "No se pudo cambiar la contraseña.");
    } finally {
      setIsChangingPassword(false);
    }
  }

  return (
    <main className={styles.page}>
      <Navbar />

      <section className={styles.shell}>
        <header className={styles.header}>
          <p className={styles.eyebrow}>Mi perfil</p>
          <h1 className={styles.title}>Tu cuenta y seguridad</h1>
          <p className={styles.description}>
            Gestiona tu imagen de perfil y mantén tu acceso protegido con una contraseña segura.
          </p>
        </header>

        {(loadError || photoSuccess || passwordSuccess) && (
          <div className={styles.alertStack}>
            {loadError ? (
              <p className={styles.errorBanner} role="alert">
                {loadError}
              </p>
            ) : null}
            {photoSuccess ? <p className={styles.successBanner}>{photoSuccess}</p> : null}
            {passwordSuccess ? <p className={styles.successBanner}>{passwordSuccess}</p> : null}
          </div>
        )}

        {isLoading ? (
          <>
            <div className={styles.skeletonHero} aria-hidden="true" />
            <div className={styles.skeletonBlock} aria-hidden="true" />
          </>
        ) : (
          <>
            <article className={styles.profileHero}>
              <div className={styles.avatarWrap}>
                <div className={styles.avatarRing}>
                  {fotoUrl ? (
                    <img className={styles.avatar} src={fotoUrl} alt={`Foto de ${perfil?.nombreCompleto}`} />
                  ) : (
                    <div className={styles.avatarPlaceholder}>{initials}</div>
                  )}
                </div>
                <span className={styles.roleBadge}>{roleLabel}</span>
              </div>

              <div className={styles.heroBody}>
                <h2 className={styles.heroName}>{perfil?.nombreCompleto || "Usuario"}</h2>
                <p className={styles.heroSubtitle}>
                  {role === "tutor"
                    ? "Tu perfil visible para paseadores cuando reservas un paseo."
                    : "Tu perfil visible para tutores cuando recibes solicitudes."}
                </p>
                <div className={styles.metaRow}>
                  {perfil?.correo ? (
                    <span className={styles.metaChip}>
                      <span className={styles.metaChipIcon} aria-hidden="true">
                        @
                      </span>
                      {perfil.correo}
                    </span>
                  ) : null}
                  {perfil?.telefono ? (
                    <span className={styles.metaChip}>
                      <span className={styles.metaChipIcon} aria-hidden="true">
                        #
                      </span>
                      {perfil.telefono}
                    </span>
                  ) : null}
                </div>

                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                  className={styles.hiddenInput}
                  aria-hidden="true"
                  tabIndex={-1}
                  onChange={(event) => void handlePhotoChange(event)}
                />
                <div className={styles.heroActions}>
                  <button
                    type="button"
                    className={styles.changePhotoButton}
                    disabled={isUploadingPhoto}
                    onClick={() => fileInputRef.current?.click()}
                  >
                    {isUploadingPhoto ? "Guardando foto..." : "Cambiar foto"}
                  </button>
                  {photoError ? (
                    <p className={styles.fieldError} role="alert">
                      {photoError}
                    </p>
                  ) : null}
                </div>
              </div>
            </article>

            <article className={styles.sectionCard}>
                <div className={styles.sectionHeader}>
                  <span className={styles.sectionEyebrow}>Seguridad</span>
                  <h3 className={styles.sectionTitle}>Cambiar contraseña</h3>
                  <p className={styles.sectionHint}>
                    Usa al menos 8 caracteres con mayúscula, número y carácter especial.
                  </p>
                </div>

                <form className={styles.passwordPanel} onSubmit={(event) => void handlePasswordSubmit(event)}>
                  <AuthInput
                    label="Contraseña actual"
                    name="contrasenaActual"
                    type="password"
                    value={passwordForm.contrasenaActual}
                    autoComplete="current-password"
                    error={passwordErrors.contrasenaActual}
                    onChange={(event) => {
                      setPasswordForm((prev) => ({ ...prev, contrasenaActual: event.target.value }));
                      setPasswordErrors((prev) => ({ ...prev, contrasenaActual: undefined }));
                    }}
                  />
                  <AuthInput
                    label="Nueva contraseña"
                    name="contrasenaNueva"
                    type="password"
                    value={passwordForm.contrasenaNueva}
                    autoComplete="new-password"
                    error={passwordErrors.contrasenaNueva}
                    onChange={(event) => {
                      setPasswordForm((prev) => ({ ...prev, contrasenaNueva: event.target.value }));
                      setPasswordErrors((prev) => ({ ...prev, contrasenaNueva: undefined }));
                    }}
                  />
                  <AuthInput
                    label="Confirmar nueva contraseña"
                    name="contrasenaConfirmacion"
                    type="password"
                    value={passwordForm.contrasenaConfirmacion}
                    autoComplete="new-password"
                    error={passwordErrors.contrasenaConfirmacion}
                    onChange={(event) => {
                      setPasswordForm((prev) => ({ ...prev, contrasenaConfirmacion: event.target.value }));
                      setPasswordErrors((prev) => ({ ...prev, contrasenaConfirmacion: undefined }));
                    }}
                  />

                  {passwordSubmitError ? (
                    <p className={styles.fieldError} role="alert">
                      {passwordSubmitError}
                    </p>
                  ) : null}

                  <button type="submit" className={styles.primaryButton} disabled={isChangingPassword}>
                    {isChangingPassword ? "Guardando..." : "Actualizar contraseña"}
                  </button>
                </form>
              </article>
          </>
        )}
      </section>
    </main>
  );
}
