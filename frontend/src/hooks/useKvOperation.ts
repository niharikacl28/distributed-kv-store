import { useCallback, useState } from 'react'
import { toApiError, type ApiErrorInfo } from '../services/http'

export type OperationStatus = 'idle' | 'pending' | 'success' | 'error'

export interface KvOperation<T> {
  status: OperationStatus
  /** Result of the last successful run, or null. */
  result: T | null
  /** Error of the last failed run, or null. */
  error: ApiErrorInfo | null
  /** True while a run is in flight; use to block duplicate submissions. */
  pending: boolean
  /** Runs `action`, tracking pending/success/error and normalising failures. */
  run: (action: () => Promise<T>) => Promise<void>
  reset: () => void
}

/**
 * Small state machine shared by the PUT/GET/DELETE operations so each panel
 * reports progress, success and failures consistently. No result is set unless
 * the action actually resolves.
 */
export function useKvOperation<T>(): KvOperation<T> {
  const [status, setStatus] = useState<OperationStatus>('idle')
  const [result, setResult] = useState<T | null>(null)
  const [error, setError] = useState<ApiErrorInfo | null>(null)

  const run = useCallback(async (action: () => Promise<T>) => {
    setStatus('pending')
    setResult(null)
    setError(null)

    try {
      const value = await action()
      setResult(value)
      setStatus('success')
    } catch (err) {
      setError(toApiError(err))
      setStatus('error')
    }
  }, [])

  const reset = useCallback(() => {
    setStatus('idle')
    setResult(null)
    setError(null)
  }, [])

  return { status, result, error, pending: status === 'pending', run, reset }
}
