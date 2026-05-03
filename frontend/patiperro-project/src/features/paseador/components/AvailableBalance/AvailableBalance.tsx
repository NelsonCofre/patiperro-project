import type { BilleteraBucket } from "../../services/billeteraPaseadorService";
import styles from "./AvailableBalance.module.css";

type AvailableBalanceProps = {
  bucket: BilleteraBucket;
  onWithdraw: () => void;
};

function formatMoney(value: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

export default function AvailableBalance({ bucket, onWithdraw }: AvailableBalanceProps) {
  const canWithdraw = bucket.amount > 0;
  const disabledMessage =
    "Necesitas saldo disponible mayor a $0 para solicitar un retiro.";

  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Saldo liquido</p>
          <h2>Saldo Disponible</h2>
          <p className={styles.description}>
            Este monto representa fondos netos que ya cumplieron el ciclo N+2 y no tienen disputas activas.
          </p>
        </div>

        <div className={styles.amountBlock}>
          <span>Monto neto disponible</span>
          <strong>{formatMoney(bucket.amount)}</strong>
        </div>
      </div>

      <div className={styles.metaGrid}>
        <article className={styles.metaCard}>
          <span>Bruto procesado</span>
          <strong>{formatMoney(bucket.grossAmount)}</strong>
        </article>
        <article className={styles.metaCard}>
          <span>Comision Patiperro</span>
          <strong>{formatMoney(bucket.commissionAmount)}</strong>
        </article>
        <article className={styles.metaCard}>
          <span>Reservas liberadas</span>
          <strong>{bucket.reservas.length}</strong>
        </article>
      </div>

      <div className={styles.commissionNote}>
        <strong>Comision de la plataforma: 5%</strong>
        <p>
          El saldo disponible ya descuenta automaticamente este porcentaje, por lo que el monto visible corresponde a lo que recibirias efectivamente.
        </p>
      </div>

      <div className={styles.footer}>
        <p className={styles.helper}>
          El valor mostrado ya corresponde al monto neto que recibirias en tu cuenta bancaria.
        </p>

        <div className={styles.actionBlock} title={canWithdraw ? "" : disabledMessage}>
          <button
            type="button"
            className={styles.withdrawButton}
            disabled={!canWithdraw}
            onClick={onWithdraw}
          >
            Retirar fondos
          </button>
          {!canWithdraw ? <span className={styles.tooltip}>{disabledMessage}</span> : null}
        </div>
      </div>
    </section>
  );
}
