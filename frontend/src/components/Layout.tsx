import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { logout } from '../api/client'
import { getRole } from '../api/client'

export default function Layout() {
  const nav = useNavigate()
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">Pneumonie<br /><small>Aide à la décision</small></div>
        <nav>
          <NavLink to="/" end>Tableau de bord</NavLink>
          <NavLink to="/patients" end>Patients</NavLink>
          <NavLink to="/patients/new">Nouveau patient</NavLink>
          {getRole() === 'ADMIN' && <NavLink to="/admin">Comptes</NavLink>}
          <NavLink to="/admin/audit">Journal d'audit</NavLink>
        </nav>
        <button className="link" onClick={async () => { await logout(); nav('/login') }}>Se déconnecter</button>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}