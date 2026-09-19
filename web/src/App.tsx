import { Navigate, Route, Routes } from "react-router-dom";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { SecurityConsolePage } from "./pages/SecurityConsolePage";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/app" element={<DashboardPage />} />
      <Route path="/security" element={<SecurityConsolePage />} />
    </Routes>
  );
}
