import { api } from './http'
import type { NodeStatus } from '../types/cluster'

function nodePath(nodeId: string, action: 'crash' | 'recover'): string {
  return `/api/nodes/${encodeURIComponent(nodeId)}/${action}`
}

/**
 * POST /api/nodes/{nodeId}/crash
 * Takes the node offline. Rejects with HttpError(404) for an unknown node and
 * HttpError(409) when the node is already crashed.
 */
export function crashNode(nodeId: string): Promise<NodeStatus> {
  return api.post<NodeStatus>(nodePath(nodeId, 'crash'))
}

/**
 * POST /api/nodes/{nodeId}/recover
 * Brings a crashed node back and resynchronises it from the leader. Rejects with
 * HttpError(404) for an unknown node and HttpError(409) when already running.
 */
export function recoverNode(nodeId: string): Promise<NodeStatus> {
  return api.post<NodeStatus>(nodePath(nodeId, 'recover'))
}
