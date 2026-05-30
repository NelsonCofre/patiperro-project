import { useEffect, useMemo, useRef, useState } from "react";
import type {
  CatalogoRegistroCuenta,
  PaseadorBankAccount,
  RegistroCuentaBancariaBody
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
  ) => Promise<void>;
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
  bankAccountsLoadError = "",
  catalogoRegistroCuenta,
  catalogoRegistroLoadError = "",
  isSubmitting,
  submitError,
  onClose,
  onSubmit
}: WithdrawalRequestProps) {
  const [selectedAccountId, setSelectedAccountId] = useState(bankAccounts[0]?.id ?? "");
  const [amountInput, setAmountInput] = useState("");
  const [localError, setLocalError] = useState("");
  const [bancoId, setBancoId] = useState("");
  const [tipoCuentaId, setTipoCuentaId] = useState("");
  const [numeroCuenta, setNumeroCuenta] = useState("");

  const needsBankRegistration = bankAccounts.length === 0;
  const catalogoListo =
    catalogoRegistroCuenta != null &&
    catalogoRegistroCuenta.bancos.length > 0 &&
    catalogoRegistroCuenta.tiposCuenta.length > 0;

  const prevIsOpenRef = useRef(false);
  const catalogDefaultsAppliedRef = useRef(false);

  useEffect(() => {
    if (!isOpen) {
      prevIsOpenRef.current = false;
      catalogDefaultsAppliedRef.current = false;
      return;
    }

    if (!prevIsOpenRef.current) {
      prevIsOpenRef.current = true;
      setSelectedAccountId(bankAccounts[0]?.id ?? "");
      setAmountInput(availableAmount > 0 ? String(availableAmount) : "");
      setLocalError("");
      setNumeroCuenta("");
      setBancoId("");
      setTipoCuentaId("");
      catalogDefaultsAppliedRef.current = false;
    }

    if (
      bankAccounts.length === 0 &&
      catalogoListo &&
      catalogoRegistroCuenta &&
      !catalogDefaultsAppliedRef.current
    ) {
      const fb = catalogoRegistroCuenta.bancos[0];
      const ft = catalogoRegistroCuenta.tiposCuenta[0];
      setBancoId(fb ? String(fb.id) : "");
      setTipoCuentaId(ft ? String(ft.id) : "");
      catalogDefaultsAppliedRef.current = true;
    }

    if (bankAccounts.length > 0) {
      setSelectedAccountId((current) => {
        const valid = bankAccounts.some((a) => a.id === current);
        return valid && current ? current : bankAccounts[0]!.id;
      });
    }
  }, [isOpen, bankAccounts, catalogoRegistroCuenta, availableAmount, catalogoListo]);

  const selectedAccount = useMemo(
    () => bankAccounts.find((account) => account.id === selectedAccountId) ?? null,
    [bankAccounts, selectedAccountId]
  );

  if (!isOpen) return null;

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const amount = Number(amountInput);

    if (!Number.isFinite(amount) || amount <= 0) {
      setLocalError("Ingrese un monto valido para continuar.");
      return;
    }

    if (amount < minWithdrawalAmount) {
      setLocalError(`El monto minimo de retiro es ${formatMoney(minWithdrawalAmount)}.`);
      return;
    }

    if (amount > availableAmount) {
      setLocalError("Saldo insuficiente para solicitar ese retiro.");
      return;
    }

    if (needsBankRegistration) {
      const numero = numeroCuenta.replace(/\s+/g, "").trim();
      const bid = Number(bancoId);
      const tid = Number(tipoCuentaId);
      if (!catalogoListo) {
        setLocalError("No hay catalogo de banco y tipo de cuenta disponible. Revisa la conexión o los datos en base.");
        return;
      }
      if (!Number.isFinite(bid) || bid <= 0 || !Number.isFinite(tid) || tid <= 0) {
        setLocalError("Selecciona banco y tipo de cuenta.");
        return;
      }
      if (numero.length < 4 || numero.length > 60) {
        setLocalError("El numero de cuenta debe tener entre 4 y 60 caracteres (sin espacios).");
        return;
      }
      setLocalError("");
      try {
        await onSubmit(amount, "", { bancoId: bid, tipoCuentaId: tid, numeroCuenta: numero });
        onClose();
      } catch {
        // submitError en el contenedor
      }
      return;
    }

    if (!selectedAccountId) {
      setLocalError("Seleccione una cuenta bancaria de destino.");
      return;
    }

    setLocalError("");
    try {
      await onSubmit(amount, selectedAccountId);
      onClose();
    } catch {
      // submitError en el contenedor
    }
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
                ? "Elegí banco y tipo de cuenta desde el listado del sistema, ingresá el número y confirmá el monto."
                : "Indica el monto y la cuenta bancaria registrada donde deseas recibir la transferencia."}
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
                  <strong>No se pudo cargar el catalogo de bancos</strong>
                  <p>{catalogoRegistroLoadError}</p>
                  <p className={styles.fetchWarningHint}>
                    Revisa la petición{" "}
                    <code className={styles.inlineCode}>GET .../catalogo/registro-cuenta</code> en la pestaña Red.
                  </p>
                </div>
              ) : null}
              <p className={styles.bankRegisterIntro}>
                Aun no tenés una cuenta asociada a tu billetera. Elegí banco y tipo desde las tablas del sistema; se
                guardarán al confirmar el retiro.
              </p>
              <label className={styles.field}>
                <span>Banco</span>
                <select
                  value={bancoId}
                  onChange={(event) => setBancoId(event.target.value)}
                  disabled={!catalogoListo}
                  required
                >
                  <option value="">{catalogoListo ? "Selecciona un banco" : "Sin catalogo"}</option>
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
                  value={tipoCuentaId}
                  onChange={(event) => setTipoCuentaId(event.target.value)}
                  disabled={!catalogoListo}
                  required
                >
                  <option value="">{catalogoListo ? "Selecciona el tipo" : "Sin catalogo"}</option>
                  {(catalogoRegistroCuenta?.tiposCuenta ?? []).map((t) => (
                    <option key={t.id} value={String(t.id)}>
                      {t.nombre}
                    </option>
                  ))}
                </select>
              </label>
              <label className={styles.field}>
                <span>Numero de cuenta</span>
                <input
                  type="text"
                  inputMode="numeric"
                  autoComplete="off"
                  value={numeroCuenta}
                  onChange={(event) => setNumeroCuenta(event.target.value)}
                  placeholder="Podés pegar con espacios; se normalizan al guardar"
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
                  value={selectedAccountId}
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
              La solicitud se valida con tu sesion activa y el saldo se descontara de inmediato al confirmar.
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
      </section>
    </div>
  );
}
