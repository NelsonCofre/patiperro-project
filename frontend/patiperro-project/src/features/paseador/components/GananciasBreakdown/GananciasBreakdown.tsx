import type { BilleteraBucket } from "../../services/billeteraPaseadorService";
import styles from "./GananciasBreakdown.module.css";

type Props = {
  bucket: BilleteraBucket;
};

function formatMoney(value: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

export default function GananciasBreakdown({ bucket }: Props) {
  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Calculadora de ganancias</p>
          <h2>Asi se descuenta tu comision antes del abono</h2>
          <p className={styles.description}>
            Este detalle muestra con transparencia cuanto paga el tutor, cuanto retiene la plataforma y cual es el monto neto que se acredita a tu billetera.
          </p>
        </div>
      </div>

      <div className={styles.grid}>
        <article className={styles.card}>
          <span>Monto Total</span>
          <strong>{formatMoney(bucket.grossAmount)}</strong>
        </article>
        <article className={styles.card}>
          <span>Comision Patiperro</span>
          <strong>- {formatMoney(bucket.commissionAmount)}</strong>
        </article>
        <article className={`${styles.card} ${styles.resultCard}`}>
          <span>Su Pago</span>
          <strong>{formatMoney(bucket.amount)}</strong>
        </article>
      </div>

      <div className={styles.equation}>
        <span>{formatMoney(bucket.grossAmount)}</span>
        <em>-</em>
        <span>{formatMoney(bucket.commissionAmount)}</span>
        <em>=</em>
        <strong>{formatMoney(bucket.amount)}</strong>
      </div>
    </section>
  );
}
