import {
  CircleX,
  LoaderCircle,
  RefreshCw,
  ScrollText,
  WifiOff,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

export type StateVariant = 'loading' | 'error' | 'disconnected' | 'empty'

interface StateMessageProps {
  variant: StateVariant
  title: string
  message?: string
  onRetry?: () => void
}

const VARIANT_ICON: Record<StateVariant, LucideIcon> = {
  loading: LoaderCircle,
  error: CircleX,
  disconnected: WifiOff,
  empty: ScrollText,
}

const VARIANT_ICON_COLOR: Record<StateVariant, string> = {
  loading: 'text-accent-secondary',
  error: 'text-status-error',
  disconnected: 'text-status-warning',
  empty: 'text-content-muted',
}

/**
 * Shared loading / error / disconnected / empty placeholder so every page
 * reports unavailable backend data consistently.
 */
export function StateMessage({
  variant,
  title,
  message,
  onRetry,
}: StateMessageProps) {
  const Icon = VARIANT_ICON[variant]

  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-sm border border-dashed border-surface-border bg-surface-card px-6 py-10 text-center">
      <Icon
        className={`h-6 w-6 ${VARIANT_ICON_COLOR[variant]} ${
          variant === 'loading' ? 'animate-spin' : ''
        }`}
        strokeWidth={1.75}
        aria-hidden="true"
      />
      <div>
        <p className="text-sm font-medium text-content-primary">{title}</p>
        {message ? (
          <p className="mt-1 max-w-md text-[12px] text-content-muted">
            {message}
          </p>
        ) : null}
      </div>
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="mt-1 inline-flex items-center gap-2 rounded-sm border border-surface-border bg-surface-panel px-3 py-1.5 text-[12px] font-medium text-content-primary transition-colors hover:border-accent/45 hover:text-accent"
        >
          <RefreshCw className="h-3.5 w-3.5" strokeWidth={1.75} aria-hidden="true" />
          Retry
        </button>
      ) : null}
    </div>
  )
}
