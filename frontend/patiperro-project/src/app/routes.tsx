import { Routes, Route } from "react-router-dom";
import Login from "../features/auth/pages/Login/Login";
import Register from "../features/auth/pages/Register/Register";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/register" element={<Register />} />
    </Routes>
  );
}