import { useCallback, useEffect, useMemo, useState } from "react";
import {
  fetchBilleteraPaseador,
  type BilleteraBucket,
  type BilleteraBucketKey,
  type BilleteraReservaItem,
  type BilleteraPaseadorData
} from "../services/billeteraPaseadorService";

const REFRESH_MS = 15000;

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

function cloneBucketWithoutReservas(
  bucket: BilleteraBucket,
  idsToRemove: Set<number>
): BilleteraBucket {
  const reservas = bucket.reservas.filter((reserva) => !idsToRemove.has(reserva.idReserva));
  return {
    ...bucket,
    reservas,
    amount: reservas.reduce((sum, reserva) => sum + reserva.montoNeto, 0),
    grossAmount: reservas.reduce((sum, reserva) => sum + reserva.montoBruto, 0),
    commissionAmount: reservas.reduce((sum, reserva) => sum + reserva.comision, 0)
  };
}

export function usePaseadorBilletera() {
  const [data, setData] = useState<BilleteraPaseadorData>(EMPTY_DATA);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [withdrawalNotice, setWithdrawalNotice] = useState("");

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

  const withdrawAvailableBalance = useCallback(() => {
    let withdrawn: BilleteraReservaItem[] = [];

    setData((current) => {
      withdrawn = current.disponible.reservas;
      if (withdrawn.length === 0) return current;

      const withdrawnIds = new Set(withdrawn.map((reserva) => reserva.idReserva));
      return {
        ...current,
        disponible: cloneBucketWithoutReservas(current.disponible, withdrawnIds),
        updatedAt: new Date().toISOString()
      };
    });

    if (withdrawn.length > 0) {
      setWithdrawalNotice(
        `Solicitud de retiro creada por ${new Intl.NumberFormat("es-CL", {
          style: "currency",
          currency: "CLP",
          maximumFractionDigits: 0
        }).format(withdrawn.reduce((sum, reserva) => sum + reserva.montoNeto, 0))}. El saldo disponible se desconto en tiempo real.`
      );
    }
  }, []);

  return {
    data,
    buckets,
    isLoading,
    isRefreshing,
    error,
    withdrawalNotice,
    setWithdrawalNotice,
    lastUpdatedLabel: formatLastUpdated(data.updatedAt),
    reload: () => load(true),
    withdrawAvailableBalance
  };
}
