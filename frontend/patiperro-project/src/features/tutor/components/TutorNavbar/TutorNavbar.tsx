// Navbar del espacio del tutor.
// Ofrece navegacion directa al home y al flujo de creacion de mascota.
import { NavLink } from "react-router-dom";
import styles from "./TutorNavbar.module.css";

const NAV_ITEMS = [
  { label: "Inicio", to: "/tutor/dashboard" },
  { label: "Anadir Mascota", to: "/tutor/mascota/nueva" }
];

export default function TutorNavbar() {
  return (
    <nav className={styles.nav} aria-label="Navegacion del tutor">
      <div className={styles.brandBlock}>
        <span className={styles.brandEyebrow}>Panel del Tutor</span>
        <strong className={styles.brandTitle}>Patiperro Home</strong>
      </div>

      <div className={styles.links}>
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `${styles.link} ${isActive ? styles.linkActive : ""}`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
