// Navbar del espacio del paseador.
// Usa NavLink para resaltar automaticamente la opcion activa.
import { NavLink, useNavigate } from "react-router-dom";
import { clearAuthSession } from "../../../auth/services/authServices";
import styles from "./PaseadorNavbar.module.css";

const NAV_ITEMS = [
  { label: "Inicio", to: "/paseador/dashboard", end: true },
  { label: "Solicitudes", to: "/paseador/dashboard/solicitudes" },
  { label: "Configurar mi Servicio", to: "/paseador/dashboard/configuracion" },
  { label: "Mi Agenda", to: "/paseador/dashboard/agenda" }
];

export default function PaseadorNavbar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    clearAuthSession();
    navigate("/login/paseador", { replace: true });
  };

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
          <button type="button" className={styles.logoutButton} onClick={handleLogout}>
            Cerrar sesion
          </button>
        </nav>
      </div>
    </header>
  );
}
