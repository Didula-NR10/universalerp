import { lazy, Suspense, useState } from 'react'
import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Activity, ArrowUpRight, BarChart3, Bell, Boxes, Building2, CalendarDays, ChevronDown, CircleHelp, Command, FileBarChart2, LayoutDashboard, LockKeyhole, Menu, Moon, Package, Search, Settings2, ShoppingBag, ShoppingCart, Sun, Truck, Users, Wallet, X } from 'lucide-react'
import { Button } from '../components/ui/button'
import { getSystemStatus } from '../services/system'
import { api } from '../services/api'
import { authConfigured, useAuth } from '../features/auth/AuthProvider'
import { LoginPage } from '../features/auth/LoginPage'
import { ChangePasswordPage } from '../features/auth/ChangePasswordPage'
import { PasswordSettingsPage } from '../features/auth/PasswordSettingsPage'
import { useBranches } from '../features/organization/BranchContext'
import { OrganizationPage } from '../features/organization/OrganizationPage'
import { InventoryPage } from '../features/inventory/InventoryPage'
import { ContactPage } from '../features/contacts/ContactPage'
import { FinancePage } from '../features/finance/FinancePage'
import { ProcurementPage } from '../features/procurement/ProcurementPage'
import { SalesPage } from '../features/sales/SalesPage'
import { PosPage } from '../features/pos/PosPage'
import { EmployeesPage } from '../features/employees/EmployeesPage'
import { AdminPage } from '../features/admin/AdminPage'
import { NotificationsPage } from '../features/notifications/NotificationsPage'
import { ThemeCustomizer } from '../theme/ThemeCustomizer'
import { useTheme } from '../theme/ThemeProvider'

const ReportsPage = lazy(() => import('../features/reporting/ReportsPage').then(module => ({ default: module.ReportsPage })))
const DashboardPage = lazy(() => import('../features/reporting/DashboardPage').then(module => ({ default: module.DashboardPage })))
const SchedulePage = lazy(() => import('../features/employees/SchedulePage').then(module => ({ default: module.SchedulePage })))

const sections = [
  { heading: 'Workspace', items: [
    { label: 'Overview', path: '/', icon: LayoutDashboard },
    { label: 'Organization', path: '/organization', icon: Building2 },
    { label: 'Employees', path: '/employees', icon: Users },
    { label: 'Schedules', path: '/schedules', icon: CalendarDays },
    { label: 'Notifications', path: '/notifications', icon: Bell },
  ] },
  { heading: 'Operations', items: [
    { label: 'Inventory', path: '/inventory', icon: Boxes },
    { label: 'Suppliers', path: '/suppliers', icon: Truck },
    { label: 'Customers', path: '/customers', icon: Users },
    { label: 'Procurement', path: '/procurement', icon: Truck },
    { label: 'Sales', path: '/sales', icon: ShoppingBag },
    { label: 'Point of sale', path: '/pos', icon: ShoppingCart },
  ] },
  { heading: 'Insights', items: [
    { label: 'Finance', path: '/finance', icon: Wallet },
    { label: 'Reports', path: '/reports', icon: FileBarChart2 },
  ] },
]

