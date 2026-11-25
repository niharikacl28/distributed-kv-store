import type { KeyValue } from '../types/keyValue'

/** Read-only display of a key/value pair returned by the backend. */
export function KeyValueDisplay({ data }: { data: KeyValue }) {
  return (
    <dl className="grid grid-cols-1 gap-px overflow-hidden rounded-sm border border-surface-border bg-surface-border sm:grid-cols-2">
      <div className="bg-surface-card px-3 py-2.5">
        <dt className="text-[10px] uppercase tracking-wide text-content-muted">
          Key
        </dt>
        <dd className="mt-1 break-all font-mono text-[13px] text-content-primary">
          {data.key}
        </dd>
      </div>
      <div className="bg-surface-card px-3 py-2.5">
        <dt className="text-[10px] uppercase tracking-wide text-content-muted">
          Value
        </dt>
        <dd className="mt-1 break-all font-mono text-[13px] text-content-primary">
          {data.value === null ? (
            <span className="text-content-muted">— (removed)</span>
          ) : (
            data.value
          )}
        </dd>
      </div>
    </dl>
  )
}
