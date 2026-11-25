import { Trash2 } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { useKvOperation } from '../hooks/useKvOperation'
import { deleteKey } from '../services/keyValueApi'
import type { KeyValue } from '../types/keyValue'
import { Button } from './Button'
import { KeyValueDisplay } from './KeyValueDisplay'
import { Panel } from './Panel'
import { describeApiError } from './apiResult'
import { ResultBanner } from './ResultBanner'
import { TextField } from './TextField'

export function KvDeletePanel() {
  const [key, setKey] = useState('')
  const [keyError, setKeyError] = useState<string | undefined>(undefined)
  const operation = useKvOperation<KeyValue>()

  const errorResult = operation.error
    ? describeApiError(operation.error, { notFound: 'Key not found' })
    : null

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (operation.pending) {
      return
    }

    const trimmedKey = key.trim()
    if (trimmedKey.length === 0) {
      setKeyError('Key is required.')
      return
    }

    setKeyError(undefined)
    void operation.run(() => deleteKey(trimmedKey))
  }

  return (
    <Panel
      title="Delete"
      description="Replicates the delete; a majority must commit it before success."
    >
      <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
        <TextField
          id="delete-key"
          label="Key"
          value={key}
          onChange={(event) => setKey(event.target.value)}
          placeholder="e.g. user:1"
          autoComplete="off"
          spellCheck={false}
          disabled={operation.pending}
          errorText={keyError}
        />
        <div>
          <Button
            type="submit"
            variant="danger"
            loading={operation.pending}
            icon={<Trash2 className="h-4 w-4" strokeWidth={1.75} aria-hidden="true" />}
          >
            {operation.pending ? 'Deleting…' : 'Delete'}
          </Button>
        </div>

        {operation.status === 'success' && operation.result ? (
          <div className="flex flex-col gap-3">
            <ResultBanner
              variant="success"
              title={`Delete committed for "${operation.result.key}".`}
            />
            <KeyValueDisplay data={operation.result} />
          </div>
        ) : null}

        {operation.status === 'error' && errorResult ? (
          <ResultBanner variant={errorResult.variant} title={errorResult.title}>
            {errorResult.message}
          </ResultBanner>
        ) : null}
      </form>
    </Panel>
  )
}
