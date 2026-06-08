import type { BilleteraBucket } from "../../services/billeteraPaseadorService";
import styles from "./AvailableBalance.module.css";

type AvailableBalanceProps = {
  bucket: BilleteraBucket;
  onWithdraw: () => void;
  isSubmitting?: boolean;
};

function formatMoney(value: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

export default function AvailableBalance({
  bucket,
  onWithdraw,
  isSubmitting = false
}: AvailableBalanceProps) {
  const canWithdraw = bucket.amount > 0;

  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Retiro</p>
          <h2>Saldo disponible</h2>
        </div>
        <div className={styles.amountBlock}>
          <strong>{formatMoney(bucket.amount)}</strong>
        </div>
      </div>

      <p className={styles.hint}>
        {canWithdraw
          ? "Puedes transferir este monto a tu cuenta bancaria registrada."
          : "Cuando tengas saldo liberado, podrás solicitar un retiro."}
      </p>

      <div className={styles.footer}>
        <button
          type="button"
          className={styles.withdrawButton}
          disabled={!canWithdraw || isSubmitting}
          onClick={onWithdraw}
        >
          {isSubmitting ? "Procesando…" : "Retirar fondos"}
        </button>
      </div>
    </section>
  );
}
