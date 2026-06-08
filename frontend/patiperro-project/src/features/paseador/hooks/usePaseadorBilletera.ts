import { useCallback, useEffect, useMemo, useState } from "react";
import {
  fetchBilleteraPaseador,
  fetchCatalogoRegistroCuentaPaseador,
  fetchCuentasBancariasPaseador,
  fetchHistorialRetirosPaseador,
  registrarCuentaBancariaPaseador,
  solicitarRetiroPaseador,
  type RetiroHistorialItem,
  type BilleteraBucket,
  type BilleteraBucketKey,
  type BilleteraPaseadorData,
  type CatalogoRegistroCuenta,
  type CuentaBancariaPaseador,
  type RegistroCuentaBancariaBody
} from "../services/billeteraPaseadorService";

const REFRESH_MS = 15000;
export const MIN_WITHDRAWAL_AMOUNT = 5000;

function formatMoneyClp(value: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

/** Mensaje visible cuando el paseador intenta retirar menos del mínimo permitido. */
export function buildMinWithdrawalErrorMessage(
  attemptedAmount: number,
  minAmount: number = MIN_WITHDRAWAL_AMOUNT
): string {
  return `Ingresaste ${formatMoneyClp(attemptedAmount)}. El monto mínimo para retirar es ${formatMoneyClp(minAmount)}.`;
}

/** Mensaje de confirmación tras registrar un retiro exitoso. */
export function buildWithdrawalSuccessMessage(
  amount: number,
  backendMessage?: string | null
): string {
  const detalle = backendMessage?.trim();
  if (detalle) {
    return `Retiro de ${formatMoneyClp(amount)} registrado. ${detalle}`;
  }
  return `Tu retiro de ${formatMoneyClp(amount)} fue registrado y se procesará en las próximas 48 horas hábiles.`;
}

export type PaseadorBankAccount = CuentaBancariaPaseador;

export type { RegistroCuentaBancariaBody, CatalogoRegistroCuenta };

function createEmptyBucket(
  key: BilleteraBucketKey,
  title: string,
  helper: string
): BilleteraBucket {
  return {
    key,
    title,
    helper,
    amount: 0,
    grossAmount: 0,
    commissionAmount: 0,
    reservaCount: 0,
    reservas: []
  };
}

const EMPTY_DATA: BilleteraPaseadorData = {
  retenido: createEmptyBucket(
    "retenido",
    "Retenido",
    "Paseos pagados aún no finalizados."
  ),
  verificacion: createEmptyBucket(
    "verificacion",
    "En verificación",
    "Paseos finalizados en confirmación."
  ),
  disponible: createEmptyBucket(
    "disponible",
    "Disponible",
    "Listo para retirar."
  ),
  proyeccionLiberacionesPorDia: [],
  updatedAt: ""
};

function formatLastUpdated(value: string): string {
  if (!value) return "Sin sincronización aún";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Sin sincronización aún";
  return new Intl.DateTimeFormat("es-CL", {
    dateStyle: "short",
    timeStyle: "short"
  }).format(date);
}

function getNextReleaseRefreshDelay(): number {
  const now = new Date();
  const next = new Date(now);
  next.setDate(now.getDate() + 1);
  next.setHours(0, 0, 1, 0);
  return Math.max(next.getTime() - now.getTime(), 1000);
}

export function usePaseadorBilletera() {
  const [data, setData] = useState<BilleteraPaseadorData>(EMPTY_DATA);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [withdrawalNotice, setWithdrawalNotice] = useState("");
  const [withdrawalHistory, setWithdrawalHistory] = useState<RetiroHistorialItem[]>([]);
  const [withdrawalHistoryError, setWithdrawalHistoryError] = useState("");
  const [isSubmittingWithdrawal, setIsSubmittingWithdrawal] = useState(false);
  const [withdrawalError, setWithdrawalError] = useState("");
  const [bankAccounts, setBankAccounts] = useState<PaseadorBankAccount[]>([]);
  const [catalogoRegistroCuenta, setCatalogoRegistroCuenta] = useState<CatalogoRegistroCuenta | null>(null);
  const [catalogoRegistroLoadError, setCatalogoRegistroLoadError] = useState("");
  /** Si falla GET cuentas-bancarias (CORS, JWT, etc.); no confundir con "sin cuentas". */
  const [bankAccountsLoadError, setBankAccountsLoadError] = useState("");

  const load = useCallback(async (silent = false) => {
    if (silent) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }
    setError("");
    setBankAccountsLoadError("");
    setCatalogoRegistroLoadError("");
    setWithdrawalHistoryError("");
    try {
      const next = await fetchBilleteraPaseador();
      setData(next);
      try {
        const historial = await fetchHistorialRetirosPaseador();
        setWithdrawalHistory(historial);
      } catch (historialError) {
        setWithdrawalHistory([]);
        setWithdrawalHistoryError(
          historialError instanceof Error
            ? historialError.message
            : "No se pudo cargar el historial de retiros."
        );
      }
      try {
        const cuentas = await fetchCuentasBancariasPaseador();
        setBankAccounts(cuentas);
        setBankAccountsLoadError("");
      } catch (cuentasError) {
        setBankAccounts([]);
        setBankAccountsLoadError(
          cuentasError instanceof Error
            ? cuentasError.message
            : "No se pudieron cargar las cuentas bancarias."
        );
      }
      try {
        const catalogo = await fetchCatalogoRegistroCuentaPaseador();
        setCatalogoRegistroCuenta(catalogo);
        setCatalogoRegistroLoadError("");
      } catch (catalogoError) {
        setCatalogoRegistroCuenta(null);
        setCatalogoRegistroLoadError(
          catalogoError instanceof Error
            ? catalogoError.message
            : "No se pudo cargar el catálogo de bancos."
        );
      }
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : "No se pudo cargar la billetera del paseador."
      );
    } finally {
      if (silent) {
        setIsRefreshing(false);
      } else {
        setIsLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    void load();
    const timer = window.setInterval(() => {
      void load(true);
    }, REFRESH_MS);
    const midnightTimer = window.setTimeout(() => {
      void load(true);
    }, getNextReleaseRefreshDelay());
    return () => {
      window.clearInterval(timer);
      window.clearTimeout(midnightTimer);
    };
  }, [load]);

  const buckets = useMemo(
    () => [data.retenido, data.verificacion, data.disponible],
    [data.disponible, data.retenido, data.verificacion]
  );

  const withdrawAvailableBalance = useCallback(
    async (amount: number, bankAccountId: string, registro?: RegistroCuentaBancariaBody) => {
      setWithdrawalError("");
      setWithdrawalNotice("");
      setIsSubmittingWithdrawal(true);

      try {
        if (amount < MIN_WITHDRAWAL_AMOUNT) {
          throw new Error(buildMinWithdrawalErrorMessage(amount));
        }

        const currentAvailable = data.disponible.amount;
        if (currentAvailable <= 0 || amount > currentAvailable) {
          throw new Error("Saldo insuficiente para completar esta solicitud de retiro.");
        }

        let selectedAccount: CuentaBancariaPaseador | null =
          bankAccounts.find((account) => account.id === bankAccountId) ?? null;
        if (registro) {
          selectedAccount = await registrarCuentaBancariaPaseador(registro);
          setBankAccounts([selectedAccount]);
          setBankAccountsLoadError("");
        }
        if (!selectedAccount) {
          throw new Error("Completa tus datos bancarios antes de solicitar el retiro.");
        }

        const result = await solicitarRetiroPaseador(amount);
        const operationId = `RET-${result.idTransaccion}`;

        setData((current) => ({
          ...current,
          disponible: {
            ...current.disponible,
            amount: Math.max(result.saldoDisponibleTrasRetiro, 0)
          },
          updatedAt: new Date().toISOString()
        }));

        try {
          const historial = await fetchHistorialRetirosPaseador();
          setWithdrawalHistory(historial);
          setWithdrawalHistoryError("");
        } catch {
          const bankLabel = `${selectedAccount.bankName} · ${selectedAccount.accountType} · ${selectedAccount.accountNumberMasked}`;
          setWithdrawalHistory((current) => [
            {
              idRetiroFondos: 0,
              idTransaccion: result.idTransaccion,
              operationId,
              monto: result.montoRetirado || amount,
              estadoPago: "PENDIENTE",
              estadoEtiqueta: "Retiro en proceso",
              solicitadoEn: new Date().toISOString(),
              cuentaDestinoResumen: bankLabel
            },
            ...current.filter((item) => item.operationId !== operationId)
          ]);
        }

        const successMessage = buildWithdrawalSuccessMessage(
          result.montoRetirado || amount,
          result.mensaje
        );
        setWithdrawalNotice(successMessage);
        return { operationId, mensaje: successMessage };
      } catch (submitError) {
        const message =
          submitError instanceof Error
            ? submitError.message
            : "No se pudo registrar la solicitud de retiro.";
        setWithdrawalError(message);
        throw submitError;
      } finally {
        setIsSubmittingWithdrawal(false);
      }
    },
    [bankAccounts, data.disponible.amount]
  );

  return {
    data,
    buckets,
    isLoading,
    isRefreshing,
    error,
    withdrawalNotice,
    setWithdrawalNotice,
    withdrawalError,
    setWithdrawalError,
    isSubmittingWithdrawal,
    bankAccounts,
    bankAccountsLoadError,
    catalogoRegistroCuenta,
    catalogoRegistroLoadError,
    withdrawalHistory,
    withdrawalHistoryError,
    minWithdrawalAmount: MIN_WITHDRAWAL_AMOUNT,
    lastUpdatedLabel: formatLastUpdated(data.updatedAt),
    reload: () => load(true),
    withdrawAvailableBalance
  };
}
