export type ApiProblem = { detail?: string; title?: string; code?: string }
export class ApiError extends Error { constructor(readonly status: number, message: string) { super(message) } }
export const apiUrl = (path: string) => {
  const base = import.meta.env.VITE_API_BASE_URL
    || (import.meta.env.VITE_API_ORIGIN ? `${import.meta.env.VITE_API_ORIGIN.replace(/\/$/, '')}/api` : '/api')
  return `${base.replace(/\/$/, '')}/v1${path}`
}

export async function api<T>(path: string, token: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(apiUrl(path), {
    ...init,
    headers: { 'Authorization': `Bearer ${token}`, ...(init.body ? { 'Content-Type': 'application/json' } : {}), ...init.headers },
  })
  if (!response.ok) {
    if (response.status === 401) window.dispatchEvent(new Event('erp-auth-expired'))
    const problem = await response.json().catch(() => ({})) as ApiProblem
    throw new ApiError(response.status, problem.detail ?? problem.title ?? `Request failed (${response.status})`)
  }
  return response.json() as Promise<T>
}
