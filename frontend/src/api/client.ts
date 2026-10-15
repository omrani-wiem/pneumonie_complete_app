const API_URL = import.meta.env.VITE_API_URL as string
const TOKEN_KEY = 'token'

export const getToken = (): string | null => localStorage.getItem(TOKEN_KEY)
export const setToken = (t: string): void => localStorage.setItem(TOKEN_KEY, t)
export const clearToken = (): void => localStorage.removeItem(TOKEN_KEY)

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
  const headers = new Headers(init.headers)
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  let res: Response
  try {
    res = await fetch(`${API_URL}${path}`, { ...init, headers })
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
  const t = data.token ?? data.accessToken ?? data.jwt
  if (typeof t !== 'string') throw new ApiError(500, 'Réponse de connexion inattendue : jeton absent.')
  setToken(t)
}

export async function register(name: string, email: string, password: string): Promise<void> {
  await request('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password }),
  })
}

export async function downloadPdf(id: number | string): Promise<void> {
  const token = getToken()
  let res: Response
  try {
    res = await fetch(`${API_URL}/api/analyses/${id}/pdf`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
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