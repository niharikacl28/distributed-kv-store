import type { NodeStatus } from '../types/cluster'

/**
 * Visual state used for colour-coding a node.
 *
 * `alive === false` always wins over the reported role, because a crashed node
 * keeps reporting its last known role from the backend snapshot.
 */
export type NodeVisualState = 'leader' | 'follower' | 'candidate' | 'crashed'

export function getNodeVisualState(node: NodeStatus): NodeVisualState {
  if (!node.alive) {
    return 'crashed'
  }
  switch (node.role) {
    case 'LEADER':
      return 'leader'
    case 'CANDIDATE':
      return 'candidate'
    default:
      return 'follower'
  }
}

export interface NodeVisualStyle {
  label: string
  /** Dot / accent fill. */
  dot: string
  /** Foreground text colour. */
  text: string
  /** Border colour for the node card. */
  border: string
  /** Small role badge styling. */
  badge: string
}

/**
 * Restrained, distinct colours per state, drawn from the Graphite + Forest
 * Green theme tokens defined in styles/global.css.
 */
export const NODE_VISUALS: Record<NodeVisualState, NodeVisualStyle> = {
  leader: {
    label: 'Leader',
    dot: 'bg-accent',
    text: 'text-accent',
    border: 'border-accent/45',
    badge: 'border-accent/45 bg-accent/10 text-accent',
  },
  follower: {
    label: 'Follower',
    dot: 'bg-accent-secondary',
    text: 'text-accent-secondary',
    border: 'border-surface-border',
    badge: 'border-surface-border bg-surface-panel text-accent-secondary',
  },
  candidate: {
    label: 'Candidate',
    dot: 'bg-status-warning',
    text: 'text-status-warning',
    border: 'border-status-warning/45',
    badge: 'border-status-warning/45 bg-status-warning/10 text-status-warning',
  },
  crashed: {
    label: 'Crashed',
    dot: 'bg-status-error',
    text: 'text-status-error',
    border: 'border-status-error/45',
    badge: 'border-status-error/45 bg-status-error/10 text-status-error',
  },
}
