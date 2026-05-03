import { Navigate, Route, Routes } from "react-router-dom";
import LoginPaseador from "../features/auth/pages/LoginPaseador/LoginPaseador";
import LoginTutor from "../features/auth/pages/LoginTutor/LoginTutor";
import RegisterPaseador from "../features/auth/pages/RegisterPaseador/RegisterPaseador";
import RegisterTutor from "../features/auth/pages/RegisterTutor/RegisterTutor";
import LandingHome from "../features/home/pages/LandingHome/LandingHome";
import AddMascota from "../features/mascota/pages/AddMascota/AddMascota";
import PaseadorAgenda from "../features/paseador/pages/PaseadorAgenda/PaseadorAgenda";
import PaseadorBilletera from "../features/paseador/pages/PaseadorBilletera/PaseadorBilletera";
import PaseadorConfiguracion from "../features/paseador/pages/PaseadorConfiguracion/PaseadorConfiguracion";
import PaseadorDashboard from "../features/paseador/pages/PaseadorDashboard/PaseadorDashboard";
import PaseadorSolicitudes from "../features/paseador/pages/PaseadorSolicitudes/PaseadorSolicitudes";
import SolicitudPaseo from "../features/tutor/pages/SolicitudPaseo/SolicitudPaseo";
import PagoReservaTutor from "../features/tutor/pages/PagoReservaTutor/PagoReservaTutor";
import TutorDashboard from "../features/tutor/pages/TutorDashboard/TutorDashboard";
import TutorReservas from "../features/tutor/pages/TutorReservas/TutorReservas";

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
      <Route path="/paseador/dashboard/solicitudes" element={<PaseadorSolicitudes />} />
      <Route path="/paseador/dashboard/solicitudes/aceptadas" element={<PaseadorSolicitudes />} />
      <Route path="/paseador/dashboard/solicitudes/en-curso" element={<PaseadorSolicitudes />} />
      <Route path="/paseador/dashboard/solicitudes/rechazadas" element={<PaseadorSolicitudes />} />
      <Route path="/paseador/dashboard/agenda" element={<PaseadorAgenda />} />
      <Route path="/paseador/dashboard/billetera" element={<PaseadorBilletera />} />
      {/* Primeras rutas del espacio del tutor. */}
      <Route path="/tutor/dashboard" element={<TutorDashboard />} />
      <Route path="/tutor/mascota/nueva" element={<AddMascota />} />
      <Route path="/tutor/solicitud-paseo" element={<SolicitudPaseo />} />
      <Route path="/tutor/pago-reserva" element={<PagoReservaTutor />} />
      <Route path="/tutor/reservas" element={<TutorReservas />} />
      {/* Fallback para rutas desconocidas. */}
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
}
