import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import AuthPage from './pages/AuthPage'
import DashboardPage from './pages/DashboardPage'
import PatientsPage from './pages/PatientsPage'
import NewPatientPage from './pages/NewPatientPage'
import PatientDetailPage from './pages/PatientDetailPage'
import NewAnalysisPage from './pages/NewAnalysisPage'
import AnalysisDetailPage from './pages/AnalysisDetailPage'
import AdminPage from './pages/AdminPage'
import AuditPage from './pages/AuditPage'
import Layout from './components/Layout'
import EditPatientPage from './pages/EditPatientPage'
import { getRole, isLoggedIn } from './api/client'

function RequireAuth({ children }: { children: ReactNode }) {
  return isLoggedIn() ? <>{children}</> : <Navigate to="/login" replace />
}

function RequireAdmin({ children }: { children: ReactNode }) {
  return getRole() === 'ADMIN' ? <>{children}</> : <Navigate to="/" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<AuthPage mode="login" />} />
        <Route path="/register" element={<AuthPage mode="register" />} />
        <Route element={<RequireAuth><Layout /></RequireAuth>}>
          <Route index element={<DashboardPage />} />
          <Route path="patients" element={<PatientsPage />} />
          <Route path="patients/new" element={<NewPatientPage />} />
          <Route path="patients/:id" element={<PatientDetailPage />} />
          <Route path="patients/:id/analyse" element={<NewAnalysisPage />} />
          <Route path="analyses/:id" element={<AnalysisDetailPage />} />
          <Route path="admin" element={<RequireAdmin><AdminPage /></RequireAdmin>} />
          <Route path="admin/audit" element={<RequireAdmin><AuditPage /></RequireAdmin>} />
          <Route path="patients/:id/edit" element={<EditPatientPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}