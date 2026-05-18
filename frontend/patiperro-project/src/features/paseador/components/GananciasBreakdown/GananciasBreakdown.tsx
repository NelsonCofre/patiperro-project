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
  const grossAmount =
    bucket.grossAmount > 0
      ? bucket.grossAmount
      : bucket.reservas.reduce((sum, item) => sum + item.montoBruto, 0);
  const commissionAmount =
    bucket.commissionAmount > 0
      ? bucket.commissionAmount
      : bucket.reservas.reduce((sum, item) => sum + item.comision, 0);
  const netFromReservas = bucket.reservas.reduce((sum, item) => sum + item.montoNeto, 0);
  const netAmount =
    bucket.key === "disponible" && netFromReservas > 0 ? netFromReservas : bucket.amount || netFromReservas;

  if (bucket.reservas.length === 0 && grossAmount === 0 && netAmount === 0) {
    return null;
  }

  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <div>
          <p className={styles.eyebrow}>Desglose de comisión</p>
          <h2>Asi se descuenta tu comision antes del abono</h2>
          <p className={styles.description}>
            Este detalle muestra con transparencia cuanto paga el tutor, cuanto retiene la plataforma y cual es el
            monto neto que se acredita a tu billetera.
          </p>
        </div>
      </div>

      <div className={styles.grid}>
        <article className={styles.card}>
          <span>Monto Total</span>
          <strong>{formatMoney(grossAmount)}</strong>
        </article>
        <article className={styles.card}>
          <span>Comision Patiperro</span>
          <strong>- {formatMoney(commissionAmount)}</strong>
        </article>
        <article className={`${styles.card} ${styles.resultCard}`}>
          <span>Su Pago</span>
          <strong>{formatMoney(netAmount)}</strong>
        </article>
      </div>

      <div className={styles.equation}>
        <span>{formatMoney(grossAmount)}</span>
        <em>-</em>
        <span>{formatMoney(commissionAmount)}</span>
        <em>=</em>
        <strong>{formatMoney(netAmount)}</strong>
      </div>
    </section>
  );
}
