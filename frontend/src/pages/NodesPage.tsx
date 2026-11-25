import { Panel } from '../components/Panel'
import { StateMessage } from '../components/StateMessage'
import { NodeTable } from '../components/NodeTable'
import type { ClusterStatusState } from '../hooks/useClusterStatus'

export function NodesPage({ cluster }: { cluster: ClusterStatusState }) {
  const { data, state, error, lastUpdated, refresh } = cluster

  return (
    <section className="mx-auto flex max-w-6xl flex-col gap-5">
      <header>
        <h2 className="text-lg font-semibold">Nodes</h2>
        <p className="mt-1 text-[13px] text-content-muted">
          Per-node state as reported by the Raft cluster.
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
              ? 'Loading nodes'
              : 'Node data unavailable'
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
        <Panel
          title="Node status"
          description={
            lastUpdated
              ? `${data.aliveNodes} of ${data.totalNodes} alive · updated ${new Date(
                  lastUpdated,
                ).toLocaleTimeString()}`
              : `${data.aliveNodes} of ${data.totalNodes} alive`
          }
          actions={
            <button
              type="button"
              onClick={refresh}
              className="rounded-sm border border-surface-border bg-surface-card px-3 py-1.5 text-[12px] font-medium text-content-primary transition-colors hover:border-accent/45 hover:text-accent"
            >
              Refresh
            </button>
          }
        >
          {data.nodes.length === 0 ? (
            <StateMessage
              variant="empty"
              title="No nodes reported"
              message="The backend returned an empty node list."
            />
          ) : (
            <NodeTable nodes={data.nodes} />
          )}
        </Panel>
      )}
    </section>
  )
}
