import { useMemo, useRef, useState } from "react";
import styles from "./FileUploader.module.css";

type FileUploaderProps = {
  accept?: string;
  disabled?: boolean;
  error?: string;
  helperText?: string;
  selectedFile: File | null;
  title?: string;
  onFileSelect: (file: File | null) => void;
};

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(2)} MB`;
}

export default function FileUploader({
  accept,
  disabled = false,
  error = "",
  helperText = "",
  selectedFile,
  title = "Selecciona un archivo",
  onFileSelect
}: FileUploaderProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragActive, setIsDragActive] = useState(false);

  const fileSummary = useMemo(() => {
    if (!selectedFile) return "";
    return `${selectedFile.name} · ${formatFileSize(selectedFile.size)}`;
  }, [selectedFile]);

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null;
    onFileSelect(file);
  };

  const handleOpenPicker = () => {
    if (disabled) return;
    inputRef.current?.click();
  };

  const handleDragOver = (event: React.DragEvent<HTMLButtonElement>) => {
    event.preventDefault();
    if (disabled) return;
    setIsDragActive(true);
  };

  const handleDragLeave = (event: React.DragEvent<HTMLButtonElement>) => {
    event.preventDefault();
    setIsDragActive(false);
  };

  const handleDrop = (event: React.DragEvent<HTMLButtonElement>) => {
    event.preventDefault();
    if (disabled) return;
    setIsDragActive(false);
    const file = event.dataTransfer.files?.[0] ?? null;
    onFileSelect(file);
  };

  const zoneClassName = [
    styles.uploadZone,
    isDragActive ? styles.dragActive : "",
    error ? styles.errorState : ""
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={styles.wrapper}>
      <input
        ref={inputRef}
        className={styles.input}
        type="file"
        accept={accept}
        onChange={handleInputChange}
        disabled={disabled}
      />

      <button
        type="button"
        className={zoneClassName}
        onClick={handleOpenPicker}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        disabled={disabled}
      >
        <span className={styles.uploadIcon} aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <path d="M14 2v6h6M12 18v-7m0 0-3 3m3-3 3 3" />
          </svg>
        </span>
        <span className={styles.uploadTitle}>
          {selectedFile ? "Archivo seleccionado" : title}
        </span>
        <span className={styles.uploadHint}>
          {selectedFile ? `${fileSummary}. Revisa la vista previa abajo.` : helperText}
        </span>
      </button>

      {error ? <p className={styles.errorText}>{error}</p> : null}
    </div>
  );
}