const pageDetails: Record<string, { title: string; description: string; icon: typeof Package }> = {
  '/organization': { title: 'Organization', description: 'Company, branches, departments, and warehouses will be managed here.', icon: Building2 },
  '/employees': { title: 'Employees', description: 'Employee records and organizational assignments will be managed here.', icon: Users },
  '/schedules': { title: 'Schedules', description: 'Plan branch shifts and employee availability.', icon: CalendarDays },
  '/notifications': { title: 'Notifications', description: 'Business activity notifications will be managed here.', icon: Bell },
  '/inventory': { title: 'Inventory', description: 'Products, stock movements, and warehouse balances will be managed here.', icon: Boxes },
  '/suppliers': { title: 'Suppliers', description: 'Supplier contact records will be managed here.', icon: Truck },
  '/customers': { title: 'Customers', description: 'Customer contact records will be managed here.', icon: Users },
  '/procurement': { title: 'Procurement', description: 'Suppliers, purchase orders, and goods receipts will be managed here.', icon: Truck },
  '/sales': { title: 'Sales', description: 'Customers, orders, invoices, and returns will be managed here.', icon: ShoppingBag },
  '/pos': { title: 'Point of sale', description: 'A connected cashier workflow will be available after sales and inventory are implemented.', icon: ShoppingCart },
  '/finance': { title: 'Finance', description: 'Expenses, payments, and financial summaries will be managed here.', icon: Wallet },
  '/reports': { title: 'Reports', description: 'Reports will use real branch-scoped API data.', icon: BarChart3 },
  '/account/password': { title: 'Change password', description: 'Update your ERP password.', icon: LockKeyhole },
}

function Sidebar({ close }: { close: () => void }) {
  return <div className="sidebar-inner">
    <div className="brand"><div className="brand-icon"><Command size={21} strokeWidth={2.5} /></div><div><strong>universal<span>erp</span></strong><small>OPERATIONS PLATFORM</small></div></div>
    <div className="company-switch"><div className="company-avatar">U</div><div><strong>Company workspace</strong><small>{authConfigured ? 'ERP workspace' : 'Foundation setup'}</small></div><ChevronDown size={16} /></div>
    <nav aria-label="Main navigation" className="nav-sections">{sections.map(section => <div className="nav-section" key={section.heading}><p>{section.heading}</p>{section.items.map(item => <NavLink onClick={close} to={item.path} end={item.path === '/'} key={item.path} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><item.icon size={18} /><span>{item.label}</span>{item.path !== '/' && (!authConfigured || !['/organization','/employees','/schedules','/notifications','/inventory','/suppliers','/customers','/finance','/procurement','/sales','/pos','/reports'].includes(item.path)) && <span className="soon-dot" title="Planned" />}</NavLink>)}</div>)}</nav>
    <div className="sidebar-bottom"><NavLink to="/account/password" onClick={close} className="nav-link"><LockKeyhole size={18} />Change password</NavLink><NavLink to="/settings" onClick={close} className="nav-link"><Settings2 size={18} />Settings</NavLink><div className="nav-link" title="Documentation is in the repository"><CircleHelp size={18} />Help</div><div className="sidebar-footnote">Version 0.1 · Foundation</div></div>
  </div>
}

