/**
 * Types mirroring the key-value REST DTOs in com.distributedkv.api.
 */

/** Body of PUT /api/kv/{key} (KeyValueRequest). */
export interface KeyValueRequest {
  value: string
}

/** Response of PUT/GET/DELETE /api/kv/{key} (KeyValueResponse). */
export interface KeyValue {
  key: string
  /** null for a DELETE response. */
  value: string | null
}
