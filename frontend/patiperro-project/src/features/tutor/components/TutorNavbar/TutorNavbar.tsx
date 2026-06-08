// Navbar del espacio del tutor.
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { useChatUnread } from "../../../chat/context/ChatUnreadContext";
import { clearAuthSession } from "../../../auth/services/authServices";
import styles from "./TutorNavbar.module.css";

type NavItem = {
  label: string;
  to: string;
  end?: boolean;
  /** Marca activo en subrutas (p. ej. alta/edición de mascota). */
  activePrefix?: string;
};

const NAV_ITEMS: NavItem[] = [
  { label: "Inicio", to: "/tutor/dashboard", end: true },
  { label: "Reservas", to: "/tutor/reservas" },
  { label: "Mis mascotas", to: "/tutor/mascotas", activePrefix: "/tutor/mascota" },
  { label: "Perfil", to: "/tutor/perfil" }
];

export default function TutorNavbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { unreadCount } = useChatUnread();

  const handleLogout = () => {
    clearAuthSession();
    navigate("/login/tutor", { replace: true });
  };

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
              className={({ isActive }) => {
                const active =
                  isActive ||
                  (item.activePrefix != null && location.pathname.startsWith(item.activePrefix));
                return `${styles.link} ${active ? styles.linkActive : ""}`;
              }}
            >
              {item.label === "Reservas" && unreadCount > 0 ? (
                <span className={styles.linkWithBadge}>
                  <span>{item.label}</span>
                  <span className={styles.chatBadge} aria-label={`${unreadCount} mensajes de chat sin leer`}>
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </span>
                </span>
              ) : (
                item.label
              )}
            </NavLink>
          ))}
          <button type="button" className={styles.logoutButton} onClick={handleLogout}>
            Cerrar sesión
          </button>
        </nav>
      </div>
    </header>
  );
}
