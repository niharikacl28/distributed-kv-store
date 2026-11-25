import { useState } from 'react'
import {
  Activity,
  CircleCheck,
  Crown,
  Network,
  Save,
  ServerOff,
  ShieldCheck,
  Trash2,
  TriangleExclamationPoint,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { StateMessage } from './StateMessage'
import type { ActivityState } from '../hooks/useActivityEvents'
import type { ActivityEvent, EventType } from '../types/activity'

interface EventVisual {
  label: string
  icon: LucideIcon
  /** Restrained status colour for the icon and label. */
  className: string
}

const EVENT_VISUALS: Record<EventType, EventVisual> = {
  LEADER_ELECTED: {
    label: 'Leader elected',
    icon: Crown,
    className: 'text-accent',
  },
  HEARTBEAT: {
    label: 'Heartbeat',
    icon: Activity,
    className: 'text-content-muted',
  },
  PUT_RECEIVED: {
    label: 'PUT received',
    icon: Save,
    className: 'text-accent-secondary',
  },
  ENTRY_REPLICATED: {
    label: 'Entry replicated',
    icon: Network,
    className: 'text-accent-secondary',
  },
  ENTRY_COMMITTED: {
    label: 'Entry committed',
    icon: CircleCheck,
    className: 'text-accent',
  },
  DELETE_RECEIVED: {
    label: 'DELETE received',
    icon: Trash2,
    className: 'text-status-warning',
  },
  NODE_CRASHED: {
    label: 'Node crashed',
    icon: ServerOff,
    className: 'text-status-error',
  },
  NODE_RECOVERED: {
    label: 'Node recovered',
    icon: ShieldCheck,
    className: 'text-accent',
  },
  WRITE_FAILED: {
    label: 'Write failed',
    icon: TriangleExclamationPoint,
    className: 'text-status-error',
  },
}

function formatTime(epochMillis: number): string {
  return new Date(epochMillis).toLocaleTimeString()
}

interface ActivityTimelineProps {
  events: ActivityEvent[]
  state: ActivityState
  error: string | null
  onRetry: () => void
}

/**
 * Vertical timeline of the most recent real cluster events, newest first.
 * Renders loading / empty / error / disconnected placeholders when there is
 * nothing to show, and marks the list stale when a refresh fails.
 */
export function ActivityTimeline({
  events,
  state,
  error,
  onRetry,
}: ActivityTimelineProps) {
  // Heartbeats are noisy; hide them by default so important events stay visible.
  const [showHeartbeats, setShowHeartbeats] = useState(false)

  if (events.length === 0) {
    return (
      <StateMessage
        variant={
          state === 'loading'
            ? 'loading'
            : state === 'disconnected'
              ? 'disconnected'
              : state === 'error'
                ? 'error'
                : 'empty'
        }
        title={
          state === 'loading'
            ? 'Loading activity'
            : state === 'ready'
              ? 'No activity yet'
              : 'Activity unavailable'
        }
        message={
          state === 'loading'
            ? 'Contacting the backend at /api/events.'
            : state === 'ready'
              ? 'The cluster has not recorded any events yet.'
              : error ??
                'The event history could not be retrieved from the backend.'
        }
        onRetry={
          state === 'ready' || state === 'loading' ? undefined : onRetry
        }
      />
    )
  }

  const isStale = state === 'disconnected' || state === 'error'

  const heartbeatCount = events.filter(
    (event) => event.type === 'HEARTBEAT',
  ).length

  // Preserve backend order (newest first) while optionally hiding heartbeats.
  const visibleEvents = showHeartbeats
    ? events
    : events.filter((event) => event.type !== 'HEARTBEAT')

  return (
    <div className="flex flex-col gap-3">
      {heartbeatCount > 0 ? (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <button
            type="button"
            aria-pressed={showHeartbeats}
            onClick={() => setShowHeartbeats((value) => !value)}
            className={`inline-flex items-center gap-1.5 rounded-sm border px-2.5 py-1 text-[11px] font-medium transition-colors ${
              showHeartbeats
                ? 'border-accent/45 bg-accent/10 text-accent'
                : 'border-surface-border text-content-muted hover:border-accent/45 hover:text-accent'
            }`}
          >
            <Activity
              className="h-3.5 w-3.5"
              strokeWidth={1.75}
              aria-hidden="true"
            />
            Show heartbeat events
            <span className="font-mono">{heartbeatCount}</span>
          </button>
          {!showHeartbeats ? (
            <span className="text-[11px] text-content-muted">
              {heartbeatCount} heartbeat event
              {heartbeatCount === 1 ? '' : 's'} hidden
            </span>
          ) : null}
        </div>
      ) : null}

      {isStale ? (
        <p className="rounded-sm border border-status-warning/45 bg-status-warning/10 px-3 py-2 text-[12px] text-status-warning">
          Showing the last known events — backend{' '}
          {state === 'disconnected' ? 'unavailable' : 'error'}.
        </p>
      ) : null}

      {visibleEvents.length === 0 ? (
        <StateMessage
          variant="empty"
          title="No important events yet"
          message={`${heartbeatCount} heartbeat event${
            heartbeatCount === 1 ? '' : 's'
          } hidden. Toggle “Show heartbeat events” to view them.`}
        />
      ) : (
        <ol className="flex flex-col">
          {visibleEvents.map((event, index) => {
            const visual = EVENT_VISUALS[event.type]
            const Icon = visual.icon
            const isLast = index === visibleEvents.length - 1

            return (
              <li key={event.id} className="relative flex gap-3 pb-4 last:pb-0">
                {!isLast ? (
                  <span
                    className="absolute bottom-0 left-[13px] top-7 w-px bg-surface-border"
                    aria-hidden="true"
                  />
                ) : null}
                <span className="relative z-10 flex h-7 w-7 shrink-0 items-center justify-center rounded-full border border-surface-border bg-surface-card">
                  <Icon
                    className={`h-3.5 w-3.5 ${visual.className}`}
                    strokeWidth={1.75}
                    aria-hidden="true"
                  />
                </span>
                <div className="min-w-0 flex-1 pt-0.5">
                  <div className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
                    <span
                      className={`text-[12px] font-medium ${visual.className}`}
                    >
                      {visual.label}
                    </span>
                    {event.nodeId ? (
                      <span className="font-mono text-[11px] text-content-muted">
                        {event.nodeId}
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-0.5 break-words text-[13px] text-content-primary">
                    {event.description}
                  </p>
                  <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-content-muted">
                    <span>term {event.term}</span>
                    <span>{formatTime(event.timestamp)}</span>
                  </div>
                </div>
              </li>
            )
          })}
        </ol>
      )}
    </div>
  )
}