function Overview() {
  const status = useQuery({ queryKey: ['system-status'], queryFn: ({ signal }) => getSystemStatus(signal), refetchInterval: 30000 })
  const available = status.isSuccess && status.data.status === 'available'
  return <>
    <div className="page-intro"><div><div className="eyebrow">WORKSPACE OVERVIEW</div><h1>Good to see you.</h1><p>Your ERP workspace is taking shape. Start with the foundation, then connect your business operations.</p></div><span className="phase-badge"><span /> Phase 1 · Foundation</span></div>
    <div className="hero-panel"><div className="hero-copy"><span className="hero-kicker"><Activity size={14} /> SYSTEM STATUS</span><h2>Built for every moving part<br />of your business.</h2><p>A single place for people, products, purchases, sales, and insights. Connect your database and authentication to begin using business modules.</p><span className="hero-link">Setup guide available in repository <ArrowUpRight size={15} /></span></div><div className="hero-art"><div className="orbit orbit-one"/><div className="orbit orbit-two"/><div className="art-center"><Command size={40} /></div><div className="art-chip chip-a"><Package size={18} /></div><div className="art-chip chip-b"><Users size={18} /></div><div className="art-chip chip-c"><BarChart3 size={18} /></div></div></div>
    <div className="section-heading"><div><h3>Workspace readiness</h3><p>Live checks from your local application</p></div><span className="tiny-label">LIVE STATUS</span></div>
    <div className="readiness-grid"><div className="readiness-card"><div className="card-top"><span className="card-icon blue"><Activity size={19}/></span><span className={`status-pill ${available ? 'up' : status.isLoading ? 'pending' : 'down'}`}>{status.isLoading ? 'Checking' : available ? 'Online' : 'Unavailable'}</span></div><h4>Backend API</h4><p>{status.isLoading ? 'Connecting to the API…' : available ? 'The foundation API is responding.' : 'Start the backend foundation profile to connect.'}</p><div className="card-bottom">{available ? 'Connected' : 'localhost:8080'}<ArrowUpRight size={15}/></div></div><div className="readiness-card"><div className="card-top"><span className="card-icon purple"><Building2 size={19}/></span><span className="status-pill pending">Next phase</span></div><h4>Business data</h4><p>PostgreSQL schema and Supabase connection are scheduled for Phase 2.</p><div className="card-bottom">Database foundation<ArrowUpRight size={15}/></div></div><div className="readiness-card"><div className="card-top"><span className="card-icon orange"><LockKeyhole size={19}/></span><span className="status-pill pending">Planned</span></div><h4>Secure access</h4><p>ERP sign-in, roles, and branch permissions come in Phase 3.</p><div className="card-bottom">Authentication<ArrowUpRight size={15}/></div></div></div>
    <div className="section-heading second"><div><h3>Business modules</h3><p>Each module will connect to real workflows and data</p></div></div><div className="module-list"><div><span className="list-icon"><Boxes size={20}/></span><div><strong>Inventory & products</strong><small>Stock ledger, warehouses, and product catalog</small></div></div><span>Planned</span></div><div className="module-list"><div><span className="list-icon"><ShoppingBag size={20}/></span><div><strong>Sales & point of sale</strong><small>Orders, invoices, payments, and checkout</small></div></div><span>Planned</span></div><div className="module-list"><div><span className="list-icon"><Truck size={20}/></span><div><strong>Procurement</strong><small>Suppliers, approvals, and goods receiving</small></div></div><span>Planned</span></div>
  </>
}

function PlannedPage({ path }: { path: string }) { const detail = pageDetails[path] ?? { title: 'Settings', description: 'System configuration will be available in a later phase.', icon: Settings2 }; const Icon = detail.icon; return <><div className="page-intro"><div><div className="eyebrow">BUSINESS MODULE</div><h1>{detail.title}</h1><p>{detail.description}</p></div><span className="phase-badge muted">Planned</span></div><div className="empty-panel"><div className="empty-icon"><Icon size={30}/></div><h2>{detail.title} is on the roadmap</h2><p>This module has no live API yet. Its workflows, permissions, and data model will be added in the project phases.</p><NavLink to="/" className="button button-default">Return to overview</NavLink></div></> }

