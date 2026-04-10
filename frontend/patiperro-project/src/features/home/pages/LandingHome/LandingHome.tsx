import { Link } from "react-router-dom";
import styles from "./LandingHome.module.css";

const HERO_IMAGE_URL =
  "https://images.pexels.com/photos/6568486/pexels-photo-6568486.jpeg?cs=srgb&dl=pexels-cottonbro-6568486.jpg&fm=jpg";

const stats = [
  { value: "+2", label: "roles disponibles" },
  { value: "100%", label: "entrada guiada" },
  { value: "1", label: "home publico" }
];

const benefits = [
  {
    icon: "Agenda",
    title: "Organizacion clara",
    text: "Tutor y paseador aterrizan en un flujo mas entendible desde el primer vistazo."
  },
  {
    icon: "Confianza",
    title: "Informacion mejor presentada",
    text: "La portada transmite cuidado, orden y una experiencia mas profesional."
  },
  {
    icon: "Roles",
    title: "Decision por rol",
    text: "Cada visitante entiende rapido si debe continuar como tutor o como paseador."
  },
  {
    icon: "Inicio",
    title: "Accion directa",
    text: "Los llamados a la accion apuntan a registro e inicio de sesion sin friccion."
  }
];

const roles = [
  {
    id: "tutor",
    eyebrow: "Para tutores",
    title: "Empieza como tutor",
    text: "Registra a tu mascota, organiza su informacion y preparate para una experiencia mas clara desde tu dashboard.",
    bullets: [
      "Crea la ficha de tu mascota",
      "Ordena mejor la informacion inicial",
      "Avanza con mas contexto al flujo de reserva"
    ],
    primaryLabel: "Crear cuenta de tutor",
    primaryTo: "/register/tutor",
    secondaryLabel: "Ya tengo cuenta",
    secondaryTo: "/login/tutor"
  },
  {
    id: "paseador",
    eyebrow: "Para paseadores",
    title: "Empieza como paseador",
    text: "Configura tarifas, agenda y disponibilidad desde una portada que ya presenta mejor tu servicio.",
    bullets: [
      "Define tarifas y horarios",
      "Gestiona bloqueos y agenda",
      "Presenta tu servicio con mas confianza"
    ],
    primaryLabel: "Crear cuenta de paseador",
    primaryTo: "/register/paseador",
    secondaryLabel: "Ya tengo cuenta",
    secondaryTo: "/login/paseador"
  }
];

const testimonials = [
  {
    name: "Camila R.",
    text: "La entrada se siente mucho mas clara. En un vistazo entendí si debía seguir como tutora."
  },
  {
    name: "Javier M.",
    text: "La parte visual transmite más confianza y la decisión entre roles se siente natural."
  },
  {
    name: "Sofia T.",
    text: "Ahora el home se ve más profesional y ayuda a entender mejor qué ofrece la plataforma."
  }
];

export default function LandingHome() {
  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.brandBlock}>
            <div className={styles.brandIcon}>P</div>
            <div>
              <p className={styles.brandTitle}>Patiperro</p>
              <p className={styles.brandSubtitle}>Paseos con mas contexto y confianza</p>
            </div>
          </div>

          <nav className={styles.nav}>
            <a href="#beneficios" className={styles.navLink}>
              Beneficios
            </a>
            <a href="#roles" className={styles.navLink}>
              Roles
            </a>
            <a href="#testimonios" className={styles.navLink}>
              Testimonios
            </a>
            <a href="#empezar" className={styles.navButton}>
              Empezar ahora
            </a>
          </nav>
        </div>
      </header>

      <section className={styles.heroSection}>
        <div className={styles.heroGlow} />

        <div className={styles.heroGrid}>
          <div className={styles.heroCopy}>
            <span className={styles.heroTag}>Patiperro te orienta desde el primer clic</span>
            <h1 className={styles.heroTitle}>
              Un landing mas claro para empezar como
              <span className={styles.heroAccent}> tutor o paseador</span>
            </h1>
            <p className={styles.heroText}>
              Diseñado para que cualquier persona que llegue al sitio entienda rapido
              qué puede hacer en la plataforma y hacia qué flujo debe avanzar.
            </p>

            <div className={styles.heroActions}>
              <Link to="/register/tutor" className={styles.primaryCta}>
                Empezar como tutor
              </Link>
              <Link to="/register/paseador" className={styles.secondaryCta}>
                Empezar como paseador
              </Link>
            </div>

            <div className={styles.statsGrid}>
              {stats.map((item) => (
                <article key={item.label} className={styles.statCard}>
                  <strong>{item.value}</strong>
                  <span>{item.label}</span>
                </article>
              ))}
            </div>
          </div>

          <div className={styles.heroVisual}>
            <div className={styles.imageShell}>
              <img
                src={HERO_IMAGE_URL}
                alt="Cuidadora abrazando a un perro"
                className={styles.heroImage}
              />

              <div className={styles.imageBadgeLeft}>
                Comunidad que cuida con carino
              </div>

              <div className={styles.imageBadgeRight}>
                <p>Home principal</p>
                <strong>Decision clara desde el primer vistazo</strong>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="beneficios" className={styles.benefitsSection}>
        <div className={styles.sectionIntro}>
          <p className={styles.sectionEyebrow}>Beneficios</p>
          <h2>Una portada mas comercial, mas limpia y mejor orientada</h2>
        </div>

        <div className={styles.benefitsGrid}>
          {benefits.map((item) => (
            <article key={item.title} className={styles.benefitCard}>
              <div className={styles.benefitIcon}>{item.icon}</div>
              <h3>{item.title}</h3>
              <p>{item.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="roles" className={styles.rolesSection}>
        <div className={styles.sectionIntro}>
          <p className={styles.sectionEyebrow}>Roles</p>
          <h2>Dos caminos claros dentro de la misma plataforma</h2>
        </div>

        <div className={styles.rolesGrid}>
          {roles.map((role) => (
            <article key={role.id} className={styles.roleCard}>
              <p className={styles.roleEyebrow}>{role.eyebrow}</p>
              <h3>{role.title}</h3>
              <p>{role.text}</p>

              <div className={styles.roleBulletList}>
                {role.bullets.map((bullet) => (
                  <span key={bullet} className={styles.roleBullet}>
                    {bullet}
                  </span>
                ))}
              </div>

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

      <section id="testimonios" className={styles.testimonialsSection}>
        <div className={styles.testimonialsShell}>
          <div className={styles.sectionIntro}>
            <p className={styles.sectionEyebrow}>Testimonios</p>
            <h2>Lo que transmite esta nueva primera impresion</h2>
          </div>

          <div className={styles.testimonialsGrid}>
            {testimonials.map((item) => (
              <article key={item.name} className={styles.testimonialCard}>
                <p>“{item.text}”</p>
                <strong>{item.name}</strong>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section id="empezar" className={styles.finalSection}>
        <div className={styles.finalCard}>
          <div>
            <p className={styles.sectionEyebrow}>Empieza hoy</p>
            <h2>Haz que la primera decision en Patiperro sea simple</h2>
            <p>
              El landing ahora se enfoca en orientar, transmitir confianza y llevar a
              cada persona al flujo correcto sin friccion.
            </p>
          </div>

          <div className={styles.finalActions}>
            <Link to="/register/tutor" className={styles.primaryCta}>
              Crear cuenta de tutor
            </Link>
            <Link to="/register/paseador" className={styles.secondaryCta}>
              Crear cuenta de paseador
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}
