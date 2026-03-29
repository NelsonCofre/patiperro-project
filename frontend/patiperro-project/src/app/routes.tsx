import { Navigate, Route, Routes } from "react-router-dom";
import LoginPaseador from "../features/auth/pages/LoginPaseador/LoginPaseador";
import LoginTutor from "../features/auth/pages/LoginTutor/LoginTutor";
import RegisterPaseador from "../features/auth/pages/RegisterPaseador/RegisterPaseador";
import RegisterTutor from "../features/auth/pages/RegisterTutor/RegisterTutor";
import PaseadorAgenda from "../features/paseador/pages/PaseadorAgenda/PaseadorAgenda";
import PaseadorDashboard from "../features/paseador/pages/PaseadorDashboard/PaseadorDashboard";

export default function AppRoutes() {
  return (
    <Routes>
      {/* Ruta inicial segura de la app. */}
      <Route path="/" element={<Navigate to="/login/tutor" />} />
      {/* Rutas de autenticacion por rol. */}
      <Route path="/login/paseador" element={<LoginPaseador />} />
      <Route path="/login/tutor" element={<LoginTutor />} />
      <Route path="/register/paseador" element={<RegisterPaseador />} />
      <Route path="/register/tutor" element={<RegisterTutor />} />
      {/* Primeras rutas del espacio del paseador. */}
      <Route path="/dashboard" element={<PaseadorDashboard />} />
      <Route path="/dashboard/agenda" element={<PaseadorAgenda />} />
      {/* Fallback para rutas desconocidas. */}
      <Route path="*" element={<Navigate to="/login/tutor" />} />
    </Routes>
  );
}
