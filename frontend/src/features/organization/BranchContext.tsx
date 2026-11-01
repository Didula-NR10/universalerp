import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../auth/AuthProvider'
import { branches, type Branch } from './organizationApi'

type BranchContextValue = { branches: Branch[]; selectedId: string; select: (id: string) => void; loading: boolean; error: string }
const BranchContext = createContext<BranchContextValue | null>(null)

export function BranchProvider({ children }: { children: ReactNode }) {
  const { session } = useAuth()
  const token = session?.access_token ?? ''
  const [selectedId, select] = useState('')
  const query = useQuery({ queryKey: ['branches', session?.user.id], queryFn: ({ signal }) => branches(token, signal), enabled: Boolean(token && !session?.mustChangePassword) })
  const list = query.data ?? []
  useEffect(() => { if (list.length && !list.some(b => b.id === selectedId)) select(list[0].id) }, [list, selectedId])
  const value = useMemo(() => ({ branches: list, selectedId, select, loading: query.isLoading, error: query.error instanceof Error ? query.error.message : '' }), [list, selectedId, query.isLoading, query.error])
  return <BranchContext.Provider value={value}>{children}</BranchContext.Provider>
}

export function useBranches() { const value = useContext(BranchContext); if (!value) throw new Error('BranchProvider is missing'); return value }
