import type { ApiErrorInfo } from '../services/http'
import type { ResultVariant } from './ResultBanner'

export interface ResultPresentation {
  variant: ResultVariant
  title: string
  message: string
}

export interface ApiErrorCopy {
  /** Title shown for HTTP 404. */
  notFound?: string
  /** Title shown for HTTP 409. */
  conflict?: string
}

/**
 * Maps a normalised API error onto banner copy. Titles for 404/409 can be
 * overridden per call site (e.g. "Key not found" vs "Unknown node").
 */
export function describeApiError(
  error: ApiErrorInfo,
  copy: ApiErrorCopy = {},
): ResultPresentation {
  if (error.disconnected) {
    return {
      variant: 'error',
      title: 'Backend unavailable',
      message: error.message,
    }
  }

  switch (error.status) {
    case 400:
      return {
        variant: 'error',
        title: 'Invalid request',
        message: error.message,
      }
    case 404:
      return {
        variant: 'info',
        title: copy.notFound ?? 'Not found',
        message: error.message,
      }
    case 409:
      return {
        variant: 'error',
        title: copy.conflict ?? 'Conflict',
        message: error.message,
      }
    case 503:
      return {
        variant: 'error',
        title: 'Service unavailable',
        message: error.message,
      }
    default:
      return {
        variant: 'error',
        title: 'Request failed',
        message: error.message,
      }
  }
}
