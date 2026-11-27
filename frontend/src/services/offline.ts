import { openDB } from 'idb';
import { api, Entity } from './api';
export const db = openDB('polyclinique-gmao', 1, {
  upgrade(db) {
    db.createObjectStore('cache');
    db.createObjectStore('operations', { keyPath: 'operationId' });
    db.createObjectStore('uploads', { keyPath: 'id' });
  },
});
export const cache = async (user: string, key: string, value: any) =>
  (await db).put('cache', value, `${user}:${key}`);
export const cached = async (user: string, key: string) =>
  (await db).get('cache', `${user}:${key}`);
export async function findCachedAssetByQr(user: string, token: string) {
  const store = (await db).transaction('cache').objectStore('cache');
  const keys = await store.getAllKeys();
  for (const key of keys) {
    if (typeof key !== 'string' || !key.startsWith(`${user}:assets/`)) continue;
    const asset = await store.get(key);
    if (asset?.qrToken === token) return asset;
  }
  return null;
}
export async function downloadFieldData(user: string) {
  async function downloadCollection(route: string) {
    let count = 0;
    let pages = 1;
    for (let page = 0; page < pages; page++) {
      const { data } = await api.get(`/${route}`, { params: { page, size: 100 } });
      pages = data.totalPages || 1;
      for (const item of data.items || []) {
        await cache(user, `${route}/${item.id}`, item);
        count++;
      }
    }
    return count;
  }
  const [assets, workOrders] = await Promise.all([
    downloadCollection('assets'),
    downloadCollection('work-orders'),
  ]);
  return { assets, workOrders };
}
export const removeCached = async (user: string, key: string) =>
  (await db).delete('cache', `${user}:${key}`);
export async function activeProfile(value?: Entity | null) {
  const d = await db;
  if (value === null) return d.delete('cache', 'activeProfile');
  if (value) return d.put('cache', value, 'activeProfile');
  return d.get('cache', 'activeProfile');
}
export async function fetchCached(user: string, key: string, url: string) {
  if (!navigator.onLine) {
    const data = await cached(user, key);
    if (data) return data;
    throw new Error('Ces données ne sont pas disponibles sur cet appareil.');
  }
  const { data } = await api.get(url);
  await cache(user, key, data);
  return data;
}
export async function pending(user: string) {
  return ((await (await db).getAll('operations')) as Entity[])
    .filter((o) => o.userId === user)
    .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}
export async function enqueue(user: string, order: Entity, action: string, payload: Entity) {
  const others = (await pending(user)).filter(
    (o) => o.resourceId === order.id && o.state !== 'APPLIED',
  );
  const operation = {
    operationId: crypto.randomUUID(),
    userId: user,
    resourceId: order.id,
    displayNumber: order.displayNumber || order.number || order.code,
    action,
    payload,
    expectedVersion: order.version + others.filter((o) => o.state === 'PENDING').length,
    state: 'PENDING',
    createdAt: new Date().toISOString(),
  };
  await (await db).put('operations', operation);
  return operation;
}
export async function queuePhoto(
  user: string,
  order: Entity,
  file: File,
  targetType = 'work-orders',
) {
  const item = {
    id: crypto.randomUUID(),
    userId: user,
    orderId: order.id,
    targetType,
    file,
    name: file.name,
    createdAt: new Date().toISOString(),
  };
  await (await db).put('uploads', item);
  return item.id;
}
export async function uploads(user: string) {
  return ((await (await db).getAll('uploads')) as Entity[]).filter((o) => o.userId === user);
}
export async function synchronize(user: string) {
  const database = await db;
  for (const item of await uploads(user)) {
    const form = new FormData();
    form.append('targetType', item.targetType || 'work-orders');
    form.append('targetId', item.orderId);
    form.append('file', item.file, item.name);
    const response = await api.post('/files', form);
    const operations = await pending(user);
    for (const op of operations) {
      let changed = false;
      const answers = op.payload.answers;
      if (answers)
        for (const key of Object.keys(answers))
          if (answers[key] === `local:${item.id}`) {
            answers[key] = response.data.id;
            changed = true;
          }
      if (changed) await database.put('operations', op);
    }
    await database.delete('uploads', item.id);
  }
  const blocked = new Set<string>();
  for (const op of await pending(user)) {
    if (op.state !== 'PENDING' || blocked.has(op.resourceId)) continue;
    const { data } = await api.post('/sync/operations', [
      {
        operationId: op.operationId,
        resourceId: op.resourceId,
        action: op.action,
        expectedVersion: op.expectedVersion,
        payload: op.payload,
      },
    ]);
    const result = data[0];
    if (result.state === 'APPLIED') {
      await cache(
        user,
        `${op.action === 'asset-reading' ? 'assets' : 'work-orders'}/${op.resourceId}`,
        result.resource,
      );
      if (
        op.action === 'answers' &&
        JSON.stringify(await cached(user, `draft:${op.resourceId}`)) ===
          JSON.stringify(op.payload.answers)
      )
        await removeCached(user, `draft:${op.resourceId}`);
      await database.delete('operations', op.operationId);
    } else {
      await database.put('operations', { ...op, state: result.state, detail: result.detail });
      blocked.add(op.resourceId);
    }
  }
}
export async function resolveOperation(user: string, op: Entity, version: number) {
  if (op.userId !== user) throw new Error('Compte différent.');
  const d = await db;
  await d.put('operations', {
    ...op,
    operationId: crypto.randomUUID(),
    expectedVersion: version,
    state: 'PENDING',
    detail: undefined,
  });
  await d.delete('operations', op.operationId);
}
