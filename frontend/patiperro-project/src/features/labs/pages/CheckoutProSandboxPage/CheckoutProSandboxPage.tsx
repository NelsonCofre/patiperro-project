import { Wallet, initMercadoPago } from "@mercadopago/sdk-react";
import { useMemo, useState } from "react";
import type { FormEvent } from "react";
import { getMercadoPagoPublicKey } from "../../../../config/mercadopago";
import {
  crearPreferenciaCheckoutPro,
  type CrearPreferenciaResponse
} from "../../services/checkoutProSandboxApi";
import styles from "./CheckoutProSandboxPage.module.css";

let sdkInitializedWithKey: string | null = null;

function ensureMercadoPagoInit(publicKey: string) {
  const k = publicKey.trim();
  if (!k || sdkInitializedWithKey === k) return;
  initMercadoPago(k, { locale: "es-CL" });
  sdkInitializedWithKey = k;
}

export default function CheckoutProSandboxPage() {
  const [idReserva, setIdReserva] = useState("123");
  const [montoTotal, setMontoTotal] = useState("5000");
  const [tituloItem, setTituloItem] = useState("Reserva paseo MVP");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [resultado, setResultado] = useState<CrearPreferenciaResponse | null>(null);

  const publicKey = useMemo(() => getMercadoPagoPublicKey(), []);
  ensureMercadoPagoInit(publicKey);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setResultado(null);

    const parsedReserva = Number(idReserva);
    const parsedMonto = Number(montoTotal);
    if (!Number.isFinite(parsedReserva) || parsedReserva <= 0) {
      setError("idReserva debe ser un numero mayor a 0.");
      return;
    }
    if (!Number.isFinite(parsedMonto) || parsedMonto <= 0) {
      setError("montoTotal debe ser un numero mayor a 0.");
      return;
    }

    setLoading(true);
    try {
      const pref = await crearPreferenciaCheckoutPro({
        idReserva: parsedReserva,
        montoTotal: parsedMonto,
        tituloItem: tituloItem.trim() || "Reserva paseo MVP"
      });
      setResultado(pref);
    } catch (err) {
      setError(err instanceof Error ? err.message : "No se pudo crear la preferencia.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className={styles.page}>
      <h1 className={styles.title}>Sandbox Checkout Pro (modulo aislado)</h1>
      <p className={styles.hint}>
        Usa comprador de prueba distinto al vendedor de la app/token. Si falla en sandbox, suele ser mezcla de cuentas
        reales y test. Este modulo crea la preferencia en <code>pagos-service</code> (requiere{" "}
        <code>VITE_PAGOS_INTERNO_SECRET</code>).
      </p>

      <form className={styles.form} onSubmit={handleSubmit}>
        <label className={styles.field}>
          <span>ID reserva</span>
          <input value={idReserva} onChange={(e) => setIdReserva(e.target.value)} />
        </label>
        <label className={styles.field}>
          <span>Monto total (CLP)</span>
          <input value={montoTotal} onChange={(e) => setMontoTotal(e.target.value)} />
        </label>
        <label className={styles.field}>
          <span>Titulo item</span>
          <input value={tituloItem} onChange={(e) => setTituloItem(e.target.value)} />
        </label>
        <div className={styles.actions}>
          <button className={styles.primary} type="submit" disabled={loading}>
            {loading ? "Creando preferencia..." : "Crear preferencia"}
          </button>
        </div>
      </form>

      {error ? <p className={styles.error}>{error}</p> : null}

      {resultado ? (
        <section className={styles.box}>
          <div className={styles.links}>
            <strong>Preference ID:</strong> <span>{resultado.preferenceId}</span>
            <strong>init_point:</strong>
            <a href={resultado.initPoint} target="_blank" rel="noreferrer">
              {resultado.initPoint}
            </a>
            <strong>sandbox_init_point:</strong>
            <a href={resultado.sandboxInitPoint} target="_blank" rel="noreferrer">
              {resultado.sandboxInitPoint}
            </a>
            <strong>urlCheckout:</strong>
            <a href={resultado.urlCheckout} target="_blank" rel="noreferrer">
              {resultado.urlCheckout}
            </a>
          </div>

          <div className={styles.walletWrap}>
            <strong>SDK Wallet (Checkout Pro)</strong>
            <Wallet initialization={{ preferenceId: resultado.preferenceId }} />
          </div>
        </section>
      ) : null}
    </main>
  );
}
