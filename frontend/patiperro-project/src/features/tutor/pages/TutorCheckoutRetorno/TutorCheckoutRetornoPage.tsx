import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
import { sincronizarCheckoutProPago } from "../../services/pagoTutorApi";
import styles from "./TutorCheckoutRetorno.module.css";

type RetornoTipo = "success" | "failure" | "pending";

const CONTENIDO: Record<
  RetornoTipo,
  { titulo: string; descripcion: string }
> = {
  success: {
    titulo: "Volviste desde Mercado Pago",
    descripcion:
      "Si el pago fue aprobado, en unos segundos verás la reserva actualizada en «Mis reservas». " +
      "Si no ves el cambio al instante, espera un momento y actualiza la página."
  },
  failure: {
    titulo: "No se completó el pago",
    descripcion:
      "Puedes intentar de nuevo desde «Mis reservas» cuando quieras. Tu reserva puede seguir pendiente de pago según su estado."
  },
  pending: {
    titulo: "Pago pendiente",
    descripcion:
      "Mercado Pago está procesando el medio de pago o esperando confirmación. Revisa más tarde en «Mis reservas»."
  }
};

/**
 * Páginas de retorno tras Checkout Pro (back_urls). Sin llamadas a API ni creación de preferencia al montar.
 */
export default function TutorCheckoutRetornoPage({ tipo }: { tipo: RetornoTipo }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const collectionStatus = searchParams.get("collection_status");
  const paymentId = searchParams.get("payment_id");
  const status = searchParams.get("status");
  const externalReference = searchParams.get("external_reference");
  const syncStarted = useRef(false);
  const [syncMessage, setSyncMessage] = useState<string | null>(null);
  const [syncError, setSyncError] = useState<string | null>(null);

  const idReservaRetorno = useMemo(() => {
    const direct = searchParams.get("idReserva");
    if (direct && /^\d+$/.test(direct)) {
      return Number(direct);
    }
    const match = (externalReference ?? "").match(/reserva-(\d+)/i);
    if (match?.[1]) {
      return Number(match[1]);
    }
    return null;
  }, [externalReference, searchParams]);

  const destinoReserva = useMemo(() => {
    if (!idReservaRetorno) {
      return "/tutor/reservas";
    }
    return `/tutor/reservas?reservaId=${idReservaRetorno}&openComprobante=1`;
  }, [idReservaRetorno]);

  useEffect(() => {
    if (tipo !== "success" || !paymentId?.trim()) {
      return;
    }
    if (syncStarted.current) {
      return;
    }
    syncStarted.current = true;
    setSyncMessage(null);
    setSyncError(null);
    void (async () => {
      try {
        await sincronizarCheckoutProPago(paymentId);
        setSyncMessage(
          "Confirmamos el pago con Mercado Pago y actualizamos tu reserva. Si no ves el cambio, actualiza «Mis reservas»."
        );
      } catch (e) {
        const message =
          e instanceof Error
            ? e.message
            : "No se pudo confirmar el pago automáticamente. Revisa «Mis reservas» o intenta más tarde.";
        // En retorno de Checkout Pro, un 401/403 puede ocurrir por sesión expirada aunque el webhook
        // ya haya confirmado el pago; evitar mensaje alarmante en flujo exitoso.
        if (message.includes("HTTP 401") || message.includes("HTTP 403")) {
          setSyncMessage(
            "Volviste correctamente desde Mercado Pago. Si tu reserva ya figura pagada en «Mis reservas», no necesitas repetir el pago."
          );
          return;
        }
        setSyncError(message);
      }
    })();
  }, [paymentId, tipo]);

  useEffect(() => {
    if (tipo !== "success" || !idReservaRetorno) {
      return;
    }
    const timer = window.setTimeout(() => {
      navigate(destinoReserva, { replace: true });
    }, 1500);
    return () => window.clearTimeout(timer);
  }, [destinoReserva, idReservaRetorno, navigate, tipo]);

  const copy = CONTENIDO[tipo];

  return (
    <main className={styles.page}>
      <TutorNavbar />
      <section className={styles.shell}>
        <article className={styles.card}>
          <h1 className={styles.title}>{copy.titulo}</h1>
          <p className={styles.text}>{copy.descripcion}</p>
          {(collectionStatus || paymentId || status) && (
            <p className={styles.muted}>
              Referencia Mercado Pago
              {collectionStatus ? ` · collection_status=${collectionStatus}` : ""}
              {status ? ` · status=${status}` : ""}
              {paymentId ? ` · payment_id=${paymentId}` : ""}
            </p>
          )}
          {syncMessage && <p className={styles.text}>{syncMessage}</p>}
          {syncError && <p className={styles.text}>{syncError}</p>}
          <Link className={styles.link} to={destinoReserva}>
            Ir a la reserva pagada
          </Link>
        </article>
      </section>
    </main>
  );
}
