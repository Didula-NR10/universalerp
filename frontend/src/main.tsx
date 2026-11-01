import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { App } from './app/App'
import { AuthProvider } from './features/auth/AuthProvider'
import { BranchProvider } from './features/organization/BranchContext'
import { ThemeProvider } from './theme/ThemeProvider'
import './styles.css'
import './theme.css'

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: 1 } } })

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider><ThemeProvider><BranchProvider><BrowserRouter><App /></BrowserRouter></BranchProvider></ThemeProvider></AuthProvider>
    </QueryClientProvider>
  </React.StrictMode>,
)
