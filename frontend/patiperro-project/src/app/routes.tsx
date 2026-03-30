import { Navigate, Route, Routes } from "react-router-dom";
import LoginPaseador from "../features/auth/pages/LoginPaseador/LoginPaseador";
import LoginTutor from "../features/auth/pages/LoginTutor/LoginTutor";
import RegisterPaseador from "../features/auth/pages/RegisterPaseador/RegisterPaseador";
import RegisterTutor from "../features/auth/pages/RegisterTutor/RegisterTutor";
import AddMascota from "../features/mascota/pages/AddMascota/AddMascota";
import PaseadorAgenda from "../features/paseador/pages/PaseadorAgenda/PaseadorAgenda";
import PaseadorDashboard from "../features/paseador/pages/PaseadorDashboard/PaseadorDashboard";
import TutorDashboard from "../features/tutor/pages/TutorDashboard/TutorDashboard";

export default function AppRoutes() {
  return (
    <Routes>
      {/* Ruta inicial segura de la app. */}
      <Route path="/" element={<Navigate to="/login/tutor" />} />
      {/* Rutas de autenticacion por rol. */}
      <Route path="/login/paseador" element={<LoginPaseador />} />
      <Route path="/register/paseador" element={<RegisterPaseador />} />
      <Route path="/login/tutor" element={<LoginTutor />} />
      <Route path="/register/tutor" element={<RegisterTutor />} />
      {/* Primeras rutas del espacio del paseador. */}
      <Route path="/paseador/dashboard" element={<PaseadorDashboard />} />
      <Route path="/paseador/dashboard/agenda" element={<PaseadorAgenda />} />
      {/* Primeras rutas del espacio del tutor. */}
      <Route path="/tutor/dashboard" element={<TutorDashboard />} />
      <Route path="/tutor/mascota/nueva" element={<AddMascota />} />
      {/* Fallback para rutas desconocidas. */}
      <Route path="*" element={<Navigate to="/login/tutor" />} />
    </Routes>
  );
}
