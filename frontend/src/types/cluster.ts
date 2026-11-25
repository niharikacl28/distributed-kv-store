/**
 * Types mirroring the Spring Boot REST responses.
 * Kept in sync with com.distributedkv.api DTOs.
 */

export type NodeRole = 'FOLLOWER' | 'CANDIDATE' | 'LEADER'

export interface NodeStatus {
  nodeId: string
  role: NodeRole
  currentTerm: number
  alive: boolean
  knownLeader: string | null
  logEntryCount: number
}

export interface ClusterStatus {
  totalNodes: number
  aliveNodes: number
  crashedNodes: number
  leaderId: string | null
  commitIndex: number
  nodes: NodeStatus[]
}
