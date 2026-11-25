import { useState } from 'react'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { NodeTable } from '../components/NodeTable'
import { Panel } from '../components/Panel'
import { describeApiError } from '../components/apiResult'
import { ResultBanner, type ResultVariant } from '../components/ResultBanner'
import { StateMessage } from '../components/StateMessage'
import type { ClusterStatusState } from '../hooks/useClusterStatus'
import { toApiError } from '../services/http'
import { crashNode, recoverNode } from '../services/nodeApi'
import type { NodeStatus } from '../types/cluster'

interface Notice {
  variant: ResultVariant
  title: string
  message?: string
}

type ControlAction = 'crash' | 'recover'

export function NodesPage({ cluster }: { cluster: ClusterStatusState }) {
  const { data, state, error, lastUpdated, refresh } = cluster
  const [crashTarget, setCrashTarget] = useState<NodeStatus | null>(null)
  const [pendingNodeId, setPendingNodeId] = useState<string | null>(null)
  const [notice, setNotice] = useState<Notice | null>(null)

  const busy = pendingNodeId !== null

  async function runControl(nodeId: string, action: ControlAction) {
    setPendingNodeId(nodeId)
    setNotice(null)

    try {
      const updated =
        action === 'crash'
          ? await crashNode(nodeId)
          : await recoverNode(nodeId)

      setNotice({
        variant: 'success',
        title: `Node ${updated.nodeId} ${
          action === 'crash' ? 'crashed' : 'recovered'
        }.`,
        message: `Role ${updated.role} · alive: ${updated.alive ? 'yes' : 'no'}`,
      })
    } catch (err) {
      const presentation = describeApiError(toApiError(err), {
        notFound: 'Unknown node',
        conflict: 'Invalid operation',
      })
      setNotice({
        variant: presentation.variant,
        title: presentation.title,
        message: presentation.message,
      })
    } finally {
      setPendingNodeId(null)
      // Reflect the real backend state after both success and rejection.
      refresh()
    }
  }

  function handleCrashRequest(node: NodeStatus) {
    setNotice(null)
    setCrashTarget(node)
  }

  function handleConfirmCrash() {
    if (!crashTarget) {
      return
    }
    const { nodeId } = crashTarget
    setCrashTarget(null)
    void runControl(nodeId, 'crash')
  }

  function handleRecover(node: NodeStatus) {
    setNotice(null)
    void runControl(node.nodeId, 'recover')
  }

  return (
    <section className="mx-auto flex max-w-6xl flex-col gap-5">
      <header>
        <h2 className="text-lg font-semibold">Nodes</h2>
        <p className="mt-1 text-[13px] text-content-muted">
          Per-node state as reported by the Raft cluster.
        </p>
      </header>

      {notice ? (
        <ResultBanner variant={notice.variant} title={notice.title}>
          {notice.message}
        </ResultBanner>
      ) : null}

      {data === null ? (
        <StateMessage
          variant={
            state === 'loading'
              ? 'loading'
              : state === 'disconnected'
                ? 'disconnected'
                : 'error'
          }
          title={state === 'loading' ? 'Loading nodes' : 'Node data unavailable'}
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
            <NodeTable
              nodes={data.nodes}
              pendingNodeId={pendingNodeId}
              busy={busy}
              onCrash={handleCrashRequest}
              onRecover={handleRecover}
            />
          )}
        </Panel>
      )}

      <ConfirmDialog
        open={crashTarget !== null}
        title={crashTarget ? `Crash ${crashTarget.nodeId}?` : 'Crash node?'}
        confirmLabel="Crash node"
        danger
        onConfirm={handleConfirmCrash}
        onCancel={() => setCrashTarget(null)}
      >
        <p>
          The node will become unavailable until it is explicitly recovered.
          While it is down, writes may fail with 503 if the cluster can no
          longer reach a majority.
        </p>
        {crashTarget?.role === 'LEADER' ? (
          <p className="text-status-error">
            This node is the current leader — the cluster will hold an election
            to choose a new one.
          </p>
        ) : null}
      </ConfirmDialog>
    </section>
  )
}
