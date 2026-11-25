import { LoaderCircle } from 'lucide-react'
import type { ButtonHTMLAttributes, ReactNode } from 'react'

export type ButtonVariant = 'primary' | 'secondary' | 'danger'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  /** Shows a spinner and disables the button. */
  loading?: boolean
  icon?: ReactNode
}

const VARIANT_CLASSES: Record<ButtonVariant, string> = {
  primary: 'border-accent/50 bg-accent/15 text-accent hover:bg-accent/25',
  secondary:
    'border-surface-border bg-surface-card text-content-primary hover:border-accent/45 hover:text-accent',
  danger:
    'border-status-error/50 bg-status-error/10 text-status-error hover:bg-status-error/20',
}

/** Compact action button. Disabled while `loading` to prevent duplicates. */
export function Button({
  variant = 'primary',
  loading = false,
  icon,
  children,
  disabled,
  className,
  ...rest
}: ButtonProps) {
  return (
    <button
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={`inline-flex items-center justify-center gap-2 rounded-sm border px-3.5 py-2 text-[13px] font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${VARIANT_CLASSES[variant]} ${
        className ?? ''
      }`}
      {...rest}
    >
      {loading ? (
        <LoaderCircle
          className="h-4 w-4 animate-spin"
          strokeWidth={1.75}
          aria-hidden="true"
        />
      ) : (
        icon
      )}
      {children}
    </button>
  )
}
