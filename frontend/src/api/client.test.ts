import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

vi.stubEnv('VITE_API_URL', 'http://api.test')

function respond(status: number, body = '', headers: Record<string, string> = {}) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status, headers })))
}

describe('request', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => vi.unstubAllGlobals())

  it('401 hors authentification : session expirée et rôle effacé', async () => {
    localStorage.setItem('role', 'USER')
    respond(401)
    const { request } = await import('./client')
    await expect(request('/api/patients')).rejects.toMatchObject({ status: 401, message: expect.stringContaining('Session expirée') })
    expect(localStorage.getItem('role')).toBeNull()
  })

  it('415 : message sur le format de fichier', async () => {
    respond(415)
    const { request } = await import('./client')
    await expect(request('/api/analyses')).rejects.toMatchObject({ status: 415, message: expect.stringContaining('JPEG ou PNG') })
  })

  it('429 : minutes d\'attente déduites de Retry-After', async () => {
    respond(429, '', { 'Retry-After': '600' })
    const { request } = await import('./client')
    await expect(request('/api/auth/login')).rejects.toMatchObject({ status: 429, message: expect.stringContaining('10 minute') })
  })

  it('502 : indique qu\'aucun résultat n\'a été produit', async () => {
    respond(502)
    const { request } = await import('./client')
    await expect(request('/api/analyses')).rejects.toMatchObject({ status: 502, message: expect.stringContaining('Aucun résultat') })
  })

  it('serveur injoignable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('network')))
    const { request } = await import('./client')
    await expect(request('/api/patients')).rejects.toMatchObject({ status: 0 })
  })
})