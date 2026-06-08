import { useMemo, useState } from "react";
import {
  buildMinWithdrawalErrorMessage,
  buildWithdrawalSuccessMessage,
  type CatalogoRegistroCuenta,
  type PaseadorBankAccount,
  type RegistroCuentaBancariaBody
} from "../../hooks/usePaseadorBilletera";
import styles from "./WithdrawalRequest.module.css";

type WithdrawalRequestProps = {
  isOpen: boolean;
  availableAmount: number;
  minWithdrawalAmount: number;
  bankAccounts: PaseadorBankAccount[];
  /** Error al cargar GET cuentas (p. ej. CORS); distinto de lista vacía real. */
  bankAccountsLoadError?: string;
  /** Catálogo banco + tipo_cuenta (null si falló la carga). */
  catalogoRegistroCuenta: CatalogoRegistroCuenta | null;
  catalogoRegistroLoadError?: string;
  isSubmitting: boolean;
  submitError: string;
  onClose: () => void;
  onSubmit: (
    amount: number,
    bankAccountId: string,
    registro?: RegistroCuentaBancariaBody
  ) => Promise<{ mensaje?: string } | void>;
};

function formatMoney(value: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

export default function WithdrawalRequest(props: WithdrawalRequestProps) {
  const { isOpen } = props;
  const [openSession, setOpenSession] = useState(0);
  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);

  if (isOpen !== prevIsOpen) {
    setPrevIsOpen(isOpen);
    if (isOpen) {
      setOpenSession((session) => session + 1);
    }
  }

  if (!isOpen) return null;

  return <WithdrawalRequestForm key={openSession} {...props} />;
}

