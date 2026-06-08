import CodigoEncuentro from "./CodigoEncuentro";
import styles from "./CodigoEncuentroModal.module.css";

type Props = {
  codigo: number | string | null;
  onClose: () => void;
};

export default function CodigoEncuentroModal({ codigo, onClose }: Props) {
  return (
    <div className={styles.overlay} onClick={onClose} role="presentation">
      <div
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-label="Codigo de encuentro"
        onClick={(event) => event.stopPropagation()}
      >
        <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Cerrar">
          ×
        </button>
        <CodigoEncuentro codigo={codigo} embedded />
      </div>
    </div>
  );
}
