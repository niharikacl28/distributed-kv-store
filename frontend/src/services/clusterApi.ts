import { api } from './http'
import type { ClusterStatus } from '../types/cluster'

export const CLUSTER_STATUS_PATH = '/api/cluster/status'

/**
 * GET /api/cluster/status
 * Returns the live cluster snapshot from the Spring Boot backend.
 */
export function fetchClusterStatus(): Promise<ClusterStatus> {
  return api.get<ClusterStatus>(CLUSTER_STATUS_PATH)
}
