import { LoaderCircle } from 'lucide-react'
import type { ButtonHTMLAttributes, ReactNode } from 'react'

export type ButtonVariant = 'primary' | 'secondary' | 'danger'
export type ButtonSize = 'sm' | 'md'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
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

const SIZE_CLASSES: Record<ButtonSize, string> = {
  sm: 'gap-1.5 px-2.5 py-1 text-[12px]',
  md: 'gap-2 px-3.5 py-2 text-[13px]',
}

/** Compact action button. Disabled while `loading` to prevent duplicates. */
export function Button({
  variant = 'primary',
  size = 'md',
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
      className={`inline-flex items-center justify-center rounded-sm border font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${SIZE_CLASSES[size]} ${VARIANT_CLASSES[variant]} ${
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
