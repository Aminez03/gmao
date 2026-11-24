import { createContext, ReactNode, useContext, useEffect, useState } from 'react';
import { api, refreshSession, setToken } from '../services/api';
import { activeProfile } from '../services/offline';
import type { AuthSessionResponse, UserProfile } from '../types/auth';
type Auth = {
  user: UserProfile | null;
  loading: boolean;
  login: (email: string, password: string, turnstileToken?: string) => Promise<void>;
  logout: () => Promise<void>;
};
const Context = createContext<Auth>(null!);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    (async () => {
      try {
        const profile = await refreshSession();
        setUser(profile);
        await activeProfile(profile);
      } catch {
        if (!navigator.onLine) setUser((await activeProfile()) || null);
      } finally {
        setLoading(false);
      }
    })();
  }, []);
  async function login(email: string, password: string, turnstileToken?: string) {
    const { data } = await api.post<AuthSessionResponse>('/auth/login', {
      email,
      password,
      turnstileToken,
    });
    setToken(data.accessToken);
    setUser(data.user);
    await activeProfile(data.user);
  }
  async function logout() {
    if (!navigator.onLine)
      throw new Error('Reconnectez-vous pour révoquer la session. Vos saisies restent conservées.');
    await api.post('/auth/logout');
    setToken('');
    setUser(null);
    await activeProfile(null);
  }
  return <Context.Provider value={{ user, loading, login, logout }}>{children}</Context.Provider>;
}
export const useAuth = () => useContext(Context);
