import { Save } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { useKvOperation } from '../hooks/useKvOperation'
import { putKey } from '../services/keyValueApi'
import type { KeyValue } from '../types/keyValue'
import { Button } from './Button'
import { KeyValueDisplay } from './KeyValueDisplay'
import { Panel } from './Panel'
import { describeApiError } from './apiResult'
import { ResultBanner } from './ResultBanner'
import { TextField } from './TextField'

export function KvPutPanel() {
  const [key, setKey] = useState('')
  const [value, setValue] = useState('')
  const [keyError, setKeyError] = useState<string | undefined>(undefined)
  const [valueError, setValueError] = useState<string | undefined>(undefined)
  const operation = useKvOperation<KeyValue>()

  const errorResult = operation.error ? describeApiError(operation.error) : null

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (operation.pending) {
      return
    }

    const trimmedKey = key.trim()
    const nextKeyError = trimmedKey.length === 0 ? 'Key is required.' : undefined
    const nextValueError =
      value.trim().length === 0 ? 'Value is required.' : undefined

    setKeyError(nextKeyError)
    setValueError(nextValueError)
    if (nextKeyError || nextValueError) {
      return
    }

    // The value is sent verbatim; the backend rejects only blank values.
    void operation.run(() => putKey(trimmedKey, value))
  }

  return (
    <Panel
      title="Put / Update"
      description="Replicates the write and resolves once a majority commits it."
      className="lg:row-span-2"
    >
      <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
        <TextField
          id="put-key"
          label="Key"
          value={key}
          onChange={(event) => setKey(event.target.value)}
          placeholder="e.g. user:1"
          autoComplete="off"
          spellCheck={false}
          disabled={operation.pending}
          errorText={keyError}
        />
        <TextField
          id="put-value"
          label="Value"
          value={value}
          onChange={(event) => setValue(event.target.value)}
          placeholder="e.g. alice"
          autoComplete="off"
          spellCheck={false}
          disabled={operation.pending}
          errorText={valueError}
        />
        <div>
          <Button
            type="submit"
            loading={operation.pending}
            icon={<Save className="h-4 w-4" strokeWidth={1.75} aria-hidden="true" />}
          >
            {operation.pending ? 'Storing…' : 'Put'}
          </Button>
        </div>

        {operation.status === 'success' && operation.result ? (
          <div className="flex flex-col gap-3">
            <ResultBanner
              variant="success"
              title={`Stored "${operation.result.key}".`}
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
