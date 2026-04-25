// Navbar del espacio del paseador.
// Usa NavLink para resaltar automaticamente la opcion activa.
import { useEffect, useMemo, useState } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { clearAuthSession } from "../../../auth/services/authServices";
import { fetchSolicitudesPendientesPaseador } from "../../services/solicitudesPaseadorService";
import styles from "./PaseadorNavbar.module.css";

const NAV_ITEMS = [
  { label: "Inicio", to: "/paseador/dashboard", end: true },
  { label: "Solicitudes", to: "/paseador/dashboard/solicitudes" },
  { label: "Configurar mi Servicio", to: "/paseador/dashboard/configuracion" },
  { label: "Mi Agenda", to: "/paseador/dashboard/agenda" }
];

const PASEADOR_SOLICITUDES_REVIEWED_AT_KEY = "patiperro_paseador_solicitudes_reviewed_at";

export default function PaseadorNavbar() {
  const location = useLocation();
  const navigate = useNavigate();
  const [pendingCount, setPendingCount] = useState(0);
  const [lastPendingUpdate, setLastPendingUpdate] = useState<number | null>(null);

  const isSolicitudesView = location.pathname.startsWith("/paseador/dashboard/solicitudes");

  const handleLogout = () => {
    clearAuthSession();
    navigate("/login/paseador", { replace: true });
  };

  useEffect(() => {
    let active = true;

    async function loadPendingCount() {
      try {
        const solicitudes = await fetchSolicitudesPendientesPaseador();
        if (!active) return;
        setPendingCount(solicitudes.filter((solicitud) => solicitud.estado === "Solicitada").length);
        setLastPendingUpdate(Date.now());
      } catch {
        if (!active) return;
        setPendingCount(0);
      }
    }

    void loadPendingCount();
    const intervalId = window.setInterval(() => {
      void loadPendingCount();
    }, 15000);

    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, []);

  useEffect(() => {
    if (!isSolicitudesView) return;
    sessionStorage.setItem(PASEADOR_SOLICITUDES_REVIEWED_AT_KEY, String(Date.now()));
  }, [isSolicitudesView, pendingCount]);

  const shouldShowBadge = useMemo(() => {
    if (pendingCount <= 0) return false;
    if (isSolicitudesView) return false;

    const reviewedAtRaw = sessionStorage.getItem(PASEADOR_SOLICITUDES_REVIEWED_AT_KEY);
    const reviewedAt = reviewedAtRaw ? Number(reviewedAtRaw) : 0;
    if (!Number.isFinite(reviewedAt)) return true;
    if (lastPendingUpdate == null) return true;
    return reviewedAt < lastPendingUpdate;
  }, [isSolicitudesView, lastPendingUpdate, pendingCount]);

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
              {item.label === "Solicitudes" ? (
                <span className={styles.linkWithBadge}>
                  <span>{item.label}</span>
                  <span className={styles.bellWrapper} aria-hidden="true">
                    <svg
                      className={styles.bellIcon}
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.9"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M15 17h5l-1.4-1.4a2 2 0 0 1-.6-1.4V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5" />
                      <path d="M9.75 20a2.25 2.25 0 0 0 4.5 0" />
                    </svg>
                    {shouldShowBadge ? (
                      <span className={styles.badge} aria-label={`${pendingCount} solicitudes pendientes`}>
                        {pendingCount > 99 ? "99+" : pendingCount}
                      </span>
                    ) : null}
                  </span>
                </span>
              ) : (
                item.label
              )}
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
