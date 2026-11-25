import { Activity, Database, LayoutDashboard, Server } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import type { ConnectionState } from '../hooks/useClusterStatus'
import { ConnectionIndicator } from './ConnectionIndicator'

export type PageId = 'overview' | 'nodes' | 'kv'

interface NavItem {
  id: PageId
  label: string
  icon: LucideIcon
}

const NAV_ITEMS: NavItem[] = [
  { id: 'overview', label: 'Overview', icon: LayoutDashboard },
  { id: 'nodes', label: 'Nodes', icon: Server },
  { id: 'kv', label: 'Key-Value', icon: Database },
]

interface AppShellProps {
  activePage: PageId
  onNavigate: (page: PageId) => void
  connection: ConnectionState
  children: ReactNode
}

export function AppShell({
  activePage,
  onNavigate,
  connection,
  children,
}: AppShellProps) {
  const navButtonClasses = (isActive: boolean) =>
    isActive
      ? 'flex items-center gap-3 rounded-sm border border-surface-border bg-surface-card px-3 py-2 text-[13px] font-medium text-content-primary'
      : 'flex items-center gap-3 rounded-sm px-3 py-2 text-[13px] text-content-muted transition-colors hover:bg-surface-card hover:text-content-primary'

  return (
    <div className="flex min-h-screen flex-col bg-surface-base text-content-primary">
      <header className="flex h-14 items-center justify-between border-b border-surface-border bg-surface-panel px-5">
        <div className="flex items-center gap-3">
          <Activity
            className="h-5 w-5 text-accent"
            strokeWidth={1.75}
            aria-hidden="true"
          />
          <div className="leading-tight">
            <h1 className="text-sm font-semibold tracking-tight">
              distributed-kv-store
            </h1>
            <p className="text-[11px] text-content-muted">
              Raft cluster monitoring
            </p>
          </div>
        </div>

        <ConnectionIndicator state={connection} />
      </header>

      <div className="flex flex-1 flex-col md:flex-row">
        {/* Sidebar (desktop) */}
        <aside className="hidden w-56 shrink-0 border-r border-surface-border bg-surface-panel p-3 md:block">
          <nav className="flex flex-col gap-1">
            {NAV_ITEMS.map(({ id, label, icon: Icon }) => {
              const isActive = activePage === id
              return (
                <button
                  key={id}
                  type="button"
                  onClick={() => onNavigate(id)}
                  aria-current={isActive ? 'page' : undefined}
                  className={`text-left ${navButtonClasses(isActive)}`}
                >
                  <Icon
                    className="h-4 w-4"
                    strokeWidth={1.75}
                    aria-hidden="true"
                  />
                  {label}
                </button>
              )
            })}
          </nav>
        </aside>

        {/* Top navigation (mobile) */}
        <nav className="flex gap-1 border-b border-surface-border bg-surface-panel px-3 py-2 md:hidden">
          {NAV_ITEMS.map(({ id, label, icon: Icon }) => {
            const isActive = activePage === id
            return (
              <button
                key={id}
                type="button"
                onClick={() => onNavigate(id)}
                aria-current={isActive ? 'page' : undefined}
                className={`flex flex-1 items-center justify-center gap-2 rounded-sm px-3 py-2 text-[13px] ${
                  isActive
                    ? 'border border-surface-border bg-surface-card font-medium text-content-primary'
                    : 'text-content-muted'
                }`}
              >
                <Icon className="h-4 w-4" strokeWidth={1.75} aria-hidden="true" />
                {label}
              </button>
            )
          })}
        </nav>

        <main className="flex-1 overflow-y-auto p-4 md:p-6">{children}</main>
      </div>
    </div>
  )
}
