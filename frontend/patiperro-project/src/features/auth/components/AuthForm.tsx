// Layout compartido de las pantallas de login.
// Se encarga del branding visual y del contenedor del formulario.
import styles from "../styles/auth.module.css";

type Props = {
  title: string;
  onSubmit: (e: React.FormEvent) => void;
  buttonText: string;
  children: React.ReactNode;
  isSubmitting?: boolean;
  isSubmitDisabled?: boolean;
  reverseLayout?: boolean;
};

export default function AuthForm({
  title,
  onSubmit,
  buttonText,
  children,
  isSubmitting,
  isSubmitDisabled,
  reverseLayout
}: Props) {
  return (
    <div
      className={`${styles.container} ${reverseLayout ? styles.containerReverse : ""}`}
    >
      {/* Panel visual y branding del formulario. */}
      <div className={styles.left}>
        <div className={styles.leftContent}>
          <h1 className={styles.logo}>Patiperro</h1>
          <p className={styles.subtitle}>
            Encuentra el mejor paseo para tu mascota
          </p>
        </div>
      </div>

      {/* Tarjeta principal donde se renderiza el formulario. */}
      <div className={styles.right}>
        <div className={styles.card}>
          <h2 className={styles.title}>{title}</h2>

          <form onSubmit={onSubmit}>
            {children}

            <button
              className={styles.submitButton}
              type="submit"
              disabled={!!isSubmitting || !!isSubmitDisabled}
            >
              {isSubmitting ? "Ingresando..." : buttonText}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
