import axios from 'axios';
import type { AuthSessionResponse, UserProfile } from '../types/auth';
let accessToken = '';
export const api = axios.create({ baseURL: '/api/v1', timeout: 20000 });
export const setToken = (value: string) => {
  accessToken = value;
};
api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});
let refreshing: Promise<UserProfile> | null = null;
export async function refreshSession() {
  if (!refreshing)
    refreshing = axios
      .post<AuthSessionResponse>('/api/v1/auth/refresh', {}, { timeout: 10000 })
      .then((r) => {
        setToken(r.data.accessToken);
        return r.data.user;
      })
      .finally(() => {
        refreshing = null;
      });
  return refreshing;
}
api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const config = error.config;
    if (error.response?.status === 401 && !config?._retried && !config?.url?.startsWith('/auth/')) {
      config._retried = true;
      await refreshSession();
      return api(config);
    }
    throw error;
  },
);
type ApiFailure = { response?: { data?: { detail?: string } }; message?: string; code?: string };
export const errorText = (error: unknown) => {
  const failure = error as ApiFailure | null;
  if (!failure?.response) {
    if (failure?.code === 'ERR_NETWORK' || failure?.message === 'Network Error')
      return 'Connexion au serveur impossible. Vérifiez votre réseau puis réessayez.';
    if (failure?.code === 'ECONNABORTED' || failure?.code === 'ETIMEDOUT')
      return 'Le serveur met trop de temps à répondre. Réessayez dans quelques instants.';
  }
  return failure?.response?.data?.detail || failure?.message || 'Une erreur est survenue.';
};
export const headers = (version?: number, key = crypto.randomUUID()) => ({
  'Idempotency-Key': key,
  ...(version === undefined ? {} : { 'If-Match': `"${version}"` }),
});
export type Entity = Record<string, any>;
export async function command(route: string, entity: Entity, action: string, payload: Entity) {
  return (
    await api.post(`/${route}/${entity.id}/${action}`, payload, {
      headers: headers(entity.version),
    })
  ).data;
}
export async function download(path: string, name: string) {
  const response = await api.get(path, { responseType: 'blob' });
  const url = URL.createObjectURL(response.data);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
