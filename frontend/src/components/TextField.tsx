import type { InputHTMLAttributes } from 'react'

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  id: string
  label: string
  hint?: string
  /** When set, the field renders in an error state and shows this message. */
  errorText?: string
}

/** Labelled monospace input used across the key-value forms. */
export function TextField({
  id,
  label,
  hint,
  errorText,
  className,
  ...rest
}: TextFieldProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label
        htmlFor={id}
        className="text-[11px] font-medium uppercase tracking-wide text-content-muted"
      >
        {label}
      </label>
      <input
        id={id}
        aria-invalid={errorText ? true : undefined}
        className={`w-full rounded-sm border bg-surface-card px-3 py-2 font-mono text-[13px] text-content-primary placeholder:text-content-muted/60 focus:outline-none focus:ring-1 focus:ring-accent/50 disabled:cursor-not-allowed disabled:opacity-50 ${
          errorText ? 'border-status-error/60' : 'border-surface-border'
        } ${className ?? ''}`}
        {...rest}
      />
      {errorText ? (
        <p className="text-[11px] text-status-error">{errorText}</p>
      ) : hint ? (
        <p className="text-[11px] text-content-muted">{hint}</p>
      ) : null}
    </div>
  )
}
