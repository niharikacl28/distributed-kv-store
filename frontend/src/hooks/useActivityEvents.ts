import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchActivityEvents } from '../services/activityApi'
import type { ActivityEvent } from '../types/activity'

export type ActivityState = 'loading' | 'ready' | 'disconnected' | 'error'

export interface ActivityEventsState {
  /** Last successfully received events, or an empty list if none yet. */
  events: ActivityEvent[]
  state: ActivityState
  error: string | null
  refresh: () => void
}

const DEFAULT_POLL_INTERVAL_MS = 3000

/**
 * Polls GET /api/events on an interval.
 *
 * Never fabricates data: `events` stays empty until the backend responds, and
 * on failure the previous list is retained while `state` reports the failure so
 * the UI can mark it stale.
 */
export function useActivityEvents(
  pollIntervalMs = DEFAULT_POLL_INTERVAL_MS,
): ActivityEventsState {
  const [events, setEvents] = useState<ActivityEvent[]>([])
  const [state, setState] = useState<ActivityState>('loading')
  const [error, setError] = useState<string | null>(null)

  const isMountedRef = useRef(true)

  const load = useCallback(async () => {
    if (!isMountedRef.current) {
      return
    }

    try {
      const data = await fetchActivityEvents()
      if (!isMountedRef.current) {
        return
      }
      setEvents(data)
      setState('ready')
      setError(null)
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

  return { events, state, error, refresh }
}
