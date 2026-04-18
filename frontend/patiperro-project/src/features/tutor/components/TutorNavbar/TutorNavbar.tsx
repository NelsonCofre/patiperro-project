// Navbar del espacio del tutor.
// Ofrece navegacion directa al home y al flujo de creacion de mascota.
import { NavLink } from "react-router-dom";
import styles from "./TutorNavbar.module.css";

const NAV_ITEMS = [
  { label: "Inicio", to: "/tutor/dashboard", end: true },
  { label: "Mis Reservas", to: "/tutor/reservas" },
  { label: "Anadir Mascota", to: "/tutor/mascota/nueva" }
];

export default function TutorNavbar() {
  return (
    <header className={styles.header}>
      <div className={styles.headerInner}>
        <div className={styles.brandBlock}>
          <div className={styles.brandIcon}>P</div>
          <div>
            <strong className={styles.brandTitle}>Patiperro</strong>
            <span className={styles.brandSubtitle}>Home del tutor</span>
          </div>
        </div>

        <nav className={styles.nav} aria-label="Navegacion del tutor">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `${styles.link} ${isActive ? styles.linkActive : ""}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  );
}
