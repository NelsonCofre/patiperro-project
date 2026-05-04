import { useCallback, useEffect, useMemo, useState } from "react";
import {
  fetchBilleteraPaseador,
  type BilleteraBucket,
  type BilleteraBucketKey,
  type BilleteraPaseadorData
} from "../services/billeteraPaseadorService";

const REFRESH_MS = 15000;
export const MIN_WITHDRAWAL_AMOUNT = 5000;

export type PaseadorBankAccount = {
  id: string;
  bankName: string;
  accountType: string;
  accountNumberMasked: string;
  holderName: string;
};

export type PendingWithdrawal = {
  operationId: string;
  amount: number;
  bankAccountId: string;
  bankLabel: string;
  requestedAt: string;
  status: "Retiro en Proceso";
};

const MOCK_BANK_ACCOUNTS: PaseadorBankAccount[] = [
  {
    id: "cta-001",
    bankName: "Banco de Chile",
    accountType: "Cuenta Corriente",
    accountNumberMasked: "****8432",
    holderName: "Paseador Patiperro"
  },
  {
    id: "cta-002",
    bankName: "BancoEstado",
    accountType: "Cuenta Vista",
    accountNumberMasked: "****2190",
    holderName: "Paseador Patiperro"
  }
];

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
  const [pendingWithdrawals, setPendingWithdrawals] = useState<PendingWithdrawal[]>([]);
  const [isSubmittingWithdrawal, setIsSubmittingWithdrawal] = useState(false);
  const [withdrawalError, setWithdrawalError] = useState("");

  const load = useCallback(async (silent = false) => {
    if (silent) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }
    setError("");
    try {
      const next = await fetchBilleteraPaseador();
      setData(next);
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
    async (amount: number, bankAccountId: string) => {
      setWithdrawalError("");
      setWithdrawalNotice("");
      setIsSubmittingWithdrawal(true);

      try {
        const selectedAccount = MOCK_BANK_ACCOUNTS.find((account) => account.id === bankAccountId);
        if (!selectedAccount) {
          throw new Error("Complete sus datos bancarios antes de solicitar el retiro.");
        }
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

        await new Promise((resolve) => window.setTimeout(resolve, 900));

        const operationId = `RET-${new Date()
          .toISOString()
          .slice(0, 10)
          .replace(/-/g, "")}-${Math.floor(Math.random() * 9000 + 1000)}`;

        setData((current) => {
          const nextAmount = Math.max(current.disponible.amount - amount, 0);
          return {
            ...current,
            disponible: {
              ...current.disponible,
              amount: nextAmount
            },
            updatedAt: new Date().toISOString()
          };
        });

        setPendingWithdrawals((current) => [
          {
            operationId,
            amount,
            bankAccountId: selectedAccount.id,
            bankLabel: `${selectedAccount.bankName} · ${selectedAccount.accountType} · ${selectedAccount.accountNumberMasked}`,
            requestedAt: new Date().toISOString(),
            status: "Retiro en Proceso"
          },
          ...current
        ]);

        setWithdrawalNotice(
          `Su solicitud de retiro ha sido recibida y sera procesada en las proximas 48 horas habiles. Operacion ${operationId}.`
        );
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
    [data.disponible.amount]
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
    bankAccounts: MOCK_BANK_ACCOUNTS,
    pendingWithdrawals,
    minWithdrawalAmount: MIN_WITHDRAWAL_AMOUNT,
    lastUpdatedLabel: formatLastUpdated(data.updatedAt),
    reload: () => load(true),
    withdrawAvailableBalance
  };
}
