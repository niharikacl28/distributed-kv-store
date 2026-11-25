import { Crown } from 'lucide-react'
import { NODE_VISUALS, type NodeVisualState } from './nodeVisuals'

/** Small status chip for a node's visual state (Leader/Follower/Candidate/Crashed). */
export function RoleBadge({ state }: { state: NodeVisualState }) {
  const visual = NODE_VISUALS[state]

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-sm border px-2 py-0.5 text-[10px] font-medium uppercase tracking-wide ${visual.badge}`}
    >
      {state === 'leader' ? (
        <Crown className="h-3 w-3" strokeWidth={2} aria-hidden="true" />
      ) : null}
      {visual.label}
    </span>
  )
}
