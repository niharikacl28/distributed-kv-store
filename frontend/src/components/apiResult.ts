import type { ApiErrorInfo } from '../services/http'
import type { ResultVariant } from './ResultBanner'

export interface ResultPresentation {
  variant: ResultVariant
  title: string
  message: string
}

/** Maps a normalised API error onto banner copy for the key-value panels. */
export function describeApiError(error: ApiErrorInfo): ResultPresentation {
  if (error.disconnected) {
    return {
      variant: 'error',
      title: 'Backend unavailable',
      message: error.message,
    }
  }

  switch (error.status) {
    case 404:
      return { variant: 'info', title: 'Key not found', message: error.message }
    case 503:
      return {
        variant: 'error',
        title: 'Service unavailable',
        message: error.message,
      }
    case 400:
      return {
        variant: 'error',
        title: 'Invalid request',
        message: error.message,
      }
    default:
      return { variant: 'error', title: 'Request failed', message: error.message }
  }
}
