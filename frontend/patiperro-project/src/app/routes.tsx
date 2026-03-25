import { Navigate, Route, Routes } from "react-router-dom";
import LoginPaseador from "../features/auth/pages/LoginPaseador/LoginPaseador";
import LoginTutor from "../features/auth/pages/LoginTutor/LoginTutor";
import RegisterPaseador from "../features/auth/pages/RegisterPaseador/RegisterPaseador";
import RegisterTutor from "../features/auth/pages/RegisterTutor/RegisterTutor";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login/tutor" />} />
      <Route path="/login/paseador" element={<LoginPaseador />} />
      <Route path="/login/tutor" element={<LoginTutor />} />
      <Route path="/register/paseador" element={<RegisterPaseador />} />
      <Route path="/register/tutor" element={<RegisterTutor />} />
      <Route path="*" element={<Navigate to="/login/tutor" />} />
    </Routes>
  );
}