function WithdrawalRequestForm({
  availableAmount,
  minWithdrawalAmount,
  bankAccounts,
  bankAccountsLoadError = "",
  catalogoRegistroCuenta,
  catalogoRegistroLoadError = "",
  isSubmitting,
  submitError,
  onClose,
  onSubmit
}: WithdrawalRequestProps) {
  const defaultAccountId = bankAccounts[0]?.id ?? "";
  const catalogoListo =
    catalogoRegistroCuenta != null &&
    catalogoRegistroCuenta.bancos.length > 0 &&
    catalogoRegistroCuenta.tiposCuenta.length > 0;
  const defaultBancoId = catalogoListo ? String(catalogoRegistroCuenta!.bancos[0]?.id ?? "") : "";
  const defaultTipoCuentaId = catalogoListo ? String(catalogoRegistroCuenta!.tiposCuenta[0]?.id ?? "") : "";

  const [selectedAccountId, setSelectedAccountId] = useState(defaultAccountId);
  const [amountInput, setAmountInput] = useState(availableAmount > 0 ? String(availableAmount) : "");
  const [localError, setLocalError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [bancoId, setBancoId] = useState(defaultBancoId);
  const [tipoCuentaId, setTipoCuentaId] = useState(defaultTipoCuentaId);
  const [numeroCuenta, setNumeroCuenta] = useState("");

  const needsBankRegistration = bankAccounts.length === 0;

  const effectiveSelectedAccountId = useMemo(() => {
    if (bankAccounts.length === 0) return "";
    const valid = bankAccounts.some((account) => account.id === selectedAccountId);
    return valid && selectedAccountId ? selectedAccountId : bankAccounts[0]!.id;
  }, [bankAccounts, selectedAccountId]);

  const effectiveBancoId = bancoId || defaultBancoId;
  const effectiveTipoCuentaId = tipoCuentaId || defaultTipoCuentaId;

  const selectedAccount = useMemo(
    () => bankAccounts.find((account) => account.id === effectiveSelectedAccountId) ?? null,
    [bankAccounts, effectiveSelectedAccountId]
  );

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const amount = Number(amountInput);

    if (!Number.isFinite(amount) || amount <= 0) {
      setLocalError("Ingresa un monto válido para continuar.");
      return;
    }

    if (amount < minWithdrawalAmount) {
      setLocalError(buildMinWithdrawalErrorMessage(amount, minWithdrawalAmount));
      return;
    }

    if (amount > availableAmount) {
      setLocalError("Saldo insuficiente para solicitar ese retiro.");
      return;
    }

    if (needsBankRegistration) {
      const numero = numeroCuenta.replace(/\s+/g, "").trim();
      const bid = Number(effectiveBancoId);
      const tid = Number(effectiveTipoCuentaId);
      if (!catalogoListo) {
        setLocalError("No hay catálogo de banco y tipo de cuenta disponible. Revisa la conexión o los datos en base.");
        return;
      }
      if (!Number.isFinite(bid) || bid <= 0 || !Number.isFinite(tid) || tid <= 0) {
        setLocalError("Selecciona banco y tipo de cuenta.");
        return;
      }
      if (numero.length < 4 || numero.length > 60) {
        setLocalError("El número de cuenta debe tener entre 4 y 60 caracteres (sin espacios).");
        return;
      }
      setLocalError("");
      try {
        const result = await onSubmit(amount, "", { bancoId: bid, tipoCuentaId: tid, numeroCuenta: numero });
        setSuccessMessage(result?.mensaje ?? buildWithdrawalSuccessMessage(amount));
      } catch {
        // submitError en el contenedor
      }
      return;
    }

    if (!effectiveSelectedAccountId) {
      setLocalError("Selecciona una cuenta bancaria de destino.");
      return;
    }

    setLocalError("");
    try {
      const result = await onSubmit(amount, effectiveSelectedAccountId);
      setSuccessMessage(result?.mensaje ?? buildWithdrawalSuccessMessage(amount));
    } catch {
      // submitError en el contenedor
    }
  }

  function handleCloseAfterSuccess() {
    setSuccessMessage("");
    onClose();
  }

  const registroBloqueado = needsBankRegistration && (!catalogoListo || Boolean(catalogoRegistroLoadError));

  return (
    <div className={styles.overlay}>
      <section className={styles.modal} role="dialog" aria-modal="true">
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>Retiro de ganancias</p>
            <h2>Solicitar retiro</h2>
            <p className={styles.description}>
              {needsBankRegistration
                ? "Selecciona el banco y tipo de cuenta desde el listado del sistema, ingresa el número y confirma el monto."
                : "Indica el monto y la cuenta bancaria registrada donde deseas recibir la transferencia."}
            </p>
          </div>
          <button type="button" className={styles.closeButton} onClick={successMessage ? handleCloseAfterSuccess : onClose}>
            Cerrar
          </button>
        </div>

        {successMessage ? (
          <div className={styles.successPanel} role="status">
            <p className={styles.successEyebrow}>Retiro registrado</p>
            <h3 className={styles.successTitle}>Tu solicitud fue enviada</h3>
            <p className={styles.successMessage}>{successMessage}</p>
            <p className={styles.successHint}>
              El monto ya se descontó de tu saldo disponible. Puedes seguir el estado en la sección Retiros.
            </p>
            <button type="button" className={styles.submitButton} onClick={handleCloseAfterSuccess}>
              Entendido
            </button>
          </div>
        ) : (
        <form className={styles.form} onSubmit={handleSubmit} noValidate>
          <div className={styles.summaryGrid}>
            <article className={styles.summaryCard}>
              <span>Saldo disponible</span>
              <strong>{formatMoney(availableAmount)}</strong>
            </article>
            <article className={styles.summaryCard}>
              <span>Mínimo de retiro</span>
              <strong>{formatMoney(minWithdrawalAmount)}</strong>
            </article>
          </div>

          <label className={styles.field}>
            <span>Monto a retirar</span>
            <input
              type="number"
              inputMode="numeric"
              min={1}
              max={availableAmount > 0 ? availableAmount : undefined}
              step="1"
              value={amountInput}
              onChange={(event) => {
                setAmountInput(event.target.value);
                const next = Number(event.target.value);
                if (Number.isFinite(next) && next > 0 && next < minWithdrawalAmount) {
                  setLocalError(buildMinWithdrawalErrorMessage(next, minWithdrawalAmount));
                  return;
                }
                if (localError) {
                  setLocalError("");
                }
              }}
              placeholder="Ej. 15000"
            />
          </label>

          {needsBankRegistration ? (
            <div className={styles.bankRegisterBlock}>
              {bankAccountsLoadError ? (
                <div className={styles.fetchWarning} role="alert">
                  <strong>No se pudo cargar la lista de cuentas</strong>
                  <p>{bankAccountsLoadError}</p>
                  <p className={styles.fetchWarningHint}>
                    Si ya registraste una cuenta, puede existir en el servidor pero no mostrarse por un fallo de red,
                    CORS o sesión. Revisa la petición{" "}
                    <code className={styles.inlineCode}>GET .../cuentas-bancarias</code> en las herramientas de red.
                  </p>
                </div>
              ) : null}
              {catalogoRegistroLoadError ? (
                <div className={styles.fetchWarning} role="alert">
                  <strong>No se pudo cargar el catálogo de bancos</strong>
                  <p>{catalogoRegistroLoadError}</p>
                  <p className={styles.fetchWarningHint}>
                    Revisa la petición{" "}
                    <code className={styles.inlineCode}>GET .../catalogo/registro-cuenta</code> en la pestaña Red.
                  </p>
                </div>
              ) : null}
              <p className={styles.bankRegisterIntro}>
                Aún no tienes una cuenta asociada a tu billetera. Selecciona banco y tipo desde las tablas del sistema; se
                guardarán al confirmar el retiro.
              </p>
              <label className={styles.field}>
                <span>Banco</span>
                <select
                  value={effectiveBancoId}
                  onChange={(event) => setBancoId(event.target.value)}
                  disabled={!catalogoListo}
                  required
                >
                  <option value="">{catalogoListo ? "Selecciona un banco" : "Sin catálogo"}</option>
                  {(catalogoRegistroCuenta?.bancos ?? []).map((b) => (
                    <option key={b.id} value={String(b.id)}>
                      {b.nombre}
                    </option>
                  ))}
                </select>
              </label>
              <label className={styles.field}>
                <span>Tipo de cuenta</span>
                <select
                  value={effectiveTipoCuentaId}
                  onChange={(event) => setTipoCuentaId(event.target.value)}
                  disabled={!catalogoListo}
                  required
                >
                  <option value="">{catalogoListo ? "Selecciona el tipo" : "Sin catálogo"}</option>
                  {(catalogoRegistroCuenta?.tiposCuenta ?? []).map((t) => (
                    <option key={t.id} value={String(t.id)}>
                      {t.nombre}
                    </option>
                  ))}
                </select>
              </label>
              <label className={styles.field}>
                <span>Número de cuenta</span>
                <input
                  type="text"
                  inputMode="numeric"
                  autoComplete="off"
                  value={numeroCuenta}
                  onChange={(event) => setNumeroCuenta(event.target.value)}
                  placeholder="Puedes pegar con espacios; se normalizan al guardar"
                  maxLength={80}
                  disabled={!catalogoListo}
                />
              </label>
            </div>
          ) : (
            <>
              <label className={styles.field}>
                <span>Cuenta bancaria de destino</span>
                <select
                  value={effectiveSelectedAccountId}
                  onChange={(event) => setSelectedAccountId(event.target.value)}
                >
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
            </>
          )}

          {localError || submitError ? (
            <div className={styles.errorBox} role="alert">
              {localError || submitError}
            </div>
          ) : null}

          <div className={styles.footer}>
            <p className={styles.helper}>
              Al confirmar, el monto se descuenta de tu saldo disponible.
            </p>
            <button
              type="submit"
              className={styles.submitButton}
              disabled={isSubmitting || (needsBankRegistration && registroBloqueado)}
            >
              {isSubmitting
                ? "Procesando solicitud..."
                : needsBankRegistration
                  ? "Guardar cuenta y solicitar retiro"
                  : "Confirmar retiro"}
            </button>
          </div>
        </form>
        )}
      </section>
    </div>
  );
}
