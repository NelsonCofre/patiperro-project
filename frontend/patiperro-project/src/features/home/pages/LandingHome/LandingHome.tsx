import { Link } from "react-router-dom";
import styles from "./LandingHome.module.css";

const HERO_IMAGE_URL =
  "https://images.pexels.com/photos/6568486/pexels-photo-6568486.jpeg?cs=srgb&dl=pexels-cottonbro-6568486.jpg&fm=jpg";

const stats = [
  { value: "+1", label: "forma simple de comenzar" },
  { value: "100%", label: "enfoque en tu perro" },
  { value: "1", label: "espacio para paseadores" }
];

const benefits = [
  {
    icon: "Confianza",
    title: "Encuentra ayuda con mas claridad",
    text: "Como tutor, puedes entender rapido por donde comenzar para buscar a la persona indicada para los paseos de tu perro."
  },
  {
    icon: "Mascota",
    title: "Tu perro al centro",
    text: "La portada habla del cuidado, del paseo y del bienestar de tu mascota antes que de la plataforma."
  },
  {
    icon: "Paseador",
    title: "Busca al paseador correcto",
    text: "La experiencia principal te ayuda a avanzar con calma y confianza hasta encontrar apoyo para tu mascota."
  },
  {
    icon: "Inicio",
    title: "Empieza sin rodeos",
    text: "Los botones principales te llevan directo a crear tu cuenta como tutor o, si corresponde, como paseador."
  }
];

const roles = [
  {
    id: "tutor",
    eyebrow: "Para tutores",
    title: "Encuentra un paseador para tu perro",
    text: "Empieza como tutor y avanza hacia una experiencia pensada para ayudarte a organizar la informacion de tu mascota y prepararte para encontrar el servicio indicado.",
    bullets: [
      "Crea la ficha de tu perro",
      "Ordena su informacion antes de reservar",
      "Preparate mejor para elegir con confianza"
    ],
    primaryLabel: "Empezar como tutor",
    primaryTo: "/register/tutor",
    secondaryLabel: "Ya tengo cuenta",
    secondaryTo: "/login/tutor"
  },
  {
    id: "paseador",
    eyebrow: "Para paseadores",
    title: "Empieza como paseador",
    text: "Si vienes a ofrecer tu servicio, tambien puedes empezar desde aqui y crear tu perfil como paseador.",
    bullets: [
      "Configura tarifas y horarios",
      "Organiza tu agenda y disponibilidad",
      "Presenta tu servicio de forma profesional"
    ],
    primaryLabel: "Empezar como paseador",
    primaryTo: "/register/paseador",
    secondaryLabel: "Ya tengo cuenta",
    secondaryTo: "/login/paseador"
  }
];

const testimonials = [
  {
    name: "Camila R.",
    text: "Ahora entiendo mas rapido por donde empezar si quiero encontrar un paseador para mi perro."
  },
  {
    name: "Javier M.",
    text: "La portada transmite mas confianza y pone a la mascota en el centro, que es justo lo que esperaba."
  },
  {
    name: "Sofia T.",
    text: "Se entiende rapido como empezar a buscar ayuda para mi perro, y aun asi la opcion para paseadores sigue estando clara."
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
              <p className={styles.brandSubtitle}>Encuentra un paseador con mas confianza</p>
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
            <span className={styles.heroTag}>Tu perro merece un paseo con confianza</span>
            <h1 className={styles.heroTitle}>
              Encuentra al paseador ideal para
              <span className={styles.heroAccent}> tu perro</span>
            </h1>
            <p className={styles.heroText}>
              Patiperro te ayuda a empezar como tutor con una experiencia clara, pensada
              para organizar la informacion de tu mascota y prepararte para elegir a la
              persona correcta para sus paseos.
            </p>

            <div className={styles.heroActions}>
              <Link to="/register/tutor" className={styles.primaryCta}>
                Encontrar un paseador
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
                Tu perro primero
              </div>

              <div className={styles.imageBadgeRight}>
                <p>Patiperro</p>
                <strong>Empieza como tutor y busca el paseo ideal para tu mascota</strong>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="beneficios" className={styles.benefitsSection}>
        <div className={styles.sectionIntro}>
          <p className={styles.sectionEyebrow}>Beneficios</p>
          <h2>Una experiencia pensada para quienes buscan cuidado y paseos con confianza</h2>
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
          <h2>Elige como quieres comenzar en Patiperro</h2>
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
            <h2>Una primera impresion mas cercana para quienes buscan cuidado</h2>
          </div>

          <div className={styles.testimonialsGrid}>
            {testimonials.map((item) => (
              <article key={item.name} className={styles.testimonialCard}>
                <p>"{item.text}"</p>
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
            <h2>Empieza hoy a buscar el mejor paseo para tu perro</h2>
            <p>
              Comienza como tutor y da el primer paso para organizar la informacion de tu
              mascota y acercarte al paseador indicado.
            </p>
          </div>

          <div className={styles.finalActions}>
            <Link to="/register/tutor" className={styles.primaryCta}>
              Empezar como tutor
            </Link>
            <Link to="/register/paseador" className={styles.secondaryCta}>
              Empezar como paseador
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}
