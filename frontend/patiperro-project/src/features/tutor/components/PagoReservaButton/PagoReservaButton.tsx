import styles from "./PagoReservaButton.module.css";

type Props = {
  preferenceId?: string | null;
  initPoint?: string | null;
  paymentStatus?: string | null;
  isPaid?: boolean;
  amountLabel: string;
  onUnavailable?: () => void;
};

function normalizeStatus(value?: string | null): string {
  return (value ?? "")
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, "_");
}

function isPaidStatus(value?: string | null): boolean {
  const status = normalizeStatus(value);
  return status.includes("pagad");
}

function isPendingPaymentStatus(value?: string | null): boolean {
  const status = normalizeStatus(value);
  return status.includes("pendiente_pago") || status.includes("pending_payment");
}

export default function PagoReservaButton({
  preferenceId,
  initPoint,
  paymentStatus,
  isPaid,
  amountLabel,
  onUnavailable
}: Props) {
  const paid = Boolean(isPaid) || isPaidStatus(paymentStatus);
  const pendingPayment = isPendingPaymentStatus(paymentStatus);

  const handleClick = () => {
    if (initPoint?.trim()) {
      window.location.assign(initPoint.trim());
      return;
    }
    onUnavailable?.();
  };

  if (paid) {
    return (
      <div className={`${styles.panel} ${styles.panelPaid}`}>
        <span className={styles.eyebrow}>Pago confirmado</span>
        <strong>La reserva ya cuenta con respaldo financiero</strong>
        <p>Se registro el pago de {amountLabel} y el dinero permanecera retenido hasta el cierre del paseo.</p>
      </div>
    );
  }

  return (
    <div className={styles.panel}>
      <span className={styles.eyebrow}>
        {pendingPayment ? "Pago en espera" : "Pago seguro"}
      </span>
      <strong>{pendingPayment ? "Completa el checkout de tu reserva" : "Paga tu reserva cuando quieras continuar"}</strong>
      <p>
        Total a pagar: {amountLabel}
        {preferenceId?.trim() ? ` · Preference ${preferenceId.trim()}` : ""}
      </p>
      <button type="button" className={styles.payButton} onClick={handleClick}>
        {pendingPayment ? "Continuar pago" : "Pagar reserva"}
      </button>
    </div>
  );
}
