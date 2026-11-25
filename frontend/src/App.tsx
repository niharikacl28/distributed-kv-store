import { useState } from 'react'
import { AppShell, type PageId } from './components/AppShell'
import { useClusterStatus } from './hooks/useClusterStatus'
import { useDocumentTitle } from './hooks/useDocumentTitle'
import { KeyValuePage } from './pages/KeyValuePage'
import { NodesPage } from './pages/NodesPage'
import { OverviewPage } from './pages/OverviewPage'

function renderPage(page: PageId, cluster: ReturnType<typeof useClusterStatus>) {
  switch (page) {
    case 'nodes':
      return <NodesPage cluster={cluster} />
    case 'kv':
      return <KeyValuePage />
    default:
      return <OverviewPage cluster={cluster} />
  }
}

export default function App() {
  useDocumentTitle('distributed-kv-store · Cluster Monitor')

  const [page, setPage] = useState<PageId>('overview')
  // Single polling source shared by the cluster pages so the backend is queried once.
  const cluster = useClusterStatus()

  return (
    <AppShell
      activePage={page}
      onNavigate={setPage}
      connection={cluster.state}
    >
      {renderPage(page, cluster)}
    </AppShell>
  )
}
