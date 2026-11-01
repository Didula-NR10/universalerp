import { apiUrl } from './api'
export type SystemStatus = { status: string; phase: string; timestamp: string }

export async function getSystemStatus(signal?: AbortSignal): Promise<SystemStatus> {
  const response = await fetch(apiUrl('/system/status'), { signal })
  if (!response.ok) throw new Error(`API returned ${response.status}`)
  return response.json() as Promise<SystemStatus>
}
