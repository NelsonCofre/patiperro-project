import { useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import { iniciarCheckoutPro } from "../../services/pagoTutorApi";
import styles from "./PagoReservaTutor.module.css";

function formatCurrency(value: number) {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0
  }).format(value);
}

function parsePositiveInt(value: string): number | null {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

export default function PagoReservaTutor() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const idReserva = parsePositiveInt(searchParams.get("idReserva") ?? "");
  const total = parsePositiveInt(searchParams.get("total") ?? "") ?? 0;
  const paseador = (searchParams.get("paseador") ?? "Paseador").trim();
  const mascota = (searchParams.get("mascota") ?? "Mascota").trim();
  const bloque = (searchParams.get("bloque") ?? "Bloque no especificado").trim();

  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<"idle" | "ok" | "error">("idle");
  const [message, setMessage] = useState("");

  const canPay = useMemo(() => Boolean(idReserva && total > 0) && !loading, [idReserva, total, loading]);

  async function handlePagar() {
    if (!idReserva || total <= 0 || loading) return;
    setLoading(true);
    setStatus("idle");
    setMessage("");

    try {
      const { initPoint } = await iniciarCheckoutPro(idReserva);
      window.location.assign(initPoint);
    } catch (error) {
      const errorMessage =
        error instanceof Error ? error.message : "No se pudo iniciar el pago con Mercado Pago.";
      setStatus("error");
      setMessage(errorMessage);
      setLoading(false);
    }
  }

  return (
    <main className={styles.page}>
      <TutorNavbar />
      <section className={styles.shell}>
        <header className={styles.heading}>
          <p>Paso 2 de 2</p>
          <h1>Detalle de pago de reserva</h1>
          <p>Revisa la información y luego presiona &quot;Ir a pagar&quot; para abrir Mercado Pago (Checkout Pro).</p>
        </header>

        <article className={styles.card}>
          <div className={styles.list}>
            <div className={styles.row}>
              <span>Reserva</span>
              <strong>{idReserva ? `#${idReserva}` : "No disponible"}</strong>
            </div>
            <div className={styles.row}>
              <span>Paseador</span>
              <strong>{paseador}</strong>
            </div>
            <div className={styles.row}>
              <span>Mascota</span>
              <strong>{mascota}</strong>
            </div>
            <div className={styles.row}>
              <span>Bloque</span>
              <strong>{bloque}</strong>
            </div>
          </div>

          <div className={styles.total}>
            <span>Total a pagar</span>
            <strong>{total > 0 ? formatCurrency(total) : "--"}</strong>
          </div>

          <p className={styles.note}>
            Serás redirigido a Mercado Pago. Al aprobar el pago, la reserva pasará a pagada y podrás seguir desde
            &quot;Mis reservas&quot;.
          </p>

          <div className={styles.actions}>
            <Link className={styles.secondary} to="/tutor/dashboard">Cancelar</Link>
            <button className={styles.primary} type="button" onClick={handlePagar} disabled={!canPay}>
              {loading ? "Procesando..." : "Ir a pagar"}
            </button>
          </div>

          {status === "ok" ? <div className={styles.success}>{message}</div> : null}
          {status === "error" ? <div className={styles.error}>{message}</div> : null}

          {status === "ok" ? (
            <div className={styles.actions}>
              <button
                type="button"
                className={styles.primary}
                onClick={() => navigate("/tutor/reservas")}
              >
                Ver mis reservas
              </button>
            </div>
          ) : null}
        </article>
      </section>
    </main>
  );
}
