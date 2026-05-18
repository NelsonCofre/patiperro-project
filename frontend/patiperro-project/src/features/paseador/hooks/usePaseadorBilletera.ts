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
    reservas: []
  };
}

const EMPTY_DATA: BilleteraPaseadorData = {
  retenido: createEmptyBucket(
    "retenido",
    "Saldo Retenido",
    "Servicios pagados que aun no han finalizado o siguen en curso."
  ),
  verificacion: createEmptyBucket(
    "verificacion",
    "Saldo en Verificacion",
    "Paseos finalizados que estan cumpliendo el periodo de liberacion N+2."
  ),
  disponible: createEmptyBucket(
    "disponible",
    "Saldo Disponible",
    "Fondos liberados y listos para retiro desde tu billetera."
  ),
  proyeccionLiberacionesPorDia: [],
  updatedAt: ""
};

function formatLastUpdated(value: string): string {
  if (!value) return "Sin sincronizacion aun";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Sin sincronizacion aun";
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
            : "No se pudo cargar el catalogo de bancos."
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
          throw new Error(
            `El monto debe ser mayor o igual a ${new Intl.NumberFormat("es-CL", {
              style: "currency",
              currency: "CLP",
              maximumFractionDigits: 0
            }).format(MIN_WITHDRAWAL_AMOUNT)}.`
          );
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
          throw new Error("Complete sus datos bancarios antes de solicitar el retiro.");
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

        setWithdrawalNotice(`${result.mensaje} Operacion ${operationId}.`);
        return { operationId };
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
