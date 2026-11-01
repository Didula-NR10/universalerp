import { afterEach, expect, it, vi } from 'vitest'
import { api, apiUrl } from './api'

afterEach(() => { vi.unstubAllGlobals(); vi.unstubAllEnvs() })

it('uses the configured cloud API origin', () => {
  vi.stubEnv('VITE_API_BASE_URL', 'https://erp-api.example.com/api/')
  expect(apiUrl('/branches')).toBe('https://erp-api.example.com/api/v1/branches')
})

it('sends the bearer token to the backend and returns JSON', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: 'one' }), { status: 200 }))
  vi.stubGlobal('fetch', fetchMock)
  await expect(api<{ id: string }>('/branches', 'signed-token')).resolves.toEqual({ id: 'one' })
  expect(fetchMock).toHaveBeenCalledWith('/api/v1/branches', expect.objectContaining({
    headers: expect.objectContaining({ Authorization: 'Bearer signed-token' }),
  }))
})

it('surfaces safe API problem details on denial', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ detail: 'Permission denied' }), { status: 403 })))
  await expect(api('/products', 'signed-token')).rejects.toMatchObject({ status: 403, message: 'Permission denied' })
})
