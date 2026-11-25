import type { ReactNode } from 'react'

interface PanelProps {
  title?: string
  description?: string
  /** Optional right-aligned content in the panel header. */
  actions?: ReactNode
  children: ReactNode
  className?: string
}

/** Bordered surface panel used to group dashboard sections. */
export function Panel({
  title,
  description,
  actions,
  children,
  className,
}: PanelProps) {
  const hasHeader = title || description || actions

  return (
    <section
      className={`rounded-sm border border-surface-border bg-surface-panel ${
        className ?? ''
      }`}
    >
      {hasHeader ? (
        <header className="flex items-start justify-between gap-4 border-b border-surface-border px-5 py-4">
          <div>
            {title ? (
              <h3 className="text-sm font-semibold text-content-primary">
                {title}
              </h3>
            ) : null}
            {description ? (
              <p className="mt-1 text-[12px] text-content-muted">
                {description}
              </p>
            ) : null}
          </div>
          {actions ? <div className="shrink-0">{actions}</div> : null}
        </header>
      ) : null}
      <div className="p-5">{children}</div>
    </section>
  )
}
