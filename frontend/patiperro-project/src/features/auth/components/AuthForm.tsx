import styles from "../styles/auth.module.css";

type Props = {
  title: string;
  onSubmit: (e: React.FormEvent) => void;
  buttonText: string;
  children: React.ReactNode;
  isSubmitting?: boolean;
};

export default function AuthForm({ title, onSubmit, buttonText, children, isSubmitting }: Props) {
  return (
    <div className={styles.container}>

      {/* 🐶 LADO IZQUIERDO */}
      <div className={styles.left}>
        <div className={styles.leftContent}>
          <h1 className={styles.logo}>🐶 Patiperro</h1>
          <p className={styles.subtitle}>
            Encuentra el mejor paseo para tu mascota
          </p>
        </div>
      </div>

      {/* 📋 LADO DERECHO */}
      <div className={styles.right}>
        <div className={styles.card}>

          <h2 className={styles.title}>{title}</h2>

          <form onSubmit={onSubmit}>
            {children}

            <button
              className={styles.submitButton}
              type="submit"
              disabled={!!isSubmitting}
            >
              {isSubmitting ? "Ingresando..." : buttonText}
            </button>
          </form>

        </div>
      </div>

    </div>
  );
}