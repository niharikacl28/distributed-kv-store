import { RefreshCw, ServerOff } from 'lucide-react'
import type { NodeStatus } from '../types/cluster'
import { getNodeVisualState, NODE_VISUALS } from './nodeVisuals'
import { Button } from './Button'
import { RoleBadge } from './RoleBadge'

const COLUMNS = [
  'Node ID',
  'Role',
  'Status',
  'Term',
  'Log entries',
  'Known leader',
  'Actions',
]

interface NodeTableProps {
  nodes: NodeStatus[]
  /** Node whose control request is currently in flight (spinner). */
  pendingNodeId: string | null
  /** True while any control request is in flight; disables all controls. */
  busy: boolean
  onCrash: (node: NodeStatus) => void
  onRecover: (node: NodeStatus) => void
}

/**
 * Technical node table for the Nodes page. Renders horizontally scrollable on
 * small screens instead of collapsing into cards, to stay information-dense.
 */
export function NodeTable({
  nodes,
  pendingNodeId,
  busy,
  onCrash,
  onRecover,
}: NodeTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[760px] border-collapse text-left">
        <thead>
          <tr className="border-b border-surface-border">
            {COLUMNS.map((column) => (
              <th
                key={column}
                scope="col"
                className="px-4 py-2.5 text-[11px] font-medium uppercase tracking-wide text-content-muted"
              >
                {column}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {nodes.map((node) => {
            const visualState = getNodeVisualState(node)
            const visual = NODE_VISUALS[visualState]
            const isPending = pendingNodeId === node.nodeId

            return (
              <tr
                key={node.nodeId}
                className="border-b border-surface-border last:border-b-0 hover:bg-surface-card"
              >
                <td className="px-4 py-3 font-mono text-[13px] text-content-primary">
                  <span className="inline-flex items-center gap-2">
                    <span
                      className={`h-2 w-2 rounded-full ${visual.dot}`}
                      aria-hidden="true"
                    />
                    {node.nodeId}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <RoleBadge state={visualState} />
                </td>
                <td className={`px-4 py-3 text-[13px] ${visual.text}`}>
                  {node.alive ? 'Alive' : 'Crashed'}
                </td>
                <td className="px-4 py-3 font-mono text-[13px] text-content-primary">
                  {node.currentTerm}
                </td>
                <td className="px-4 py-3 font-mono text-[13px] text-content-primary">
                  {node.logEntryCount}
                </td>
                <td className="px-4 py-3 font-mono text-[13px] text-content-muted">
                  {node.knownLeader ?? '—'}
                </td>
                <td className="px-4 py-3">
                  {node.alive ? (
                    <Button
                      size="sm"
                      variant="danger"
                      disabled={busy}
                      loading={isPending}
                      onClick={() => onCrash(node)}
                      icon={
                        <ServerOff
                          className="h-3.5 w-3.5"
                          strokeWidth={1.75}
                          aria-hidden="true"
                        />
                      }
                    >
                      Crash
                    </Button>
                  ) : (
                    <Button
                      size="sm"
                      variant="secondary"
                      disabled={busy}
                      loading={isPending}
                      onClick={() => onRecover(node)}
                      icon={
                        <RefreshCw
                          className="h-3.5 w-3.5"
                          strokeWidth={1.75}
                          aria-hidden="true"
                        />
                      }
                    >
                      Recover
                    </Button>
                  )}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
