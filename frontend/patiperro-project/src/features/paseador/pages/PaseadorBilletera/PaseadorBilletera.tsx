import { useMemo, useState } from "react";
import AvailableBalance from "../../components/AvailableBalance/AvailableBalance";
import BalanceCard from "../../components/BalanceCard/BalanceCard";
import GananciasBreakdown from "../../components/GananciasBreakdown/GananciasBreakdown";
import PaseadorNavbar from "../../components/PaseadorNavbar/PaseadorNavbar";
import WithdrawalRequest from "../../components/WithdrawalRequest/WithdrawalRequest";
import { usePaseadorBilletera } from "../../hooks/usePaseadorBilletera";
import type {
  BilleteraBucketKey,
  BilleteraReservaItem
} from "../../services/billeteraPaseadorService";
import styles from "./PaseadorBilletera.module.css";

function formatMoney(value: number): string {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

function formatDate(value: string): string {
  if (!value) return "Fecha pendiente";
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("es-CL", { dateStyle: "full" }).format(date);
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

function formatDateTime(value?: string | null): string {
  if (!value) return "Sin liberacion programada";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Sin liberacion programada";
  return new Intl.DateTimeFormat("es-CL", {
    dateStyle: "full",
    timeStyle: "short"
  }).format(date);
}

function formatWithdrawalDate(value: string): string {
  if (!value) return "Fecha no disponible";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("es-CL", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

function getProximaLiberacion(reservas: BilleteraReservaItem[]): string {
  const fechas = reservas
    .map((item) => item.fechaLiberacionEstimada)
    .filter((value): value is string => Boolean(value))
    .map((value) => new Date(value).getTime())
    .filter((value) => Number.isFinite(value))
    .sort((a, b) => a - b);

  if (fechas.length === 0) return "Sin liberaciones programadas";

  return new Intl.DateTimeFormat("es-CL", {
    dateStyle: "full",
    timeStyle: "short"
  }).format(new Date(fechas[0]));
}

function getEmptyCopy(selectedBucket: BilleteraBucketKey): { title: string; text: string } {
  if (selectedBucket === "retenido") {
    return {
      title: "Todavia no tienes saldo retenido",
      text: "Cuando un paseo pagado quede aceptado o en curso, aparecera aqui con su desglose por reserva."
    };
  }
  if (selectedBucket === "verificacion") {
    return {
      title: "No tienes montos en verificacion",
      text: "Los paseos finalizados se mostraran aqui durante el periodo de espera antes de liberar el saldo."
    };
  }
  return {
    title: "No tienes saldo disponible aun",
    text: "Una vez cumplido el plazo de liberacion, tu dinero listo para retiro aparecera en esta vista."
  };
}

function ReservaWalletRow({ reserva }: { reserva: BilleteraReservaItem }) {
  return (
    <article className={styles.reservaRow}>
      <div>
        <p className={styles.rowEyebrow}>Reserva #{reserva.idReserva}</p>
        <h3>{reserva.mascotaNombre}</h3>
        <p className={styles.rowMeta}>
          Tutor: {reserva.tutorNombre} - {formatDate(reserva.fecha)} - {formatTime(reserva.horaInicio)}
        </p>
        {reserva.fechaLiberacionEstimada ? (
          <p className={styles.rowRelease}>
            Disponible desde: {formatDateTime(reserva.fechaLiberacionEstimada)}
          </p>
        ) : null}
      </div>
      <div className={styles.rowSide}>
        <strong>{formatMoney(reserva.montoNeto)}</strong>
        <span>{reserva.estado}</span>
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
  const totalWalletAmount = buckets.reduce((sum, bucket) => sum + bucket.amount, 0);

  return (
    <main className={styles.page}>
      <PaseadorNavbar />

      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <p className={styles.eyebrow}>Mi Billetera</p>
          <h1 className={styles.title}>Sigue tu saldo retenido y el avance de cada reserva</h1>
          <p className={styles.description}>
            Esta vista separa tus fondos por etapa para que entiendas con claridad que monto
            sigue retenido, cual esta en verificacion y que dinero ya se encuentra listo para retiro.
          </p>

          <div className={styles.infoBanner}>
            <strong>Mensaje de seguridad</strong>
            <p>
              Este saldo corresponde a servicios pagados que aun no han finalizado. Una vez
              que marques el paseo como terminado, este monto iniciara su proceso de liberacion.
            </p>
          </div>

          <div className={styles.heroHighlights}>
            <article className={styles.highlightCard}>
              <span>Fondo total monitoreado</span>
              <strong>{formatMoney(totalWalletAmount)}</strong>
              <p>Suma consolidada entre fondos retenidos, en verificacion y disponibles.</p>
            </article>
            <article className={styles.highlightCard}>
              <span>Reservas con impacto en billetera</span>
              <strong>{buckets.reduce((sum, bucket) => sum + bucket.reservas.length, 0)}</strong>
              <p>Cada monto se puede abrir para revisar exactamente que reservas lo componen.</p>
            </article>
          </div>
        </div>

        <aside className={styles.syncPanel}>
          <span>Actualizacion de billetera</span>
          <strong>{isRefreshing ? "Sincronizando..." : "Al dia"}</strong>
          <p>Ultima revision: {lastUpdatedLabel}</p>
          <button type="button" onClick={() => void reload()} disabled={isRefreshing}>
            Actualizar ahora
          </button>
        </aside>
      </section>

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
                ? `Proxima liberacion: ${getProximaLiberacion(bucket.reservas)}`
                : bucket.key === "disponible"
                  ? "Fondos ya listos para retiro"
                  : "Servicios aun dentro del ciclo de resguardo"
            }
          />
        ))}
      </section>

      {!isLoading && !error && data.proyeccionLiberacionesPorDia.length > 0 ? (
        <section className={styles.proyeccionSection} aria-label="Proyección de liberación a saldo disponible">
          <div className={styles.proyeccionHeader}>
            <p className={styles.cardEyebrow}>Calendario de liberación</p>
            <h2>Cuándo pasará a disponible lo que está en verificación</h2>
            <p className={styles.proyeccionIntro}>
              Agrupación según la misma regla N+2 del backend (día del fin del paseo más dos días calendario). Si hay
              disputa activa en una reserva, la liberación puede quedar en pausa hasta que soporte la cierre.
            </p>
          </div>
          {data.proyeccionLiberacionesPorDia.map((grupo) => (
            <div key={grupo.fechaDisponibleDesde} className={styles.proyeccionGrupo}>
              <div className={styles.proyeccionGrupoHeader}>
                <strong>Disponible desde: {formatDate(grupo.fechaDisponibleDesde)}</strong>
                <span>Total neto: {formatMoney(grupo.totalNeto)}</span>
              </div>
              {grupo.liberacionPausadaPorDisputa ? (
                <p className={styles.proyeccionDisputa}>
                  Hay al menos una reserva en este grupo con disputa activa: la liberación automática puede estar en
                  pausa.
                </p>
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

      <GananciasBreakdown bucket={currentBucket} />

      {withdrawalNotice ? (
        <div className={styles.feedbackBanner} role="status">
          <span>{withdrawalNotice}</span>
          <button type="button" onClick={() => setWithdrawalNotice("")}>
            Cerrar
          </button>
        </div>
      ) : null}

      <section className={styles.withdrawalsSection} aria-label="Historial de retiros">
        <div className={styles.withdrawalsHeader}>
          <div>
            <p className={styles.cardEyebrow}>Historial de retiros</p>
            <h2>Solicitudes a tu cuenta bancaria</h2>
            <p className={styles.sectionText}>
              Cada retiro descuenta tu saldo disponible al momento de la solicitud. El estado refleja el procesamiento
              simulado de la transferencia.
            </p>
          </div>
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
                  <strong>{withdrawal.operationId}</strong>
                  <p>{withdrawal.cuentaDestinoResumen ?? "Cuenta bancaria registrada"}</p>
                  <p className={styles.withdrawalDate}>
                    Solicitado: {formatWithdrawalDate(withdrawal.solicitadoEn)}
                  </p>
                </div>
                <div className={styles.withdrawalCardSide}>
                  <strong>{formatMoney(withdrawal.monto)}</strong>
                  <span>{withdrawal.estadoEtiqueta}</span>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <article className={styles.emptyState}>
            <strong>Aun no tienes retiros registrados</strong>
            <p>Cuando solicites un retiro desde tu saldo disponible, aparecera aqui con su numero de operacion.</p>
          </article>
        )}
      </section>

      <section className={styles.detailSection}>
        <div className={styles.detailHeader}>
          <div>
            <p className={styles.cardEyebrow}>Desglose por reserva</p>
            <h2>{currentBucket.title}</h2>
            <p className={styles.sectionText}>{currentBucket.helper}</p>
          </div>
          <div className={styles.detailSummary}>
            <div className={styles.detailSummaryBlock}>
              <span>Monto neto</span>
              <strong>{formatMoney(currentBucket.amount)}</strong>
            </div>
            <div className={styles.detailSummaryBlock}>
              <span>Reservas</span>
              <strong>{currentBucket.reservas.length}</strong>
            </div>
          </div>
        </div>

        {isLoading ? (
          <div className={styles.loadingState}>Cargando billetera del paseador...</div>
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
        onSubmit={async (amount, bankAccountId, registro) => {
          await withdrawAvailableBalance(amount, bankAccountId, registro);
        }}
      />
    </main>
  );
}
