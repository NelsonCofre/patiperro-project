import styles from "./SaldoRetenidoNotice.module.css";

type Props = {
  compact?: boolean;
  onOpenInfo?: () => void;
  message?: string | null;
};

const DEFAULT_MESSAGE =
  "Su dinero sera retenido por Patiperro y solo se liberara al paseador una vez que el viaje finalice satisfactoriamente.";

export default function SaldoRetenidoNotice({ compact = false, onOpenInfo, message }: Props) {
  const text = message?.trim() || DEFAULT_MESSAGE;

  return (
    <section className={`${styles.notice} ${compact ? styles.noticeCompact : ""}`}>
      <div className={styles.icon} aria-hidden="true">
        $
      </div>
      <div className={styles.copy}>
        <span className={styles.eyebrow}>Saldo retenido</span>
        <strong>Pago protegido hasta el fin del paseo</strong>
        <p>{text}</p>
      </div>
      {onOpenInfo ? (
        <button type="button" className={styles.infoButton} onClick={onOpenInfo}>
          Como funciona
        </button>
      ) : null}
    </section>
  );
}
