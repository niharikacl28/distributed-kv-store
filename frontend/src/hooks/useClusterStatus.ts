import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchClusterStatus } from '../services/clusterApi'
import type { ClusterStatus } from '../types/cluster'

export type ConnectionState = 'loading' | 'ready' | 'disconnected' | 'error'

export interface ClusterStatusState {
  /** Last successfully received snapshot, or null if none has been received. */
  data: ClusterStatus | null
  state: ConnectionState
  error: string | null
  /** Epoch millis of the last successful poll, or null. */
  lastUpdated: number | null
  refresh: () => void
}

const DEFAULT_POLL_INTERVAL_MS = 3000

/**
 * Polls GET /api/cluster/status on an interval.
 *
 * Never fabricates data: `data` stays null until the backend responds, and on
 * failure the previous snapshot (if any) is retained while `state` reports the
 * failure so the UI can mark it stale.
 */
export function useClusterStatus(
  pollIntervalMs = DEFAULT_POLL_INTERVAL_MS,
): ClusterStatusState {
  const [data, setData] = useState<ClusterStatus | null>(null)
  const [state, setState] = useState<ConnectionState>('loading')
  const [error, setError] = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<number | null>(null)

  const isMountedRef = useRef(true)

  const load = useCallback(async () => {
    if (!isMountedRef.current) {
      return
    }

    try {
      const status = await fetchClusterStatus()
      if (!isMountedRef.current) {
        return
      }
      setData(status)
      setState('ready')
      setError(null)
      setLastUpdated(Date.now())
    } catch (err) {
      if (!isMountedRef.current) {
        return
      }
      const message =
        err instanceof Error ? err.message : 'Unexpected client error'
      setError(message)
      // A thrown fetch (network/CORS) surfaces as TypeError; HTTP errors do not.
      setState(err instanceof TypeError ? 'disconnected' : 'error')
    }
  }, [])

  useEffect(() => {
    isMountedRef.current = true
    // Initial fetch. State updates happen after the awaited response, so the
    // rule's synchronous-update concern does not apply to this async call.
    // oxlint-disable-next-line react/set-state-in-effect
    void load()

    const intervalId = window.setInterval(() => {
      void load()
    }, pollIntervalMs)

    return () => {
      isMountedRef.current = false
      window.clearInterval(intervalId)
    }
  }, [load, pollIntervalMs])

  const refresh = useCallback(() => {
    setState('loading')
    void load()
  }, [load])

  return { data, state, error, lastUpdated, refresh }
}
