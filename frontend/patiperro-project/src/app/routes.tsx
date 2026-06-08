import type { ReactElement } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { ACCESS_TOKEN_SESSION_KEY, PASEADOR_ID_SESSION_KEY, TUTOR_ID_SESSION_KEY } from "../config/api";
import LoginPaseador from "../features/auth/pages/LoginPaseador/LoginPaseador";
import LoginTutor from "../features/auth/pages/LoginTutor/LoginTutor";
import RegisterPaseador from "../features/auth/pages/RegisterPaseador/RegisterPaseador";
import RegisterTutor from "../features/auth/pages/RegisterTutor/RegisterTutor";
import ChatReservaPage from "../features/chat/pages/ChatReservaPage/ChatReservaPage";
import LandingHome from "../features/home/pages/LandingHome/LandingHome";
import CheckoutProSandboxPage from "../features/labs/pages/CheckoutProSandboxPage/CheckoutProSandboxPage";
import AddMascota from "../features/mascota/pages/AddMascota/AddMascota";
import MisMascotas from "../features/mascota/pages/MisMascotas/MisMascotas";
import PaseadorAgenda from "../features/paseador/pages/PaseadorAgenda/PaseadorAgenda";
import PaseadorBilletera from "../features/paseador/pages/PaseadorBilletera/PaseadorBilletera";
import PaseadorConfiguracion from "../features/paseador/pages/PaseadorConfiguracion/PaseadorConfiguracion";
import PaseadorVerificacion from "../features/paseador/pages/PaseadorVerificacion/PaseadorVerificacion";
import PaseadorDashboard from "../features/paseador/pages/PaseadorDashboard/PaseadorDashboard";
import PaseadorSolicitudes from "../features/paseador/pages/PaseadorSolicitudes/PaseadorSolicitudes";
import SolicitudPaseo from "../features/tutor/pages/SolicitudPaseo/SolicitudPaseo";
import PagoReservaTutor from "../features/tutor/pages/PagoReservaTutor/PagoReservaTutor";
import TutorCheckoutRetornoPage from "../features/tutor/pages/TutorCheckoutRetorno/TutorCheckoutRetornoPage";
import TutorDashboard from "../features/tutor/pages/TutorDashboard/TutorDashboard";
import TutorReservas from "../features/tutor/pages/TutorReservas/TutorReservas";
import MiPerfilCuenta from "../features/shared/pages/MiPerfilCuenta/MiPerfilCuenta";

function RequireTutorAuth({ children }: { children: ReactElement }) {
  const location = useLocation();
  const accessToken = sessionStorage.getItem(ACCESS_TOKEN_SESSION_KEY)?.trim();
  const tutorId = sessionStorage.getItem(TUTOR_ID_SESSION_KEY)?.trim();

  if (!accessToken || !tutorId) {
    return <Navigate to="/login/tutor" replace state={{ from: location }} />;
  }

  return children;
}

function RequireChatAuth({ children }: { children: ReactElement }) {
  const location = useLocation();
  const accessToken = sessionStorage.getItem(ACCESS_TOKEN_SESSION_KEY)?.trim();
  const tutorId = sessionStorage.getItem(TUTOR_ID_SESSION_KEY)?.trim();
  const paseadorId = sessionStorage.getItem(PASEADOR_ID_SESSION_KEY)?.trim();

  if (!accessToken || (!tutorId && !paseadorId)) {
    return <Navigate to="/" replace state={{ from: location }} />;
  }

  return children;
}

