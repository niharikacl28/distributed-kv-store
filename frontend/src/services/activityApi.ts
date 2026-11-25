import { api } from './http'
import type { ActivityEvent } from '../types/activity'

export const ACTIVITY_EVENTS_PATH = '/api/events'

/**
 * GET /api/events
 * Returns the recent cluster events reported by the backend, newest first.
 */
export function fetchActivityEvents(): Promise<ActivityEvent[]> {
  return api.get<ActivityEvent[]>(ACTIVITY_EVENTS_PATH)
}
