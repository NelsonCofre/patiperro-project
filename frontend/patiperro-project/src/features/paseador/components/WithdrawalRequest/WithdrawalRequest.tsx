import { useEffect, useMemo, useState } from "react";
import type { PaseadorBankAccount } from "../../hooks/usePaseadorBilletera";
import styles from "./WithdrawalRequest.module.css";

type WithdrawalRequestProps = {
  isOpen: boolean;
  availableAmount: number;
  minWithdrawalAmount: number;
  bankAccounts: PaseadorBankAccount[];
  isSubmitting: boolean;
  submitError: string;
  onClose: () => void;
  onSubmit: (amount: number, bankAccountId: string) => Promise<void>;
};

function formatMoney(value: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

export default function WithdrawalRequest({
  isOpen,
  availableAmount,
  minWithdrawalAmount,
  bankAccounts,
  isSubmitting,
  submitError,
  onClose,
  onSubmit
}: WithdrawalRequestProps) {
  const [selectedAccountId, setSelectedAccountId] = useState(bankAccounts[0]?.id ?? "");
  const [amountInput, setAmountInput] = useState("");
  const [localError, setLocalError] = useState("");

  useEffect(() => {
    if (!isOpen) return;
    setSelectedAccountId(bankAccounts[0]?.id ?? "");
    setAmountInput(availableAmount > 0 ? String(availableAmount) : "");
    setLocalError("");
  }, [availableAmount, bankAccounts, isOpen]);

  const selectedAccount = useMemo(
    () => bankAccounts.find((account) => account.id === selectedAccountId) ?? null,
    [bankAccounts, selectedAccountId]
  );

  if (!isOpen) return null;

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const amount = Number(amountInput);

    if (bankAccounts.length === 0) {
      setLocalError("Complete sus datos bancarios para poder solicitar el retiro.");
      return;
    }

    if (!selectedAccountId) {
      setLocalError("Seleccione una cuenta bancaria de destino.");
      return;
    }

    if (!Number.isFinite(amount) || amount <= 0) {
      setLocalError("Ingrese un monto valido para continuar.");
      return;
    }

    if (amount < minWithdrawalAmount) {
      setLocalError(
        `El monto minimo de retiro es ${formatMoney(minWithdrawalAmount)}.`
      );
      return;
    }

    if (amount > availableAmount) {
      setLocalError("Saldo insuficiente para solicitar ese retiro.");
      return;
    }

    setLocalError("");
    try {
      await onSubmit(amount, selectedAccountId);
      onClose();
    } catch {
      // El mensaje especifico viene desde submitError en el contenedor.
    }
  }

  return (
    <div className={styles.overlay}>
      <section className={styles.modal} role="dialog" aria-modal="true">
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>Retiro de ganancias</p>
            <h2>Solicitar retiro</h2>
            <p className={styles.description}>
              Indica el monto y la cuenta bancaria registrada donde deseas recibir la transferencia.
            </p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            Cerrar
          </button>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.summaryGrid}>
            <article className={styles.summaryCard}>
              <span>Saldo disponible</span>
              <strong>{formatMoney(availableAmount)}</strong>
            </article>
            <article className={styles.summaryCard}>
              <span>Minimo de retiro</span>
              <strong>{formatMoney(minWithdrawalAmount)}</strong>
            </article>
          </div>

          <label className={styles.field}>
            <span>Monto a retirar</span>
            <input
              type="number"
              inputMode="numeric"
              min={minWithdrawalAmount}
              max={availableAmount}
              step="1"
              value={amountInput}
              onChange={(event) => setAmountInput(event.target.value)}
              placeholder="Ej. 15000"
            />
          </label>

          <label className={styles.field}>
            <span>Cuenta bancaria de destino</span>
            <select
              value={selectedAccountId}
              onChange={(event) => setSelectedAccountId(event.target.value)}
              disabled={bankAccounts.length === 0}
            >
              {bankAccounts.length === 0 ? (
                <option value="">Sin cuentas registradas</option>
              ) : null}
              {bankAccounts.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.bankName} · {account.accountType} · {account.accountNumberMasked}
                </option>
              ))}
            </select>
          </label>

          {selectedAccount ? (
            <article className={styles.accountCard}>
              <strong>{selectedAccount.holderName}</strong>
              <p>
                {selectedAccount.bankName} · {selectedAccount.accountType}
              </p>
              <span>{selectedAccount.accountNumberMasked}</span>
            </article>
          ) : null}

          {localError || submitError ? (
            <div className={styles.errorBox} role="alert">
              {localError || submitError}
            </div>
          ) : null}

          <div className={styles.footer}>
            <p className={styles.helper}>
              La solicitud se valida con tu sesion activa y el saldo se descontara de inmediato al confirmar.
            </p>
            <button type="submit" className={styles.submitButton} disabled={isSubmitting}>
              {isSubmitting ? "Procesando solicitud..." : "Confirmar retiro"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
