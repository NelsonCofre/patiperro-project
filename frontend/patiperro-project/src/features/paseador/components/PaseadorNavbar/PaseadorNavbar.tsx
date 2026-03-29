// Navbar del espacio del paseador.
// Usa NavLink para resaltar automaticamente la opcion activa.
import { NavLink } from "react-router-dom";
import styles from "./PaseadorNavbar.module.css";

const NAV_ITEMS = [
  { label: "Inicio", to: "/dashboard" },
  { label: "Configurar mi Servicio", to: "/dashboard/configuracion" },
  { label: "Mi Agenda", to: "/dashboard/agenda" }
];

export default function PaseadorNavbar() {
  return (
    <nav className={styles.nav} aria-label="Navegacion del paseador">
      <div className={styles.brandBlock}>
        <span className={styles.brandEyebrow}>Panel del Paseador</span>
        <strong className={styles.brandTitle}>Patiperro Pro</strong>
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
