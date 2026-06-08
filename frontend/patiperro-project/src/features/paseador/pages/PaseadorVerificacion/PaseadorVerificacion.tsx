// Verificacion de identidad: subida de un PDF (max. 5 MB) con aprobacion automatica.

import { useCallback, useEffect, useState } from "react";

import { Link } from "react-router-dom";

import DocumentPreviewModal from "../../components/DocumentPreviewModal/DocumentPreviewModal";

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



function mensajeErrorDocumento(error: unknown): string {

  const raw = error instanceof Error ? error.message : "No se pudo cargar el documento.";

  if (/no encontrado|no disponible|404/i.test(raw)) {

    return "El archivo ya no está disponible. Puedes subir uno nuevo con el botón Cambiar documento.";

  }

  return raw;

}



export default function PaseadorVerificacion() {

  const [estado, setEstado] = useState<VerificacionIdentidadDTO | null>(null);

  const [loadStatus, setLoadStatus] = useState<"idle" | "loading" | "error">("idle");

  const [loadError, setLoadError] = useState("");

  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const [pendingPreviewUrl, setPendingPreviewUrl] = useState<string | null>(null);

  const [fileError, setFileError] = useState("");

  const [submitError, setSubmitError] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [isDownloading, setIsDownloading] = useState(false);

  const [successMessage, setSuccessMessage] = useState("");

  const [uploadProgress, setUploadProgress] = useState(0);

  const [showUploadedPreview, setShowUploadedPreview] = useState(false);

  const [uploadedPreviewUrl, setUploadedPreviewUrl] = useState<string | null>(null);

  const [uploadedPreviewError, setUploadedPreviewError] = useState("");

  const [isLoadingUploadedPreview, setIsLoadingUploadedPreview] = useState(false);

  const [showReplaceForm, setShowReplaceForm] = useState(false);



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



  useEffect(() => {

    if (!selectedFile) {

      setPendingPreviewUrl(null);

      return;

    }



    const url = URL.createObjectURL(selectedFile);

    setPendingPreviewUrl(url);

    return () => {

      URL.revokeObjectURL(url);

    };

  }, [selectedFile]);



  useEffect(() => {

    return () => {

      if (uploadedPreviewUrl) {

        URL.revokeObjectURL(uploadedPreviewUrl);

      }

    };

  }, [uploadedPreviewUrl]);



  const resetUploadedPreviewCache = () => {

    if (uploadedPreviewUrl) {

      URL.revokeObjectURL(uploadedPreviewUrl);

      setUploadedPreviewUrl(null);

    }

    setUploadedPreviewError("");

  };



  const loadUploadedPreview = async () => {

    resetUploadedPreviewCache();

    setIsLoadingUploadedPreview(true);

    try {

      const blob = await descargarDocumentoVerificacion();

      const url = URL.createObjectURL(blob);

      setUploadedPreviewUrl(url);

    } catch (err) {

      setUploadedPreviewError(mensajeErrorDocumento(err));

    } finally {

      setIsLoadingUploadedPreview(false);

    }

  };



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



  const handleClearSelection = () => {

    setSelectedFile(null);

    setFileError("");

    setUploadProgress(0);

  };



  const handleCancelReplace = () => {

    setShowReplaceForm(false);

    handleClearSelection();

    setSubmitError("");

  };



  const handleStartReplace = () => {

    setShowReplaceForm(true);

    setShowUploadedPreview(false);

    handleClearSelection();

    setSubmitError("");

  };



  const handleSubmit = async () => {

    if (!selectedFile) {

      setFileError("Selecciona un archivo PDF antes de subir.");

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

      setShowReplaceForm(false);

      setUploadProgress(100);

      resetUploadedPreviewCache();

      setSuccessMessage(

        data.puedeCambiarDocumento && data.estado === "APROBADO"

          ? "Documento actualizado. Tu identidad sigue verificada."

          : "Documento recibido. Tu identidad quedó verificada."

      );

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

      setSubmitError(mensajeErrorDocumento(err));

    } finally {

      setIsDownloading(false);

    }

  };



  const handleOpenUploadedPreview = async () => {

    setShowUploadedPreview(true);

    if (uploadedPreviewUrl) {

      return;

    }

    await loadUploadedPreview();

  };



  const handleCloseUploadedPreview = () => {

    setShowUploadedPreview(false);

  };



  const puedeSubir = estado?.puedeSubir ?? false;
  const puedeCambiarDocumento =
    estado?.puedeCambiarDocumento ?? estado?.estado === "APROBADO";
  const documentoAccesible = estado?.documentoAccesible ?? false;

  const isLoading = loadStatus === "loading";

  const canConfirmUpload = Boolean(selectedFile && pendingPreviewUrl && !fileError && !isSubmitting);

  const showUploadForm = puedeSubir || (showReplaceForm && puedeCambiarDocumento);

  const isReplacing = showReplaceForm && puedeCambiarDocumento && !puedeSubir;



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



      <DocumentPreviewModal

        isOpen={showUploadedPreview}

        title="Documento subido"

        fileName="documento-verificacion.pdf"

        previewUrl={uploadedPreviewUrl}

        isLoading={isLoadingUploadedPreview}

        error={uploadedPreviewError}

        onClose={handleCloseUploadedPreview}

        onDownload={() => void handleDownload()}

        downloadLabel={isDownloading ? "Descargando..." : "Descargar PDF"}

        onRetry={

          uploadedPreviewError

            ? () => {

                if (puedeCambiarDocumento) {

                  handleCloseUploadedPreview();

                  handleStartReplace();

                  return;

                }

                void loadUploadedPreview();

              }

            : undefined

        }

        retryLabel={puedeCambiarDocumento ? "Cambiar documento" : "Reintentar"}

      />



      <PaseadorNavbar />



      <section className={styles.hero}>

        <div>

          <p className={styles.eyebrow}>Verificacion de identidad</p>

          <h1 className={styles.title}>Confirma tu identidad con un documento</h1>

          <p className={styles.description}>

            Sube un PDF con tu cedula de identidad (maximo 5 MB). Revisa la vista previa antes de

            confirmar la subida.

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

            Si no has iniciado sesión, ve a <Link to="/login/paseador">iniciar sesión</Link>.

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

            <div className={styles.metaItem}>

              <span className={styles.metaLabel}>Enviado</span>

              <strong>{formatFecha(estado.enviadoEn)}</strong>

            </div>

            <div className={styles.metaItem}>

              <span className={styles.metaLabel}>Revisado</span>

              <strong>{formatFecha(estado.revisadoEn)}</strong>

            </div>

            <div className={styles.metaItem}>

              <span className={styles.metaLabel}>Documento</span>

              <strong

                className={

                  estado.tieneDocumento

                    ? documentoAccesible

                      ? styles.metaValueOk

                      : styles.metaValueWarn

                    : styles.metaValueMuted

                }

              >

                {estado.tieneDocumento

                  ? documentoAccesible

                    ? "Disponible"

                    : "No disponible"

                  : "Sin subir"}

              </strong>

            </div>

          </div>



          {estado.estado === "APROBADO" && estado.tieneDocumento && !documentoAccesible ? (

            <div className={styles.warningBox} role="alert">

              <strong>Archivo no encontrado</strong>

              <p>

                Tu verificacion sigue activa, pero el PDF guardado ya no esta disponible. Sube uno

                nuevo para poder verlo o descargarlo.

              </p>

            </div>

          ) : null}



          {estado.estado === "RECHAZADO" && estado.motivoRechazo ? (

            <div className={styles.rechazoBox} role="alert">

              <strong>Motivo de rechazo</strong>

              <p>{estado.motivoRechazo}</p>

            </div>

          ) : null}



          {estado.tieneDocumento && documentoAccesible ? (

            <div className={styles.documentCard}>

              <div className={styles.documentCardIcon} aria-hidden="true">

                PDF

              </div>

              <div className={styles.documentCardBody}>

                <strong>Documento verificado</strong>

                <span>documento-verificacion.pdf</span>

              </div>

              <div className={styles.actionsRow}>

                <button

                  type="button"

                  className={styles.primaryButton}

                  onClick={() => void handleOpenUploadedPreview()}

                >

                  Ver documento

                </button>

                <button

                  type="button"

                  className={styles.secondaryButton}

                  onClick={() => void handleDownload()}

                  disabled={isDownloading}

                >

                  {isDownloading ? "Descargando..." : "Descargar PDF"}

                </button>

                {puedeCambiarDocumento ? (

                  <button

                    type="button"

                    className={styles.ghostButton}

                    onClick={handleStartReplace}

                    disabled={showReplaceForm}

                  >

                    Cambiar documento

                  </button>

                ) : null}

              </div>

            </div>

          ) : null}



          {estado.tieneDocumento && !documentoAccesible && puedeCambiarDocumento ? (

            <div className={styles.actionsRow}>

              <button type="button" className={styles.primaryButton} onClick={handleStartReplace}>

                Subir documento nuevamente

              </button>

            </div>

          ) : null}



          {estado.estado === "APROBADO" && !showUploadForm ? (

            <p className={styles.verifiedNote}>

              Tu identidad ya esta verificada. Puedes cambiar el PDF cuando lo necesites.

            </p>

          ) : null}



          {showUploadForm ? (

            <>

              <div className={styles.sectionHeader}>

                <div>

                  <p className={styles.cardEyebrow}>Documento</p>

                  <h2>{isReplacing ? "Reemplazar documento" : "Sube tu cedula en PDF"}</h2>

                </div>

                {isReplacing ? (

                  <button type="button" className={styles.linkButton} onClick={handleCancelReplace}>

                    Cancelar

                  </button>

                ) : null}

              </div>



              {isReplacing ? (

                <p className={styles.replaceHint}>

                  El nuevo PDF reemplazara al anterior y mantendras tu estado verificado.

                </p>

              ) : null}



              <FileUploader

                accept="application/pdf,.pdf"

                disabled={isSubmitting}

                error={fileError}

                helperText="Arrastra tu PDF aquí o haz clic para seleccionarlo. Máximo 5 MB."

                selectedFile={selectedFile}

                title="Seleccionar PDF de verificación"

                onFileSelect={handleFileChange}

              />



              {pendingPreviewUrl && selectedFile ? (

                <div className={styles.previewPanel}>

                  <div className={styles.previewHeader}>

                    <strong>Vista previa del documento</strong>

                    <span>{selectedFile.name}</span>

                  </div>

                  <iframe

                    className={styles.previewFrame}

                    src={pendingPreviewUrl}

                    title={`Vista previa de ${selectedFile.name}`}

                  />

                </div>

              ) : null}



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

                {selectedFile ? (

                  <button

                    type="button"

                    className={styles.secondaryButton}

                    onClick={handleClearSelection}

                    disabled={isSubmitting}

                  >

                    Cambiar archivo

                  </button>

                ) : null}

                <button

                  type="button"

                  className={styles.primaryButton}

                  onClick={() => void handleSubmit()}

                  disabled={!canConfirmUpload}

                >

                  {isSubmitting

                    ? "Subiendo..."

                    : isReplacing

                      ? "Confirmar cambio"

                      : "Aceptar y subir"}

                </button>

              </div>

            </>

          ) : null}

        </section>

      ) : null}

    </main>

  );

}

