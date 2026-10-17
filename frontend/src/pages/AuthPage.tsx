import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login, register } from '../api/client'

export default function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const nav = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [busy, setBusy] = useState(false)
  const isLogin = mode === 'login'

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      if (isLogin) {
        await login(email, password)
        nav('/', { replace: true })
      } else {
        await register(name, email, password)
        setDone(true)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur inconnue')
    } finally {
      setBusy(false)
    }
  }

  if (done) {
    return (
      <main className="card">
        <h1>Compte créé</h1>
        <p>Votre compte doit être validé par un administrateur avant de pouvoir vous connecter.</p>
        <p><Link to="/login">Retour à la connexion</Link></p>
      </main>
    )
  }

  return (
    <main className="card">
      <h1>{isLogin ? 'Connexion' : 'Créer un compte'}</h1>
      <form onSubmit={onSubmit}>
        {!isLogin && (
          <label>
            Nom
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </label>
        )}
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Mot de passe
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
            required minLength={isLogin ? 1 : 10} />
        </label>
        {!isLogin && (
          <p className="muted">10 caractères minimum, avec une lettre, un chiffre et un caractère spécial.</p>
        )}
        {error && <p className="error" role="alert">{error}</p>}
        <button disabled={busy}>{busy ? 'Patientez…' : isLogin ? 'Se connecter' : "S'inscrire"}</button>
      </form>
      <p>
        {isLogin ? (
          <>Pas de compte ? <Link to="/register">Créer un compte</Link></>
        ) : (
          <>Déjà inscrit ? <Link to="/login">Se connecter</Link></>
        )}
      </p>
    </main>
  )
}