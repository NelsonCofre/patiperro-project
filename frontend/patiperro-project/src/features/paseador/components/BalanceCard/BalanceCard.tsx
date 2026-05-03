import type { BilleteraBucket } from "../../services/billeteraPaseadorService";
import styles from "./BalanceCard.module.css";

type BalanceCardProps = {
  bucket: BilleteraBucket;
  isActive?: boolean;
  isPrimary?: boolean;
  onClick?: () => void;
};

function formatMoney(value: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

export default function BalanceCard({
  bucket,
  isActive = false,
  isPrimary = false,
  onClick
}: BalanceCardProps) {
  const variantClass =
    bucket.key === "retenido"
      ? styles.retenidoCard
      : bucket.key === "verificacion"
        ? styles.verificacionCard
        : styles.disponibleCard;

  const className = [
    styles.card,
    variantClass,
    isPrimary ? styles.primaryCard : "",
    isActive ? styles.activeCard : ""
  ]
    .filter(Boolean)
    .join(" ");

  const content = (
    <>
      <div className={styles.header}>
        <span className={styles.title}>{bucket.title}</span>
        <span className={styles.count}>{bucket.reservas.length} reservas</span>
      </div>
      <strong className={styles.amount}>{formatMoney(bucket.amount)}</strong>
      <p className={styles.helper}>{bucket.helper}</p>
    </>
  );

  if (!onClick) {
    return <article className={className}>{content}</article>;
  }

  return (
    <button type="button" className={`${className} ${styles.buttonCard}`} onClick={onClick}>
      {content}
    </button>
  );
}
