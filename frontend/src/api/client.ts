const API_URL = import.meta.env.VITE_API_URL as string
const ROLE_KEY = 'role'

// Le jeton est dans un cookie HttpOnly : JavaScript ne peut ni le lire ni le voler.
// On ne garde que le rôle, pour l'affichage. La sécurité réelle est côté serveur.
export const isLoggedIn = (): boolean => localStorage.getItem(ROLE_KEY) !== null
export const getRole = (): string | null => localStorage.getItem(ROLE_KEY)
export const clearToken = (): void => {
  localStorage.removeItem(ROLE_KEY)
  localStorage.removeItem('token') // ancien stockage, à nettoyer
}

export class ApiError extends Error {
  status: number
  retryAfter?: number
  constructor(status: number, message: string, retryAfter?: number) {
    super(message)
    this.status = status
    this.retryAfter = retryAfter
  }
}

async function serverMessage(res: Response): Promise<string | null> {
  try {
    const data = await res.clone().json()
    return data?.message ?? data?.error ?? null
  } catch {
    try {
      const txt = (await res.text()).trim()
      return txt && txt.length < 200 ? txt : null
    } catch {
      return null
    }
  }
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let res: Response
  try {
    res = await fetch(`${API_URL}${path}`, { ...init, credentials: 'include' })
  } catch {
    throw new ApiError(0, 'Serveur injoignable. Vérifiez que le backend est démarré.')
  }

  if (!res.ok) {
    const msg = await serverMessage(res)
    const isAuthRoute = path.startsWith('/api/auth')
    if (res.status === 401 && !isAuthRoute) {
      clearToken()
      throw new ApiError(401, 'Session expirée. Veuillez vous reconnecter.')
    }
    if (res.status === 401) throw new ApiError(401, msg ?? 'Identifiants invalides')
    if (res.status === 415)
      throw new ApiError(415, 'Format de fichier non pris en charge. Utilisez une image JPEG ou PNG.')
    if (res.status === 429) {
      const ra = Number(res.headers.get('Retry-After'))
      const wait = Number.isFinite(ra) && ra > 0 ? Math.ceil(ra / 60) : null
      throw new ApiError(
        429,
        wait ? `Trop de tentatives. Réessayez dans ${wait} minute(s).` : 'Trop de tentatives. Réessayez plus tard.',
        Number.isFinite(ra) ? ra : undefined,
      )
    }
    if (res.status === 502)
      throw new ApiError(502, "Le service d'analyse est indisponible. Aucun résultat n'a été produit.")
    throw new ApiError(res.status, msg ?? `Erreur inattendue (${res.status}).`)
  }

  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export async function login(email: string, password: string): Promise<void> {
  const data = await request<Record<string, unknown>>('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  clearToken()
  localStorage.setItem(ROLE_KEY, typeof data.role === 'string' ? data.role : 'USER')
}

export async function logout(): Promise<void> {
  try {
    await request('/api/auth/logout', { method: 'POST' })
  } finally {
    clearToken()
  }
}

export async function register(name: string, email: string, password: string): Promise<void> {
  await request('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password }),
  })
}

export async function downloadPdf(id: number | string): Promise<void> {
  let res: Response
  try {
    res = await fetch(`${API_URL}/api/analyses/${id}/pdf`, { credentials: 'include' })
  } catch {
    throw new ApiError(0, 'Serveur injoignable. Vérifiez que le backend est démarré.')
  }
  if (res.status === 401) {
    clearToken()
    throw new ApiError(401, 'Session expirée. Veuillez vous reconnecter.')
  }
  if (!res.ok) throw new ApiError(res.status, `Téléchargement du PDF impossible (${res.status}).`)
  const url = URL.createObjectURL(await res.blob())
  const a = document.createElement('a')
  a.href = url
  a.download = `analyse_${id}.pdf`
  a.click()
  URL.revokeObjectURL(url)
}

export function postJson<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

export function putJson<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

export async function downloadFile(path: string, filename: string): Promise<void> {
  let res: Response
  try {
    res = await fetch(`${API_URL}${path}`, { credentials: 'include' })
  } catch {
    throw new ApiError(0, 'Serveur injoignable. Vérifiez que le backend est démarré.')
  }
  if (res.status === 401) {
    clearToken()
    throw new ApiError(401, 'Session expirée. Veuillez vous reconnecter.')
  }
  if (!res.ok) throw new ApiError(res.status, `Téléchargement impossible (${res.status}).`)
  const url = URL.createObjectURL(await res.blob())
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}