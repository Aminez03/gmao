import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Detail from '../components/resources/ResourceDetail';
import { api } from '../services/api';
import { fetchCached } from '../services/offline';
import type { Resource } from '../components/domain';

const auth = vi.hoisted(() => ({ role: 'ADMIN' }));
vi.mock('../app/Auth', () => ({
  useAuth: () => ({ user: { id: 'user', role: auth.role } }),
}));
vi.mock('../services/offline', () => ({ fetchCached: vi.fn(), enqueue: vi.fn() }));

const resource: Resource = {
  class: 'Checklist',
  route: 'checklists',
  group: 'maintenance',
  label: 'Gammes et checklists',
  fields: [{ name: 'name', type: 's', required: true }],
};
const close = vi.fn();
const refresh = vi.fn().mockResolvedValue(undefined);

function renderDetail(published = false) {
  vi.mocked(fetchCached).mockResolvedValue({
    id: 'gamme',
    name: 'Contrôle pompe',
    version: 3,
    published,
    steps: [],
  });
  return render(
    <MemoryRouter>
      <QueryClientProvider
        client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
      >
        <Detail resource={resource} id="gamme" onClose={close} onRefresh={refresh} />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  auth.role = 'ADMIN';
  vi.clearAllMocks();
  vi.spyOn(api, 'delete').mockResolvedValue({ data: { archived: true } });
});
afterEach(cleanup);

describe('Suppression des gammes', () => {
  it('demande confirmation et permet d’annuler sans envoyer de suppression', async () => {
    renderDetail();
    fireEvent.click(await screen.findByRole('button', { name: 'Supprimer' }));
    const confirmation = screen.getByRole('dialog', { name: 'Supprimer la gamme ?' });
    expect(confirmation).toHaveTextContent('Contrôle pompe');
    expect(api.delete).not.toHaveBeenCalled();
    fireEvent.click(within(confirmation).getByRole('button', { name: 'Annuler' }));
    expect(api.delete).not.toHaveBeenCalled();
  });

  it.each(['ADMIN', 'RESPONSABLE_TECHNIQUE'])(
    'supprime le brouillon pour %s avec sa version et une clé d’opération',
    async (role) => {
      auth.role = role;
      renderDetail();
      fireEvent.click(await screen.findByRole('button', { name: 'Supprimer' }));
      fireEvent.click(screen.getByRole('button', { name: 'Confirmer la suppression' }));
      await waitFor(() =>
        expect(api.delete).toHaveBeenCalledWith('/checklists/gamme', {
          headers: { 'If-Match': '"3"', 'Idempotency-Key': expect.any(String) },
        }),
      );
      await waitFor(() => expect(refresh).toHaveBeenCalledOnce());
      expect(close).toHaveBeenCalledOnce();
    },
  );

  it('désactive la suppression d’une gamme publiée', async () => {
    renderDetail(true);
    expect(await screen.findByRole('button', { name: 'Supprimer' })).toBeDisabled();
    expect(screen.getByText('Une gamme publiée ne peut pas être supprimée.')).toBeInTheDocument();
    expect(api.delete).not.toHaveBeenCalled();
  });

  it('ne propose pas de suppression au technicien', async () => {
    auth.role = 'TECHNICIEN';
    renderDetail();
    await screen.findByRole('heading', { name: 'Contrôle pompe' });
    expect(screen.queryByRole('button', { name: 'Supprimer' })).not.toBeInTheDocument();
  });

  it('conserve la fiche et affiche le refus du serveur en cas de conflit de version', async () => {
    vi.mocked(api.delete).mockRejectedValue({
      response: { data: { detail: 'La gamme a changé. Rechargez la fiche.' } },
    });
    renderDetail();
    fireEvent.click(await screen.findByRole('button', { name: 'Supprimer' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirmer la suppression' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La gamme a changé. Rechargez la fiche.',
    );
    expect(close).not.toHaveBeenCalled();
    expect(refresh).not.toHaveBeenCalled();
  });
});
