import {
  CircleCheck,
  CircleX,
  Crown,
  Hash,
  ListOrdered,
  Users,
} from 'lucide-react'
import { NodeCard } from '../components/NodeCard'
import { Panel } from '../components/Panel'
import { StateMessage } from '../components/StateMessage'
import { StatCard } from '../components/StatCard'
import type { ClusterStatusState } from '../hooks/useClusterStatus'
import type { ClusterStatus, NodeStatus } from '../types/cluster'

/**
 * Derives the cluster's current Raft term.
 *
 * The backend ClusterStatusResponse has no cluster-level term field, so we read
 * it from real node data: prefer the elected leader's term, otherwise the
 * highest term among alive nodes, otherwise the highest term overall. Returns
 * null when there are no nodes at all.
 */
function deriveClusterTerm(status: ClusterStatus): number | null {
  const leader = status.nodes.find((node) => node.nodeId === status.leaderId)
  if (leader) {
    return leader.currentTerm
  }

  const pool = status.nodes.some((node) => node.alive)
    ? status.nodes.filter((node) => node.alive)
    : status.nodes

  if (pool.length === 0) {
    return null
  }

  return Math.max(...pool.map((node) => node.currentTerm))
}

function formatTime(epochMillis: number): string {
  return new Date(epochMillis).toLocaleTimeString()
}

function isStale(state: ClusterStatusState['state']): boolean {
  return state === 'disconnected' || state === 'error'
}

export function OverviewPage({ cluster }: { cluster: ClusterStatusState }) {
  const { data, state, error, lastUpdated, refresh } = cluster

  return (
    <section className="mx-auto flex max-w-6xl flex-col gap-5">
      <header>
        <h2 className="text-lg font-semibold">Cluster Overview</h2>
        <p className="mt-1 text-[13px] text-content-muted">
          Live Raft cluster state from the backend.
        </p>
      </header>

      {data === null ? (
        <StateMessage
          variant={
            state === 'loading'
              ? 'loading'
              : state === 'disconnected'
                ? 'disconnected'
                : 'error'
          }
          title={
            state === 'loading'
              ? 'Loading cluster status'
              : 'Cluster status unavailable'
          }
          message={
            state === 'loading'
              ? 'Contacting the backend at /api/cluster/status.'
              : error ??
                'The backend could not be reached. No values are shown because none have been received.'
          }
          onRetry={state === 'loading' ? undefined : refresh}
        />
      ) : (
        <>
          {isStale(state) ? (
            <div className="flex items-center justify-between gap-3 rounded-sm border border-status-warning/45 bg-status-warning/10 px-4 py-2.5 text-[12px] text-status-warning">
              <span>
                Showing last known snapshot
                {lastUpdated ? ` from ${formatTime(lastUpdated)}` : ''} — backend{' '}
                {state === 'disconnected' ? 'unavailable' : 'error'}.
              </span>
              <button
                type="button"
                onClick={refresh}
                className="shrink-0 rounded-sm border border-status-warning/45 px-2.5 py-1 text-[11px] font-medium transition-colors hover:bg-status-warning/15"
              >
                Retry
              </button>
            </div>
          ) : null}

          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-6">
            <StatCard
              label="Total nodes"
              value={data.totalNodes}
              icon={Users}
              hint="in cluster"
            />
            <StatCard
              label="Alive nodes"
              value={data.aliveNodes}
              icon={CircleCheck}
              valueClassName="text-accent"
              hint="responded"
            />
            <StatCard
              label="Crashed nodes"
              value={data.crashedNodes}
              icon={CircleX}
              valueClassName={
                data.crashedNodes > 0 ? 'text-status-error' : 'text-content-primary'
              }
              hint="unreachable"
            />
            <StatCard
              label="Current leader"
              value={data.leaderId}
              icon={Crown}
              valueClassName="text-accent"
              hint={data.leaderId ? 'elected' : 'no leader elected'}
            />
            <StatCard
              label="Raft term"
              value={deriveClusterTerm(data)}
              icon={Hash}
              hint="current term"
            />
            <StatCard
              label="Commit index"
              value={data.commitIndex}
              icon={ListOrdered}
              hint="committed entries"
            />
          </div>

          <Panel
            title="Cluster topology"
            description="Live role, term, and log state for each node."
          >
            {data.nodes.length === 0 ? (
              <StateMessage
                variant="empty"
                title="No nodes reported"
                message="The backend returned an empty node list."
              />
            ) : (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
                {data.nodes.map((node: NodeStatus) => (
                  <NodeCard key={node.nodeId} node={node} />
                ))}
              </div>
            )}
          </Panel>
        </>
      )}
    </section>
  )
}
