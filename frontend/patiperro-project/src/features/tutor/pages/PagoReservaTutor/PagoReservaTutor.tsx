import { Wallet, initMercadoPago } from "@mercadopago/sdk-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getMercadoPagoPublicKey } from "../../../../config/mercadopago";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import { crearPreferenciaCheckoutProTutor } from "../../services/pagoTutorApi";
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
  const [searchParams] = useSearchParams();
  const idReserva = parsePositiveInt(searchParams.get("idReserva") ?? "");
  const total = parsePositiveInt(searchParams.get("total") ?? "") ?? 0;
  const paseador = (searchParams.get("paseador") ?? "Paseador").trim();
  const mascota = (searchParams.get("mascota") ?? "Mascota").trim();
  const bloque = (searchParams.get("bloque") ?? "Bloque no especificado").trim();

  const [errorMessage, setErrorMessage] = useState("");
  const [loadingPreferencia, setLoadingPreferencia] = useState(false);
  const [preferenceId, setPreferenceId] = useState((searchParams.get("preferenceId") ?? "").trim());
  const [successMessage, setSuccessMessage] = useState("");

  const puedeMostrarBrick = useMemo(
    () => Boolean(idReserva && total > 0 && getMercadoPagoPublicKey()),
    [idReserva, total]
  );

  const publicKey = getMercadoPagoPublicKey();
  useEffect(() => {
    if (!publicKey) return;
    initMercadoPago(publicKey, { locale: "es-CL" });
  }, [publicKey]);

  async function handleCrearPreferencia() {
    if (!idReserva || total <= 0) {
      setErrorMessage("Faltan datos de reserva o monto para crear el checkout.");
      return;
    }
    setLoadingPreferencia(true);
    setErrorMessage("");
    setSuccessMessage("");
    try {
      const dto = await crearPreferenciaCheckoutProTutor({
        idReserva,
        montoTotal: total,
        tituloItem: `Reserva #${idReserva}`
      });
      if (!dto.preferenceId) {
        throw new Error("Mercado Pago no devolvió preferenceId.");
      }
      setPreferenceId(dto.preferenceId);
      setSuccessMessage("Preferencia creada. Completa el pago usando el botón de Mercado Pago.");
    } catch (error) {
      const message = error instanceof Error ? error.message : "No se pudo crear la preferencia.";
      setErrorMessage(message);
    } finally {
      setLoadingPreferencia(false);
    }
  }

  useEffect(() => {
    if (preferenceId || loadingPreferencia) return;
    if (!idReserva || total <= 0) return;
    void handleCrearPreferencia();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [idReserva, total, preferenceId]);

  return (
    <main className={styles.page}>
      <TutorNavbar />
      <section className={styles.shell}>
        <header className={styles.heading}>
          <p>Paso 2 de 2</p>
          <h1>Pago de reserva</h1>
          <p>
            Completa el formulario de Mercado Pago aquí mismo (Checkout API). El monto lo confirma el servidor según la
            reserva; no uses otro total.
          </p>
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

          <div className={styles.actions}>
            <Link className={styles.secondary} to="/tutor/dashboard">
              Cancelar
            </Link>
          </div>

          {loadingPreferencia ? <p className={styles.note}>Preparando checkout de Mercado Pago...</p> : null}

          {puedeMostrarBrick && preferenceId ? (
            <div className={styles.brickSection}>
              <Wallet initialization={{ preferenceId }} />
            </div>
          ) : !preferenceId ? (
            <p className={styles.note}>
              {!idReserva || total <= 0
                ? "Faltan datos de reserva o monto en la URL. Vuelve desde «Mis reservas»."
                : "Configura la Public Key en src/config/mercadopago.ts para habilitar el pago embebido."}
            </p>
          ) : null}

          {successMessage ? <div className={styles.success}>{successMessage}</div> : null}
          {errorMessage ? <div className={styles.error}>{errorMessage}</div> : null}
        </article>
      </section>
    </main>
  );
}
