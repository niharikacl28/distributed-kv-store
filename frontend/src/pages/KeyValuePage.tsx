import { KvDeletePanel } from '../components/KvDeletePanel'
import { KvGetPanel } from '../components/KvGetPanel'
import { KvPutPanel } from '../components/KvPutPanel'

export function KeyValuePage() {
  return (
    <section className="mx-auto flex max-w-6xl flex-col gap-5">
      <header>
        <h2 className="text-lg font-semibold">Key-Value</h2>
        <p className="mt-1 text-[13px] text-content-muted">
          Execute PUT, GET and DELETE operations through the backend.
        </p>
      </header>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <KvPutPanel />
        <KvGetPanel />
        <KvDeletePanel />
      </div>
    </section>
  )
}
