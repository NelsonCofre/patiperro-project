// Navbar del espacio del paseador.
// Usa NavLink para resaltar automaticamente la opcion activa.
import { NavLink } from "react-router-dom";
import styles from "./PaseadorNavbar.module.css";

const NAV_ITEMS = [
  { label: "Inicio", to: "/paseador/dashboard", end: true },
  { label: "Configurar mi Servicio", to: "/paseador/dashboard/configuracion" },
  { label: "Mi Agenda", to: "/paseador/dashboard/agenda" }
];

export default function PaseadorNavbar() {
  return (
    <header className={styles.header}>
      <div className={styles.headerInner}>
        <div className={styles.brandBlock}>
          <div className={styles.brandIcon}>P</div>
          <div>
            <strong className={styles.brandTitle}>Patiperro Pro</strong>
            <span className={styles.brandSubtitle}>Panel del paseador</span>
          </div>
        </div>

        <nav className={styles.nav} aria-label="Navegacion del paseador">
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
