import styles from "./CodigoEncuentro.module.css";

type Props = {
  codigo: number | string | null;
};

function formatCodigo(codigo: Props["codigo"]): string | null {
  if (codigo == null) return null;

  const digitsOnly = String(codigo).replace(/\D/g, "");
  if (!digitsOnly) return null;

  return digitsOnly.padStart(4, "0").slice(-4);
}

export default function CodigoEncuentro({ codigo }: Props) {
  const formatted = formatCodigo(codigo);

  return (
    <section className={styles.card} aria-label="Codigo de encuentro">
      <div className={styles.header}>
        <span className={styles.eyebrow}>Codigo de encuentro</span>
        <strong>Reserva aceptada</strong>
      </div>

      {formatted ? (
        <div className={styles.codeWrap} aria-label={`Codigo ${formatted}`}>
          {formatted.split("").map((digit, index) => (
            <span key={`${digit}-${index}`} className={styles.digit}>
              {digit}
            </span>
          ))}
        </div>
      ) : (
        <div className={styles.pending}>Codigo pendiente de generacion</div>
      )}

      <p className={styles.helper}>
        Muestrale este codigo al paseador al momento del encuentro. No hay opcion para
        copiarlo ni compartirlo desde la app.
      </p>
    </section>
  );
}
