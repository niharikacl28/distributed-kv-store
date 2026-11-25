import { CircleCheck, CircleX, Info } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'

export type ResultVariant = 'success' | 'error' | 'info'

const VARIANT_STYLES: Record<ResultVariant, string> = {
  success: 'border-accent/45 bg-accent/10 text-accent',
  error: 'border-status-error/45 bg-status-error/10 text-status-error',
  info: 'border-status-warning/45 bg-status-warning/10 text-status-warning',
}

const VARIANT_ICONS: Record<ResultVariant, LucideIcon> = {
  success: CircleCheck,
  error: CircleX,
  info: Info,
}

interface ResultBannerProps {
  variant: ResultVariant
  title: string
  children?: ReactNode
}

/** Inline success / error / not-found notification for an operation. */
export function ResultBanner({ variant, title, children }: ResultBannerProps) {
  const Icon = VARIANT_ICONS[variant]

  return (
    <div
      role="status"
      className={`flex items-start gap-2.5 rounded-sm border px-3.5 py-3 text-[12px] ${VARIANT_STYLES[variant]}`}
    >
      <Icon
        className="mt-0.5 h-4 w-4 shrink-0"
        strokeWidth={1.75}
        aria-hidden="true"
      />
      <div className="min-w-0">
        <p className="font-medium">{title}</p>
        {children ? (
          <div className="mt-1.5 text-content-muted">{children}</div>
        ) : null}
      </div>
    </div>
  )
}
