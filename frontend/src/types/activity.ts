/**
 * Types mirroring the backend JSON returned by GET /api/events.
 * Kept in sync with com.distributedkv.activity.ActivityEvent / EventType.
 */

export type EventType =
  | 'LEADER_ELECTED'
  | 'HEARTBEAT'
  | 'PUT_RECEIVED'
  | 'ENTRY_REPLICATED'
  | 'ENTRY_COMMITTED'
  | 'DELETE_RECEIVED'
  | 'NODE_CRASHED'
  | 'NODE_RECOVERED'
  | 'WRITE_FAILED'

export interface ActivityEvent {
  id: number
  type: EventType
  description: string
  nodeId: string | null
  term: number
  timestamp: number
}
