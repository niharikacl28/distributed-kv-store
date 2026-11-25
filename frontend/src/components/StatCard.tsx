import type { LucideIcon } from 'lucide-react'

interface StatCardProps {
  label: string
  value: string | number | null
  icon: LucideIcon
  /** Optional secondary line under the value (e.g. "of 5 nodes"). */
  hint?: string
  /** Overrides the value colour, e.g. text-status-error for crashed nodes. */
  valueClassName?: string
}

/**
 * A single cluster metric tile. Renders an em dash when `value` is null so the
 * UI never shows a fabricated figure.
 */
export function StatCard({
  label,
  value,
  icon: Icon,
  hint,
  valueClassName,
}: StatCardProps) {
  const displayValue = value === null || value === '' ? '—' : String(value)

  return (
    <div className="flex flex-col gap-3 rounded-sm border border-surface-border bg-surface-card px-4 py-4">
      <div className="flex items-center justify-between gap-2">
        <span className="text-[11px] font-medium uppercase tracking-wide text-content-muted">
          {label}
        </span>
        <Icon
          className="h-4 w-4 shrink-0 text-content-muted"
          strokeWidth={1.75}
          aria-hidden="true"
        />
      </div>
      <span
        className={`font-mono text-2xl leading-none ${
          valueClassName ?? 'text-content-primary'
        }`}
      >
        {displayValue}
      </span>
      {hint ? (
        <span className="text-[11px] text-content-muted">{hint}</span>
      ) : null}
    </div>
  )
}
