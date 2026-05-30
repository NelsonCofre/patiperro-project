// Verificacion de identidad: subida de un PDF (max. 5 MB) con aprobacion automatica.
import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import FileUploader from "../../components/FileUploader/FileUploader";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import {
  descargarDocumentoVerificacion,
  fetchVerificacionEstado,
  subirDocumentoVerificacion,
  validarPdfVerificacion,
  type VerificacionIdentidadDTO
} from "../../services/verificacionPaseadorService";
import styles from "./PaseadorVerificacion.module.css";

function formatFecha(iso: string | null): string {
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  return d.toLocaleString("es-CL", {
    dateStyle: "medium",
    timeStyle: "short"
  });
}

function estadoBadgeClass(estado: VerificacionIdentidadDTO["estado"]): string {
  switch (estado) {
    case "APROBADO":
      return styles.badgeOk;
    case "RECHAZADO":
      return styles.badgeError;
    case "EN_PROCESO":
      return styles.badgePending;
    default:
      return styles.badgeNeutral;
  }
}

export default function PaseadorVerificacion() {
  const [estado, setEstado] = useState<VerificacionIdentidadDTO | null>(null);
  const [loadStatus, setLoadStatus] = useState<"idle" | "loading" | "error">("idle");
  const [loadError, setLoadError] = useState("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [uploadProgress, setUploadProgress] = useState(0);

  const cargarEstado = useCallback(async () => {
    setLoadStatus("loading");
    setLoadError("");
    try {
      const data = await fetchVerificacionEstado();
      setEstado(data);
      setLoadStatus("idle");
    } catch (err) {
      setLoadStatus("error");
      setLoadError(err instanceof Error ? err.message : "Error al cargar.");
    }
  }, []);

  useEffect(() => {
    void cargarEstado();
  }, [cargarEstado]);

  const handleFileChange = (file: File | null) => {
    setSelectedFile(file);
    setFileError("");
    setSubmitError("");
    setSuccessMessage("");
    setUploadProgress(0);
    if (!file) return;

    const err = validarPdfVerificacion(file);
    if (err) {
      setFileError(err);
      setSelectedFile(null);
    }
  };

  const handleSubmit = async () => {
    if (!selectedFile) {
      setFileError("Selecciona un archivo PDF antes de enviar.");
      return;
    }

    setIsSubmitting(true);
    setSubmitError("");
    setSuccessMessage("");
    setUploadProgress(0);

    try {
      const data = await subirDocumentoVerificacion(selectedFile, {
        onProgress: (percent) => setUploadProgress(percent)
      });
      setEstado(data);
      setSelectedFile(null);
      setUploadProgress(100);
      setSuccessMessage("Documento recibido. Tu identidad quedo verificada.");
    } catch (err) {
      setUploadProgress(0);
      setSubmitError(err instanceof Error ? err.message : "No se pudo subir el documento.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDownload = async () => {
    setIsDownloading(true);
    setSubmitError("");
    try {
      const blob = await descargarDocumentoVerificacion();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "documento-verificacion.pdf";
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "No se pudo descargar el documento.");
    } finally {
      setIsDownloading(false);
    }
  };

  const puedeSubir = estado?.puedeSubir ?? false;
  const isLoading = loadStatus === "loading";

  return (
    <main className={styles.page}>
      {successMessage ? (
        <div className={styles.toastOverlay}>
          <div className={styles.toastCard} role="dialog" aria-modal="true">
            <span className={styles.toastEyebrow}>Verificacion</span>
            <h2 className={styles.toastTitle}>Documento enviado</h2>
            <p className={styles.toastText}>{successMessage}</p>
            <button
              type="button"
              className={styles.toastCloseButton}
              onClick={() => setSuccessMessage("")}
            >
              Cerrar
            </button>
          </div>
        </div>
      ) : null}

      <PaseadorNavbar />

      <section className={styles.hero}>
        <div>
          <p className={styles.eyebrow}>Verificacion de identidad</p>
          <h1 className={styles.title}>Confirma tu identidad con un documento</h1>
          <p className={styles.description}>
            Sube un PDF con tu cedula de identidad (maximo 5 MB). Al enviarlo, tu cuenta se
            marca como verificada de inmediato para generar confianza con los tutores.
          </p>
        </div>

        <div className={styles.infoCard}>
          <span className={styles.infoLabel}>Estado actual</span>
          {isLoading ? (
            <strong>Cargando...</strong>
          ) : estado ? (
            <span className={`${styles.badge} ${estadoBadgeClass(estado.estado)}`}>
              {estado.estadoEtiqueta}
            </span>
          ) : (
            <strong>-</strong>
          )}
        </div>
      </section>

      {loadError ? (
        <div className={styles.bannerError} role="alert">
          <p>{loadError}</p>
          <p className={styles.bannerHint}>
            Si no has iniciado sesion, ve a <Link to="/login/paseador">iniciar sesion</Link>.
          </p>
        </div>
      ) : null}

      {submitError ? (
        <div className={styles.bannerError} role="alert">
          {submitError}
        </div>
      ) : null}

      {!isLoading && !loadError && estado ? (
        <section className={styles.formCard}>
          <div className={styles.metaGrid}>
            <div>
              <span className={styles.metaLabel}>Enviado</span>
              <strong>{formatFecha(estado.enviadoEn)}</strong>
            </div>
            <div>
              <span className={styles.metaLabel}>Revisado</span>
              <strong>{formatFecha(estado.revisadoEn)}</strong>
            </div>
            <div>
              <span className={styles.metaLabel}>Documento en servidor</span>
              <strong>{estado.tieneDocumento ? "Si" : "No"}</strong>
            </div>
          </div>

          {estado.estado === "RECHAZADO" && estado.motivoRechazo ? (
            <div className={styles.rechazoBox} role="alert">
              <strong>Motivo de rechazo</strong>
              <p>{estado.motivoRechazo}</p>
            </div>
          ) : null}

          {estado.tieneDocumento ? (
            <div className={styles.actionsRow}>
              <button
                type="button"
                className={styles.secondaryButton}
                onClick={() => void handleDownload()}
                disabled={isDownloading}
              >
                {isDownloading ? "Descargando..." : "Descargar mi documento"}
              </button>
            </div>
          ) : null}

          {puedeSubir ? (
            <>
              <div className={styles.sectionHeader}>
                <div>
                  <p className={styles.cardEyebrow}>Documento</p>
                  <h2>Sube tu cedula en PDF</h2>
                </div>
              </div>

              <FileUploader
                accept="application/pdf,.pdf"
                disabled={isSubmitting}
                error={fileError}
                helperText="Arrastra tu PDF aqui o haz clic para seleccionarlo. Maximo 5 MB."
                selectedFile={selectedFile}
                title="Seleccionar PDF de verificacion"
                onFileSelect={handleFileChange}
              />

              {isSubmitting ? (
                <div
                  className={styles.progressCard}
                  role="status"
                  aria-live="polite"
                  aria-label={`Subida en progreso ${uploadProgress}%`}
                >
                  <div className={styles.progressHeader}>
                    <strong>Subiendo documento</strong>
                    <span>{uploadProgress}%</span>
                  </div>
                  <div className={styles.progressTrack} aria-hidden="true">
                    <div
                      className={styles.progressFill}
                      style={{ width: `${uploadProgress}%` }}
                    />
                  </div>
                </div>
              ) : null}

              <div className={styles.actions}>
                <button
                  type="button"
                  className={styles.primaryButton}
                  onClick={() => void handleSubmit()}
                  disabled={isSubmitting || !selectedFile || Boolean(fileError)}
                >
                  {isSubmitting ? "Enviando..." : "Enviar y verificar"}
                </button>
              </div>
            </>
          ) : (
            <p className={styles.verifiedNote}>
              Tu identidad ya esta verificada. No necesitas subir otro documento.
            </p>
          )}
        </section>
      ) : null}
    </main>
  );
}
