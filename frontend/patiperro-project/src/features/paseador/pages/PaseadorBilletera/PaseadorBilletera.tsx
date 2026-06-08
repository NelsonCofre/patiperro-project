import { useMemo, useState } from "react";
import AvailableBalance from "../../components/AvailableBalance/AvailableBalance";
import BalanceCard from "../../components/BalanceCard/BalanceCard";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import WithdrawalRequest from "../../components/WithdrawalRequest/WithdrawalRequest";
import { usePaseadorBilletera } from "../../hooks/usePaseadorBilletera";
import { formatPaseoCount, type BilleteraBucketKey, type BilleteraReservaItem } from "../../services/billeteraPaseadorService";
import styles from "./PaseadorBilletera.module.css";

function formatMoney(value: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

function formatDateShort(value: string): string {
  if (!value) return "Sin fecha";
  const date = new Date(`${value}T12:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("es-CL", { dateStyle: "medium" }).format(date);
}

function formatTime(value: string): string {
  if (!value) return "--:--";
  const date = new Date(`1970-01-01T${value}`);
  if (Number.isNaN(date.getTime())) return value.slice(0, 5);
  return new Intl.DateTimeFormat("es-CL", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

function formatReleaseDate(value?: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("es-CL", { dateStyle: "medium" }).format(date);
}

function formatWithdrawalDate(value: string): string {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("es-CL", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

function getProximaLiberacionCorta(reservas: BilleteraReservaItem[]): string {
  const fechas = reservas
    .map((item) => item.fechaLiberacionEstimada)
    .filter((value): value is string => Boolean(value))
    .map((value) => new Date(value).getTime())
    .filter((value) => Number.isFinite(value))
    .sort((a, b) => a - b);

  if (fechas.length === 0) return "Sin fecha estimada";

  return new Intl.DateTimeFormat("es-CL", { dateStyle: "medium" }).format(new Date(fechas[0]));
}

function getEmptyCopy(selectedBucket: BilleteraBucketKey): { title: string; text: string } {
  if (selectedBucket === "retenido") {
    return {
      title: "Sin saldo retenido",
      text: "Aparecerá aquí cuando tengas paseos pagados en curso o por iniciar."
    };
  }
  if (selectedBucket === "verificacion") {
    return {
      title: "Nada en verificación",
      text: "Los paseos finalizados se muestran aquí mientras se confirman."
    };
  }
  return {
    title: "Sin saldo disponible",
    text: "Cuando se libere tu dinero, podrás retirarlo desde esta vista."
  };
}

function ReservaWalletRow({ reserva }: { reserva: BilleteraReservaItem }) {
  const releaseLabel = formatReleaseDate(reserva.fechaLiberacionEstimada);

  return (
    <article className={styles.reservaRow}>
      <div>
        <h3>{reserva.mascotaNombre}</h3>
        <p className={styles.rowMeta}>
          {formatDateShort(reserva.fecha)} · {formatTime(reserva.horaInicio)}
        </p>
        {releaseLabel ? (
          <p className={styles.rowRelease}>Disponible desde {releaseLabel}</p>
        ) : null}
      </div>
      <div className={styles.rowSide}>
        <strong>{formatMoney(reserva.montoNeto)}</strong>
      </div>
    </article>
  );
}

export default function PaseadorBilletera() {
  const {
    buckets,
    data,
    error,
    isLoading,
    isRefreshing,
    lastUpdatedLabel,
    reload,
    withdrawalNotice,
    setWithdrawalNotice,
    withdrawAvailableBalance,
    withdrawalError,
    setWithdrawalError,
    isSubmittingWithdrawal,
    bankAccounts,
    bankAccountsLoadError,
    catalogoRegistroCuenta,
    catalogoRegistroLoadError,
    withdrawalHistory,
    withdrawalHistoryError,
    minWithdrawalAmount
  } = usePaseadorBilletera();
  const [selectedBucket, setSelectedBucket] = useState<BilleteraBucketKey>("retenido");
  const [isWithdrawalModalOpen, setIsWithdrawalModalOpen] = useState(false);

  const currentBucket = useMemo(() => {
    if (selectedBucket === "verificacion") return data.verificacion;
    if (selectedBucket === "disponible") return data.disponible;
    return data.retenido;
  }, [data.disponible, data.retenido, data.verificacion, selectedBucket]);

  const emptyCopy = getEmptyCopy(selectedBucket);

  return (
    <main className={styles.page}>
      <PaseadorNavbar />

      <section className={styles.hero}>
        <div className={styles.heroMain}>
          <p className={styles.eyebrow}>Mi billetera</p>
          <h1 className={styles.title}>Tus ganancias</h1>
          <p className={styles.description}>
            Revisa cuánto tienes retenido, en verificación o listo para retirar.
          </p>
        </div>
        <div className={styles.heroActions}>
          <span className={styles.syncLabel}>
            {isRefreshing ? "Actualizando…" : `Actualizado ${lastUpdatedLabel}`}
          </span>
          <button type="button" className={styles.refreshButton} onClick={() => void reload()} disabled={isRefreshing}>
            Actualizar
          </button>
        </div>
      </section>

      {withdrawalNotice ? (
        <div className={styles.feedbackBanner} role="status">
          <div>
            <strong>Retiro registrado</strong>
            <p>{withdrawalNotice}</p>
          </div>
          <button type="button" onClick={() => setWithdrawalNotice("")}>
            Cerrar
          </button>
        </div>
      ) : null}

      <section className={styles.balanceGrid}>
        {buckets.map((bucket, index) => (
          <BalanceCard
            key={bucket.key}
            bucket={bucket}
            isPrimary={index === 0}
            isActive={selectedBucket === bucket.key}
            onClick={() => setSelectedBucket(bucket.key)}
            extraLabel={
              bucket.key === "verificacion"
                ? (() => {
                    const proxima = getProximaLiberacionCorta(bucket.reservas);
                    return proxima !== "Sin fecha estimada" ? `Libera ~ ${proxima}` : undefined;
                  })()
                : bucket.key === "disponible"
                  ? "Listo para retiro"
                  : undefined
            }
          />
        ))}
      </section>

      {!isLoading && !error && data.proyeccionLiberacionesPorDia.length > 0 ? (
        <section className={styles.proyeccionSection} aria-label="Próximas liberaciones">
          <div className={styles.proyeccionHeader}>
            <h2>Próximas liberaciones</h2>
            <p className={styles.proyeccionIntro}>Montos en verificación que pasarán a saldo disponible.</p>
          </div>
          {data.proyeccionLiberacionesPorDia.map((grupo) => (
            <div key={grupo.fechaDisponibleDesde} className={styles.proyeccionGrupo}>
              <div className={styles.proyeccionGrupoHeader}>
                <strong>{formatDateShort(grupo.fechaDisponibleDesde)}</strong>
                <span>{formatMoney(grupo.totalNeto)}</span>
              </div>
              {grupo.liberacionPausadaPorDisputa ? (
                <p className={styles.proyeccionDisputa}>Hay un reclamo activo; la liberación puede demorarse.</p>
              ) : null}
              <div className={styles.proyeccionLista}>
                {grupo.reservas.map((reserva) => (
                  <ReservaWalletRow key={`${grupo.fechaDisponibleDesde}-${reserva.idReserva}`} reserva={reserva} />
                ))}
              </div>
            </div>
          ))}
        </section>
      ) : null}

      <AvailableBalance
        bucket={data.disponible}
        isSubmitting={isSubmittingWithdrawal}
        onWithdraw={() => {
          setWithdrawalError("");
          setIsWithdrawalModalOpen(true);
          setSelectedBucket("disponible");
        }}
      />

      <section className={styles.withdrawalsSection} aria-label="Historial de retiros">
        <div className={styles.withdrawalsHeader}>
          <h2>Retiros</h2>
        </div>

        {withdrawalHistoryError ? (
          <article className={styles.errorState} role="alert">
            <strong>No pudimos cargar el historial</strong>
            <p>{withdrawalHistoryError}</p>
          </article>
        ) : withdrawalHistory.length > 0 ? (
          <div className={styles.withdrawalList}>
            {withdrawalHistory.map((withdrawal) => (
              <article
                key={`${withdrawal.operationId}-${withdrawal.idRetiroFondos}`}
                className={styles.withdrawalCard}
              >
                <div>
                  <strong>{formatMoney(withdrawal.monto)}</strong>
                  <p>{withdrawal.estadoEtiqueta}</p>
                  {withdrawal.cuentaDestinoResumen ? (
                    <p className={styles.withdrawalMeta}>{withdrawal.cuentaDestinoResumen}</p>
                  ) : null}
                  {withdrawal.solicitadoEn ? (
                    <p className={styles.withdrawalDate}>{formatWithdrawalDate(withdrawal.solicitadoEn)}</p>
                  ) : null}
                </div>
              </article>
            ))}
          </div>
        ) : (
          <article className={styles.emptyState}>
            <strong>Sin retiros aún</strong>
            <p>Cuando retires saldo disponible, lo verás aquí.</p>
          </article>
        )}
      </section>

      <section className={styles.detailSection}>
        <div className={styles.detailHeader}>
          <div>
            <h2>{currentBucket.title}</h2>
            <p className={styles.detailCount}>
              {formatPaseoCount(currentBucket.reservaCount)} · {formatMoney(currentBucket.amount)}
            </p>
          </div>
        </div>

        {isLoading ? (
          <div className={styles.loadingState}>Cargando billetera…</div>
        ) : error ? (
          <article className={styles.errorState} role="alert">
            <strong>No pudimos cargar tu billetera</strong>
            <p>{error}</p>
            <button type="button" onClick={() => void reload()} disabled={isRefreshing}>
              Reintentar
            </button>
          </article>
        ) : currentBucket.reservas.length > 0 ? (
          <div className={styles.reservasList}>
            {currentBucket.reservas.map((reserva) => (
              <ReservaWalletRow key={reserva.idReserva} reserva={reserva} />
            ))}
          </div>
        ) : (
          <article className={styles.emptyState}>
            <strong>{emptyCopy.title}</strong>
            <p>{emptyCopy.text}</p>
          </article>
        )}
      </section>

      <WithdrawalRequest
        isOpen={isWithdrawalModalOpen}
        availableAmount={data.disponible.amount}
        minWithdrawalAmount={minWithdrawalAmount}
        bankAccounts={bankAccounts}
        bankAccountsLoadError={bankAccountsLoadError}
        catalogoRegistroCuenta={catalogoRegistroCuenta}
        catalogoRegistroLoadError={catalogoRegistroLoadError}
        isSubmitting={isSubmittingWithdrawal}
        submitError={withdrawalError}
        onClose={() => {
          if (isSubmittingWithdrawal) return;
          setWithdrawalError("");
          setIsWithdrawalModalOpen(false);
        }}
        onSubmit={async (amount, bankAccountId, registro) =>
          withdrawAvailableBalance(amount, bankAccountId, registro)
        }
      />
    </main>
  );
}
