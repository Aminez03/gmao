import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ReferenceValue from '../components/ReferenceValue';
import { resources, userReferenceFields } from '../components/domain';
import { fetchCached } from '../services/offline';

vi.mock('../app/Auth', () => ({
  useAuth: () => ({ user: { id: 'current-user', name: 'Ali Mansouri' } }),
}));
vi.mock('../services/offline', () => ({ fetchCached: vi.fn() }));
afterEach(cleanup);

function renderReference(field: string, value?: string, userNames = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <ReferenceValue field={field} value={value} resource={resources[0]} entity={{ userNames }} />
    </QueryClientProvider>,
  );
}

describe('Noms dans les colonnes utilisateurs', () => {
  it.each([...userReferenceFields])('affiche le nom pour %s sans requête de compte', (field) => {
    renderReference(field, 'user-uuid', { [field]: 'Nour Ben Salem' });
    expect(screen.getByText('Nour Ben Salem')).toBeInTheDocument();
    expect(screen.queryByText('user-uuid')).not.toBeInTheDocument();
    expect(fetchCached).not.toHaveBeenCalled();
  });

  it('utilise le nom du profil connecté avec un ancien cache', () => {
    renderReference('userId', 'current-user');
    expect(screen.getByText('Ali Mansouri')).toBeInTheDocument();
  });

  it('signale un utilisateur indisponible sans afficher son UUID', () => {
    renderReference('technicianId', 'unknown-uuid');
    expect(screen.getByText('Utilisateur indisponible')).toBeInTheDocument();
    expect(screen.queryByText('unknown-uuid')).not.toBeInTheDocument();
  });

  it('conserve un tiret pour une référence absente', () => {
    renderReference('approvedBy');
    expect(screen.getByText('—')).toBeInTheDocument();
  });

  it('continue de résoudre les autres références', async () => {
    vi.mocked(fetchCached).mockResolvedValueOnce({ name: 'Équipe A' });
    renderReference('teamId', 'team-uuid');
    expect(await screen.findByText('Équipe A')).toBeInTheDocument();
  });

  it('affiche la personne remplacée au lieu de son identifiant d’affectation', async () => {
    vi.mocked(fetchCached).mockResolvedValueOnce({
      id: 'assignment-uuid',
      userId: 'replaced-user-uuid',
      userNames: { userId: 'Nour Ben Salem' },
    });
    renderReference('replacementForId', 'assignment-uuid');
    expect(await screen.findByText('Nour Ben Salem')).toBeInTheDocument();
    expect(screen.queryByText('assignment-uuid')).not.toBeInTheDocument();
    expect(screen.queryByText('replaced-user-uuid')).not.toBeInTheDocument();
  });

  it('masque l’identifiant quand l’affectation remplacée est inaccessible', async () => {
    vi.mocked(fetchCached).mockRejectedValueOnce(new Error('Accès refusé'));
    renderReference('replacementForId', 'missing-assignment-uuid');
    expect(await screen.findByText('Affectation indisponible')).toBeInTheDocument();
    expect(screen.queryByText('missing-assignment-uuid')).not.toBeInTheDocument();
  });
});
