import type { Entity } from '../services/api';

/** A permanent server reference; never derive a visible number from a row index or UUID. */
export function businessReference(entity?: Entity | null): string {
  return entity?.displayNumber || entity?.number || entity?.code || 'Référence indisponible';
}
