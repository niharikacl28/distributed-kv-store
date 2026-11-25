import { api } from './http'
import type { KeyValue, KeyValueRequest } from '../types/keyValue'

/** Endpoint path for a key, URL-encoding characters unsafe in a path segment. */
export function kvPath(key: string): string {
  return `/api/kv/${encodeURIComponent(key)}`
}

/**
 * PUT /api/kv/{key}
 * Replicates the write and resolves only once it is majority-committed.
 * Rejects with HttpError(400) for a blank key/value and HttpError(503) when no
 * leader is available or a majority cannot commit the entry.
 */
export function putKey(key: string, value: string): Promise<KeyValue> {
  const body: KeyValueRequest = { value }
  return api.put<KeyValue>(kvPath(key), body)
}

/**
 * GET /api/kv/{key}
 * Reads from the leader's committed store. Rejects with HttpError(404) when the
 * key is missing and HttpError(503) when there is no leader.
 */
export function getKey(key: string): Promise<KeyValue> {
  return api.get<KeyValue>(kvPath(key))
}

/**
 * DELETE /api/kv/{key}
 * Replicates the delete and resolves once majority-committed. Responds with the
 * key and a null value; rejects with HttpError(503) on no leader / no quorum.
 */
export function deleteKey(key: string): Promise<KeyValue> {
  return api.delete<KeyValue>(kvPath(key))
}
