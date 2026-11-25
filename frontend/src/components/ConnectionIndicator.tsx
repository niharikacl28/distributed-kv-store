import type { ConnectionState } from '../hooks/useClusterStatus'

interface ConnectionIndicatorProps {
  state: ConnectionState
}

const CONNECTION_LABEL: Record<ConnectionState, string> = {
  loading: 'Connecting…',
  ready: 'Backend connected',
  disconnected: 'Backend unavailable',
  error: 'Backend error',
}

const CONNECTION_DOT: Record<ConnectionState, string> = {
  loading: 'bg-content-muted',
  ready: 'bg-accent',
  disconnected: 'bg-status-error',
  error: 'bg-status-warning',
}

/** Live backend connectivity indicator shown in the app header. */
export function ConnectionIndicator({ state }: ConnectionIndicatorProps) {
  return (
    <div className="flex items-center gap-2 text-[11px] text-content-muted">
      <span
        className={`h-1.5 w-1.5 rounded-full ${CONNECTION_DOT[state]} ${
          state === 'loading' ? 'animate-pulse' : ''
        }`}
        aria-hidden="true"
      />
      {CONNECTION_LABEL[state]}
    </div>
  )
}
