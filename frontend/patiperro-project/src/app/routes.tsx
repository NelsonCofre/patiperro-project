import { Routes, Route, Navigate } from "react-router-dom";
import Login from "../features/auth/pages/Login/Login";
import Register from "../features/auth/pages/Register/Register";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to={"/Login"}/>} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
    </Routes>
  );
}