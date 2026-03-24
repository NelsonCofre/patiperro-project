import { Navigate, Route, Routes } from "react-router-dom";
import LoginTutor from "../features/auth/pages/LoginTutor/LoginTutor";
import RegisterTutor from "../features/auth/pages/RegisterTutor/RegisterTutor";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login/tutor" />} />
      <Route path="/login/tutor" element={<LoginTutor />} />
      <Route path="/register/tutor" element={<RegisterTutor />} />
      <Route path="*" element={<Navigate to="/login/tutor" />} />
    </Routes>
  );
}
