import { Link } from "react-router-dom";
import styles from "./LandingHome.module.css";

const HERO_IMAGE_URL =
  "https://images.pexels.com/photos/6568486/pexels-photo-6568486.jpeg?cs=srgb&dl=pexels-cottonbro-6568486.jpg&fm=jpg";
const TUTOR_IMAGE_URL =
  "https://images.pexels.com/photos/7210754/pexels-photo-7210754.jpeg?cs=srgb&dl=pexels-sam-lion-7210754.jpg&fm=jpg";
const PASEADOR_IMAGE_URL =
  "https://images.pexels.com/photos/1904105/pexels-photo-1904105.jpeg?cs=srgb&dl=pexels-helena-lopes-1904105.jpg&fm=jpg";

const stats = [
  { value: "1 lugar", label: "para organizar el paseo de tu perro" },
  { value: "2 rutas", label: "claras para tutor o paseador" },
  { value: "100%", label: "pensado desde el cuidado de la mascota" }
];

const highlights = [
  {
    title: "Elige con contexto",
    text: "Conoce mejor al paseador antes de reservar y decide con más tranquilidad."
  },
  {
    title: "Ordena tu agenda",
    text: "Como paseador, mantén tus bloques y solicitudes visibles en un mismo flujo."
  },
  {
    title: "Empieza sin vueltas",
    text: "Cada entrada te lleva directo al registro correcto según el rol que necesitas."
  }
];

const benefits = [
  {
    icon: "Confianza",
    title: "Decisiones más claras",
    text: "La experiencia de inicio ayuda a entender rápido qué hacer primero para avanzar con tu perro."
  },
  {
    icon: "Agenda",
    title: "Todo más ordenado",
    text: "Las reservas, horarios y estados quedan conectados para que el flujo se sienta simple de seguir."
  },
  {
    icon: "Cuidado",
    title: "Tu mascota al centro",
    text: "La información importante del paseo aparece antes que el ruido, con foco real en el bienestar."
  }
];

