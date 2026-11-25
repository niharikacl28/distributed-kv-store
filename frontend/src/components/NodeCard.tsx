import type { NodeStatus } from '../types/cluster'
import { getNodeVisualState, NODE_VISUALS } from './nodeVisuals'
import { RoleBadge } from './RoleBadge'

/**
 * Card used for the five-node cluster visualization on the Overview page.
 * Every value comes from the backend snapshot; nothing is derived or faked.
 */
export function NodeCard({ node }: { node: NodeStatus }) {
  const visualState = getNodeVisualState(node)
  const visual = NODE_VISUALS[visualState]

  return (
    <article
      className={`flex flex-col gap-3 rounded-sm border bg-surface-card px-4 py-4 ${visual.border}`}
    >
      <header className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span
            className={`h-2 w-2 rounded-full ${visual.dot}`}
            aria-hidden="true"
          />
          <span className="font-mono text-[13px] font-medium text-content-primary">
            {node.nodeId}
          </span>
        </div>
        <RoleBadge state={visualState} />
      </header>

      <dl className="grid grid-cols-2 gap-x-3 gap-y-2 border-t border-surface-border pt-3">
        <div>
          <dt className="text-[10px] uppercase tracking-wide text-content-muted">
            Term
          </dt>
          <dd className="font-mono text-[13px] text-content-primary">
            {node.currentTerm}
          </dd>
        </div>
        <div>
          <dt className="text-[10px] uppercase tracking-wide text-content-muted">
            Log entries
          </dt>
          <dd className="font-mono text-[13px] text-content-primary">
            {node.logEntryCount}
          </dd>
        </div>
      </dl>

      <footer className="flex items-center justify-between text-[11px] text-content-muted">
        <span>{node.alive ? 'Alive' : 'Crashed'}</span>
        <span className="font-mono">
          leader: {node.knownLeader ?? '—'}
        </span>
      </footer>
    </article>
  )
}