function RequirePaseadorAuth({ children }: { children: ReactElement }) {
  const location = useLocation();
  const accessToken = sessionStorage.getItem(ACCESS_TOKEN_SESSION_KEY)?.trim();
  const paseadorId = sessionStorage.getItem(PASEADOR_ID_SESSION_KEY)?.trim();

  if (!accessToken || !paseadorId) {
    return <Navigate to="/login/paseador" replace state={{ from: location }} />;
  }

  return children;
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* Ruta inicial segura de la app. */}
      <Route path="/" element={<LandingHome />} />
      {/* Rutas de autenticacion por rol. */}
      <Route path="/login/paseador" element={<LoginPaseador />} />
      <Route path="/register/paseador" element={<RegisterPaseador />} />
      <Route path="/login/tutor" element={<LoginTutor />} />
      <Route path="/register/tutor" element={<RegisterTutor />} />
      {/* Primeras rutas del espacio del paseador. */}
      <Route path="/paseador/dashboard" element={<PaseadorDashboard />} />
      <Route
        path="/paseador/dashboard/configuracion"
        element={<PaseadorConfiguracion />}
      />
      <Route
        path="/paseador/dashboard/verificacion"
        element={<PaseadorVerificacion />}
      />
      <Route path="/paseador/dashboard/solicitudes" element={<PaseadorSolicitudes />} />
      <Route path="/paseador/dashboard/solicitudes/aceptadas" element={<PaseadorSolicitudes />} />
      <Route path="/paseador/dashboard/solicitudes/en-curso" element={<PaseadorSolicitudes />} />
      <Route path="/paseador/dashboard/solicitudes/finalizadas" element={<PaseadorSolicitudes />} />
      <Route path="/paseador/dashboard/solicitudes/rechazadas" element={<PaseadorSolicitudes />} />
      <Route path="/paseador/dashboard/solicitudes/expiradas" element={<PaseadorSolicitudes />} />
      <Route path="/paseador/dashboard/agenda" element={<PaseadorAgenda />} />
      <Route path="/paseador/dashboard/billetera" element={<PaseadorBilletera />} />
      <Route
        path="/paseador/dashboard/perfil"
        element={
          <RequirePaseadorAuth>
            <MiPerfilCuenta role="paseador" />
          </RequirePaseadorAuth>
        }
      />
      {/* Primeras rutas del espacio del tutor. */}
      <Route
        path="/tutor/dashboard"
        element={
          <RequireTutorAuth>
            <TutorDashboard />
          </RequireTutorAuth>
        }
      />
      <Route
        path="/tutor/mascotas"
        element={
          <RequireTutorAuth>
            <MisMascotas />
          </RequireTutorAuth>
        }
      />
      <Route
        path="/tutor/mascota/nueva"
        element={
          <RequireTutorAuth>
            <AddMascota />
          </RequireTutorAuth>
        }
      />
      <Route
        path="/tutor/mascotas/:idMascota/editar"
        element={
          <RequireTutorAuth>
            <AddMascota />
          </RequireTutorAuth>
        }
      />
      <Route
        path="/tutor/solicitud-paseo"
        element={
          <RequireTutorAuth>
            <SolicitudPaseo />
          </RequireTutorAuth>
        }
      />
      <Route
        path="/tutor/pago-reserva"
        element={
          <RequireTutorAuth>
            <PagoReservaTutor />
          </RequireTutorAuth>
        }
      />
      <Route
        path="/tutor/reservas/pago/exito"
        element={<TutorCheckoutRetornoPage tipo="success" />}
      />
      <Route
        path="/tutor/reservas/pago/error"
        element={<TutorCheckoutRetornoPage tipo="failure" />}
      />
      <Route
        path="/tutor/reservas/pago/pendiente"
        element={<TutorCheckoutRetornoPage tipo="pending" />}
      />
      <Route
        path="/tutor/perfil"
        element={
          <RequireTutorAuth>
            <MiPerfilCuenta role="tutor" />
          </RequireTutorAuth>
        }
      />
      <Route
        path="/tutor/reservas"
        element={
          <RequireTutorAuth>
            <TutorReservas />
          </RequireTutorAuth>
        }
      />
      <Route
        path="/chat/reserva/:idReserva"
        element={
          <RequireChatAuth>
            <ChatReservaPage />
          </RequireChatAuth>
        }
      />
      {/* Modulo aislado de pruebas Checkout Pro (MVP sandbox). */}
      <Route path="/labs/checkout-pro" element={<CheckoutProSandboxPage />} />
      {/* Fallback para rutas desconocidas. */}
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
}
