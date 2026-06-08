import { useEffect } from "react";

import styles from "./DocumentPreviewModal.module.css";



type DocumentPreviewModalProps = {

  isOpen: boolean;

  title: string;

  fileName?: string;

  previewUrl: string | null;

  isLoading?: boolean;

  error?: string;

  onClose: () => void;

  onDownload?: () => void;

  downloadLabel?: string;

  onRetry?: () => void;

  retryLabel?: string;

};



export default function DocumentPreviewModal({

  isOpen,

  title,

  fileName,

  previewUrl,

  isLoading = false,

  error = "",

  onClose,

  onDownload,

  downloadLabel = "Descargar",

  onRetry,

  retryLabel = "Reintentar"

}: DocumentPreviewModalProps) {

  useEffect(() => {

    if (!isOpen) return;



    const handleKeyDown = (event: KeyboardEvent) => {

      if (event.key === "Escape") {

        onClose();

      }

    };



    const previousOverflow = document.body.style.overflow;

    document.body.style.overflow = "hidden";

    window.addEventListener("keydown", handleKeyDown);



    return () => {

      document.body.style.overflow = previousOverflow;

      window.removeEventListener("keydown", handleKeyDown);

    };

  }, [isOpen, onClose]);



  if (!isOpen) {

    return null;

  }



  return (

    <div

      className={styles.overlay}

      role="presentation"

      onClick={(event) => {

        if (event.target === event.currentTarget) {

          onClose();

        }

      }}

    >

      <div className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="document-preview-title">

        <div className={styles.header}>

          <div className={styles.headerText}>

            <h2 id="document-preview-title" className={styles.title}>

              {title}

            </h2>

            {fileName ? <span className={styles.fileName}>{fileName}</span> : null}

          </div>

          <div className={styles.headerActions}>

            {onDownload && !error ? (

              <button type="button" className={styles.secondaryButton} onClick={onDownload}>

                {downloadLabel}

              </button>

            ) : null}

            <button type="button" className={styles.closeButton} onClick={onClose}>

              Cerrar

            </button>

          </div>

        </div>



        <div className={styles.body}>

          {isLoading ? (

            <div className={styles.statePanel}>

              <span className={styles.spinner} aria-hidden="true" />

              <p className={styles.loading}>Cargando vista previa...</p>

            </div>

          ) : error ? (

            <div className={styles.statePanel} role="alert">

              <span className={styles.errorIcon} aria-hidden="true">

                !

              </span>

              <strong className={styles.errorTitle}>No se pudo mostrar el documento</strong>

              <p className={styles.error}>{error}</p>

              {onRetry ? (

                <button type="button" className={styles.retryButton} onClick={onRetry}>

                  {retryLabel}

                </button>

              ) : null}

            </div>

          ) : previewUrl ? (

            <iframe

              className={styles.frame}

              src={previewUrl}

              title={fileName ?? title}

            />

          ) : (

            <div className={styles.statePanel}>

              <p className={styles.loading}>No hay vista previa disponible.</p>

            </div>

          )}

        </div>

      </div>

    </div>

  );

}

