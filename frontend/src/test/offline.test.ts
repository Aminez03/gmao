import { describe, it, expect, beforeEach, vi } from 'vitest';
import {
  db,
  enqueue,
  pending,
  resolveOperation,
  synchronize,
  cache,
  cached,
} from '../services/offline';
import { api } from '../services/api';
describe('Saisies hors ligne', () => {
  beforeEach(async () => {
    const d = await db;
    await d.clear('operations');
    await d.clear('uploads');
    await d.clear('cache');
  });
  it('isole les données entre deux comptes', async () => {
    await cache('alice', 'work-order', { answer: 42 });
    expect(await cached('bob', 'work-order')).toBeUndefined();
    expect(await cached('alice', 'work-order')).toEqual({ answer: 42 });
  });
  it('conserve chaque saisie et sa version attendue', async () => {
    await enqueue('alice', { id: 'order', version: 3 }, 'answers', { answers: { test: true } });
    await enqueue('alice', { id: 'order', version: 3 }, 'time', { minutes: 5 });
    const ops = await pending('alice');
    expect(ops).toHaveLength(2);
    expect(ops.map((o) => o.expectedVersion)).toEqual([3, 4]);
    expect(await pending('bob')).toHaveLength(0);
  });
  it('ne supprime pas une saisie en conflit', async () => {
    const op = await enqueue('alice', { id: 'order', version: 3 }, 'answers', {
      answers: { test: true },
    });
    vi.spyOn(api, 'post').mockResolvedValue({
      data: [{ operationId: op.operationId, state: 'CONFLICT', detail: 'Version modifiée' }],
    });
    await synchronize('alice');
    const values = await pending('alice');
    expect(values).toHaveLength(1);
    expect(values[0].payload.answers.test).toBe(true);
    expect(values[0].state).toBe('CONFLICT');
  });
  it('ne retire une opération qu’après accusé serveur', async () => {
    const op = await enqueue('alice', { id: 'order', version: 3 }, 'answers', {
      answers: { test: true },
    });
    vi.spyOn(api, 'post').mockResolvedValue({
      data: [
        { operationId: op.operationId, state: 'APPLIED', resource: { id: 'order', version: 4 } },
      ],
    });
    await synchronize('alice');
    expect(await pending('alice')).toHaveLength(0);
    expect((await cached('alice', 'work-orders/order')).version).toBe(4);
  });
  it('préserve le contenu lors de la résolution explicite', async () => {
    const op = await enqueue('alice', { id: 'order', version: 3 }, 'answers', {
      answers: { test: true },
    });
    await resolveOperation('alice', op, 8);
    const values = await pending('alice');
    expect(values[0].operationId).not.toBe(op.operationId);
    expect(values[0].expectedVersion).toBe(8);
    expect(values[0].payload).toEqual(op.payload);
  });
});
