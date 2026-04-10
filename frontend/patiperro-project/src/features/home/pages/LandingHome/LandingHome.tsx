import { Link } from "react-router-dom";
import styles from "./LandingHome.module.css";

const featureCards = [
  {
    title: "Agenda clara",
    text: "Organiza paseos, disponibilidad y bloqueos desde una vista pensada para el dia a dia."
  },
  {
    title: "Mascotas mejor descritas",
    text: "Los tutores pueden registrar informacion clave para que cada paseo se ajuste mejor a su perro."
  },
  {
    title: "Conexion por confianza",
    text: "La plataforma ayuda a que tutores y paseadores conecten con informacion util desde el primer vistazo."
  }
];

const roleCards = [
  {
    eyebrow: "Para tutores",
    title: "Empieza como tutor",
    text: "Registra a tu mascota, revisa informacion clara del servicio y prepara futuras reservas con mas contexto.",
    primaryLabel: "Crear cuenta de tutor",
    primaryTo: "/register/tutor",
    secondaryLabel: "Ya tengo cuenta",
    secondaryTo: "/login/tutor"
  },
  {
    eyebrow: "Para paseadores",
    title: "Empieza como paseador",
    text: "Configura tarifas, administra tu agenda y muestra tu servicio de forma profesional desde el primer dia.",
    primaryLabel: "Crear cuenta de paseador",
    primaryTo: "/register/paseador",
    secondaryLabel: "Ya tengo cuenta",
    secondaryTo: "/login/paseador"
  }
];

export default function LandingHome() {
  return (
    <main className={styles.page}>
      <section className={styles.hero}>
        <header className={styles.navbar}>
          <div>
            <p className={styles.brandEyebrow}>Patiperro</p>
            <strong className={styles.brandTitle}>Paseos con mas contexto y confianza</strong>
          </div>

          <div className={styles.navActions}>
            <Link to="/login/tutor" className={styles.navLink}>
              Ingresar como tutor
            </Link>
            <Link to="/login/paseador" className={styles.navButton}>
              Ingresar como paseador
            </Link>
          </div>
        </header>

        <div className={styles.heroGrid}>
          <div className={styles.heroContent}>
            <p className={styles.eyebrow}>Home principal</p>
            <h1 className={styles.title}>Encuentra tu mejor punto de partida en Patiperro</h1>
            <p className={styles.description}>
              Un espacio pensado para que tutores y paseadores comiencen rapido,
              entiendan el servicio y entren al flujo correcto sin perderse entre pantallas.
            </p>

            <div className={styles.heroActions}>
              <Link to="/register/tutor" className={styles.primaryButton}>
                Empezar como tutor
              </Link>
              <Link to="/register/paseador" className={styles.secondaryButton}>
                Empezar como paseador
              </Link>
            </div>

            <div className={styles.featureRow}>
              {featureCards.map((item) => (
                <article key={item.title} className={styles.featureCard}>
                  <strong>{item.title}</strong>
                  <p>{item.text}</p>
                </article>
              ))}
            </div>
          </div>

          <aside className={styles.heroPanel}>
            <p className={styles.panelEyebrow}>Elegir tu rol</p>
            <h2>Todo empieza con una entrada clara</h2>
            <p>
              Decide si vienes a cuidar perros o a encontrar a la persona ideal para
              pasear al tuyo. El home te dirige al flujo correcto desde el primer clic.
            </p>

            <div className={styles.panelSteps}>
              <div className={styles.stepCard}>
                <span>01</span>
                <strong>Explora</strong>
                <p>Entiende rapidamente que ofrece la plataforma.</p>
              </div>
              <div className={styles.stepCard}>
                <span>02</span>
                <strong>Elige tu rol</strong>
                <p>Entra como tutor o como paseador segun tu necesidad.</p>
              </div>
              <div className={styles.stepCard}>
                <span>03</span>
                <strong>Comienza</strong>
                <p>Accede al flujo de registro o inicio de sesion correspondiente.</p>
              </div>
            </div>
          </aside>
        </div>
      </section>

      <section className={styles.rolesSection}>
        <div className={styles.rolesHeader}>
          <p className={styles.rolesEyebrow}>Elige como quieres comenzar</p>
          <h2>Dos caminos claros, una experiencia consistente</h2>
          <p>
            Diseñamos este inicio para que cualquier persona que llegue al sitio sepa
            exactamente hacia donde ir y que puede esperar del producto.
          </p>
        </div>

        <div className={styles.rolesGrid}>
          {roleCards.map((role) => (
            <article key={role.title} className={styles.roleCard}>
              <p className={styles.roleEyebrow}>{role.eyebrow}</p>
              <h3>{role.title}</h3>
              <p>{role.text}</p>

              <div className={styles.roleActions}>
                <Link to={role.primaryTo} className={styles.rolePrimaryButton}>
                  {role.primaryLabel}
                </Link>
                <Link to={role.secondaryTo} className={styles.roleSecondaryLink}>
                  {role.secondaryLabel}
                </Link>
              </div>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