export function App() {
  const location = useLocation()
  const auth = useAuth()
  const branchState = useBranches()
  const [mobileOpen, setMobileOpen] = useState(false)
  const { resolvedMode, setAppearance } = useTheme()
  const profile = useQuery({
    queryKey: ['erp-profile', auth.session?.user.id],
    queryFn: ({ signal }) => api<{ user: { id: string; status: string } }>('/me', auth.session!.access_token, { signal }),
    enabled: Boolean(authConfigured && auth.session && !auth.session.mustChangePassword),
    retry: false,
  })
  if (authConfigured && auth.loading) return <div className="loading-page">Loading session…</div>
  if (authConfigured && !auth.session) return <LoginPage />
  if (authConfigured && auth.session?.mustChangePassword) return <ChangePasswordPage />
  if (authConfigured && profile.isLoading) return <div className="loading-page">Checking ERP access…</div>
  if (authConfigured && profile.error) return <main className="login-page"><div className="login-card"><h1>ERP access unavailable</h1><p>This Supabase account needs an active ERP profile, or the API could not be reached.</p><div className="form-actions"><Button onClick={() => profile.refetch()}>Try again</Button><Button variant="outline" onClick={() => auth.signOut()}>Sign out</Button></div></div></main>
  const name = location.pathname === '/' ? 'Overview' : pageDetails[location.pathname]?.title ?? 'Settings'
  return <div className="app-shell"><aside className="sidebar desktop-sidebar"><Sidebar close={() => setMobileOpen(false)} /></aside>{mobileOpen && <div className="mobile-backdrop" onClick={() => setMobileOpen(false)}><aside className="sidebar mobile-sidebar" onClick={e => e.stopPropagation()}><Sidebar close={() => setMobileOpen(false)} /></aside></div>}<div className="main-area"><header className="topbar"><div className="top-left"><Button variant="ghost" className="menu-button" onClick={() => setMobileOpen(true)} aria-label="Open menu"><Menu size={20}/></Button><span className="breadcrumb-root">Workspace</span><span className="breadcrumb-slash">/</span><strong>{name}</strong></div><div className="top-actions">{authConfigured && <select aria-label="Selected branch" className="branch-select" value={branchState.selectedId} onChange={event => branchState.select(event.target.value)}><option value="">Select branch</option>{branchState.branches.map(branch => <option key={branch.id} value={branch.id}>{branch.name}</option>)}</select>}<ThemeCustomizer/><Button variant="ghost" className="appearance-trigger" aria-label="Toggle light or dark mode" title="Toggle light or dark mode" onClick={() => setAppearance(resolvedMode === 'dark' ? 'light' : 'dark')}>{resolvedMode === 'dark' ? <Sun size={19}/> : <Moon size={19}/>}</Button>{authConfigured && <><div className="top-divider"/><span className="profile-email">{auth.session?.user.email}</span><Button variant="outline" className="signout-button" onClick={() => auth.signOut()}>Sign out</Button></>}</div></header><main className="content"><Suspense fallback={<p className="empty-copy">Loading page…</p>}><Routes><Route path="/" element={authConfigured ? <DashboardPage/> : <Overview/>}/><Route path="/organization" element={authConfigured ? <OrganizationPage/> : <PlannedPage path="/organization"/>}/><Route path="/employees" element={authConfigured ? <EmployeesPage/> : <PlannedPage path="/employees"/>}/><Route path="/schedules" element={authConfigured ? <SchedulePage/> : <PlannedPage path="/schedules"/>}/><Route path="/notifications" element={authConfigured ? <NotificationsPage/> : <PlannedPage path="/notifications"/>}/><Route path="/inventory" element={authConfigured ? <InventoryPage/> : <PlannedPage path="/inventory"/>}/><Route path="/suppliers" element={authConfigured ? <ContactPage type="SUPPLIER"/> : <PlannedPage path="/suppliers"/>}/><Route path="/customers" element={authConfigured ? <ContactPage type="CUSTOMER"/> : <PlannedPage path="/customers"/>}/><Route path="/finance" element={authConfigured ? <FinancePage/> : <PlannedPage path="/finance"/>}/><Route path="/procurement" element={authConfigured ? <ProcurementPage/> : <PlannedPage path="/procurement"/>}/><Route path="/sales" element={authConfigured ? <SalesPage/> : <PlannedPage path="/sales"/>}/><Route path="/pos" element={authConfigured ? <PosPage/> : <PlannedPage path="/pos"/>}/><Route path="/reports" element={authConfigured ? <ReportsPage/> : <PlannedPage path="/reports"/>}/>{Object.keys(pageDetails).filter(path => !['/organization','/employees','/schedules','/notifications','/inventory','/suppliers','/customers','/finance','/procurement','/sales','/pos','/reports'].includes(path)).map(path => <Route key={path} path={path} element={<PlannedPage path={path}/>}/>)}<Route path="/account/password" element={authConfigured ? <PasswordSettingsPage/> : <PlannedPage path="/account/password"/>}/><Route path="/settings" element={authConfigured ? <AdminPage/> : <PlannedPage path="/settings"/>}/><Route path="*" element={<Navigate to="/" replace/>}/></Routes></Suspense></main></div></div>
}
