import { Link, useSearchParams } from "react-router-dom";
import TutorNavbar from "../../components/TutorNavbar/TutorNavbar";
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
  const [searchParams] = useSearchParams();
  const collectionStatus = searchParams.get("collection_status");
  const paymentId = searchParams.get("payment_id");
  const status = searchParams.get("status");

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
          <Link className={styles.link} to="/tutor/reservas">
            Ir a mis reservas
          </Link>
        </article>
      </section>
    </main>
  );
}
