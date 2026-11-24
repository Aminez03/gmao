import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import UsersPage from '../features/users/pages/UsersPage';
import { api } from '../services/api';
import type { UserProfile } from '../types/auth';

const auth = vi.hoisted(() => ({ user: null as UserProfile | null }));
vi.mock('../app/Auth', () => ({ useAuth: () => auth }));
afterEach(cleanup);

function renderPage(client = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
  return render(
    <QueryClientProvider client={client}>
      <UsersPage />
    </QueryClientProvider>,
  );
}

function profile(id: string, role: UserProfile['role']): UserProfile {
  return {
    id,
    role,
    email: `${id}@example.test`,
    name: id,
    active: true,
    contractorId: null,
    version: 0,
  };
}

describe('Accès aux utilisateurs', () => {
  it.each([null, profile('technicien', 'TECHNICIEN')])(
    'ne charge aucun compte sans permission (%j)',
    async (user) => {
      auth.user = user;
      const get = vi.spyOn(api, 'get');
      renderPage();
      expect(screen.getByRole('alert')).toHaveTextContent('Accès réservé');
      expect(get).not.toHaveBeenCalled();
    },
  );

  it('isole le cache selon le compte connecté', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: { items: [] } });
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false, staleTime: Infinity } },
    });
    auth.user = profile('admin-a', 'ADMIN');
    const view = renderPage(client);
    await vi.waitFor(() => expect(get).toHaveBeenCalledTimes(1));
    view.unmount();
    auth.user = profile('superviseur-b', 'RESPONSABLE_TECHNIQUE');
    renderPage(client);
    await vi.waitFor(() => expect(get).toHaveBeenCalledTimes(2));
    expect(screen.queryByRole('button', { name: 'Créer un compte' })).not.toBeInTheDocument();
  });
});