const roles = [
  {
    id: "tutor",
    eyebrow: "Para tutores",
    title: "Encuentra un paseo confiable para tu perro",
    text: "Crea tu cuenta, registra a tu mascota y prepárate para elegir al paseador con mayor tranquilidad.",
    imageUrl: TUTOR_IMAGE_URL,
    imageAlt: "Tutor acariciando a su perro",
    bullets: [
      "Registra la información clave de tu mascota",
      "Busca paseadores según cercanía y disponibilidad",
      "Sigue el estado del paseo desde tu panel"
    ],
    primaryLabel: "Empezar como tutor",
    primaryTo: "/register/tutor",
    secondaryLabel: "Ya tengo cuenta",
    secondaryTo: "/login/tutor"
  },
  {
    id: "paseador",
    eyebrow: "Para paseadores",
    title: "Organiza tu servicio y responde más rápido",
    text: "Activa tu perfil profesional, administra tu agenda y responde solicitudes desde un panel claro.",
    imageUrl: PASEADOR_IMAGE_URL,
    imageAlt: "Paseador caminando con un perro",
    bullets: [
      "Configura tu disponibilidad y tus tarifas",
      "Gestiona solicitudes pendientes con mayor orden",
      "Mantén una presentación profesional de tu servicio"
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
    text: "Ahora entiendo mucho más rápido por dónde comenzar cuando quiero buscar un paseo para mi perro."
  },
  {
    name: "Javier M.",
    text: "Se siente más claro, más cálido y más fácil de usar desde el primer vistazo."
  },
  {
    name: "Sofía T.",
    text: "La entrada para cada rol quedó súper directa. No pierdo tiempo tratando de averiguar dónde entrar."
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
              <p className={styles.brandSubtitle}>Paseos con más claridad para tu perro</p>
            </div>
          </div>

          <nav className={styles.nav} aria-label="Navegacion principal">
            <a href="#beneficios" className={styles.navLink}>
              Beneficios
            </a>
            <a href="#roles" className={styles.navLink}>
              Roles
            </a>
            <a href="#testimonios" className={styles.navLink}>
              Testimonios
            </a>
            <div className={styles.navCtas}>
              <Link to="/register/tutor" className={styles.navButton}>
                Empezar como tutor
              </Link>
              <Link to="/register/paseador" className={styles.navSecondaryButton}>
                Empezar como paseador
              </Link>
            </div>
          </nav>
        </div>
      </header>

      <section className={styles.heroSection}>
        <div className={styles.heroGrid}>
          <div className={styles.heroCopy}>
            <span className={styles.heroTag}>Tu perro merece paseos con confianza</span>
            <h1 className={styles.heroTitle}>
              Encuentra apoyo para
              <span className={styles.heroAccent}> cuidar y pasear a tu perro</span>
            </h1>
            <p className={styles.heroText}>
              Patiperro conecta a tutores y paseadores desde una experiencia más clara,
              cercana y ordenada, para que reservar, responder y seguir un paseo se sienta
              natural desde el primer momento.
            </p>

            <div className={styles.heroActions}>
              <Link to="/register/tutor" className={styles.primaryCta}>
                Quiero empezar como tutor
              </Link>
              <Link to="/register/paseador" className={styles.secondaryCta}>
                Quiero ofrecer paseos
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
            <img
              src={HERO_IMAGE_URL}
              alt="Persona abrazando a un perro antes de un paseo"
              className={styles.heroImage}
            />

            <div className={styles.visualNoteTop}>
              <span>Cuidado diario</span>
              <strong>Una entrada simple para decidir mejor cada paseo</strong>
            </div>

            <div className={styles.visualNoteBottom}>
              <span>Patiperro</span>
              <strong>Empieza hoy como tutor o como paseador</strong>
            </div>
          </div>
        </div>
      </section>

      <section className={styles.highlightsSection}>
        <div className={styles.highlightsGrid}>
          {highlights.map((item) => (
            <article key={item.title} className={styles.highlightItem}>
              <h2>{item.title}</h2>
              <p>{item.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="beneficios" className={styles.benefitsSection}>
        <div className={styles.sectionIntro}>
          <p className={styles.sectionEyebrow}>Beneficios</p>
          <h2>Una forma más amable de empezar y avanzar en cada reserva</h2>
          <p>
            El inicio del flujo pone la decisión correcta al frente, con una experiencia
            más limpia para tutores y una entrada directa para paseadores.
          </p>
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
          <h2>Elige cómo quieres comenzar en Patiperro</h2>
          <p>
            Cada camino tiene su propia entrada para que no pierdas tiempo buscando dónde
            registrarte o iniciar sesión.
          </p>
        </div>

        <div className={styles.rolesGrid}>
          {roles.map((role) => (
            <article key={role.id} className={styles.roleCard}>
              <img src={role.imageUrl} alt={role.imageAlt} className={styles.roleImage} />

              <div className={styles.roleContent}>
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
              </div>
            </article>
          ))}
        </div>
      </section>

      <section id="testimonios" className={styles.testimonialsSection}>
        <div className={styles.sectionIntro}>
          <p className={styles.sectionEyebrow}>Testimonios</p>
          <h2>Una primera impresión más clara y más cercana</h2>
        </div>

        <div className={styles.testimonialsGrid}>
          {testimonials.map((item) => (
            <article key={item.name} className={styles.testimonialCard}>
              <p>{item.text}</p>
              <strong>{item.name}</strong>
            </article>
          ))}
        </div>
      </section>

      <section id="empezar" className={styles.finalSection}>
        <div className={styles.finalBand}>
          <div>
            <p className={styles.finalEyebrow}>Empieza hoy</p>
            <h2>Elige tu camino y entra directo al registro correcto</h2>
            <p>
              Ya sea para buscar ayuda con tu perro o para ofrecer paseos, puedes empezar
              desde aquí con una entrada clara para cada rol.
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
