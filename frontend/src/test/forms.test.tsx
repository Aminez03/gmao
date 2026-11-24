import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';
import ResourceForm from '../components/ResourceForm';
afterEach(cleanup);
describe('Formulaires métier', () => {
  it('bloque les champs obligatoires vides', async () => {
    const save = vi.fn();
    render(
      <ResourceForm
        fields={[{ name: 'title', type: 's', required: true }]}
        onSave={save}
        onCancel={() => {}}
      />,
    );
    fireEvent.submit(screen.getByRole('button', { name: 'Enregistrer' }).closest('form')!);
    await screen.findByText('Ce champ est obligatoire.');
    expect(save).not.toHaveBeenCalled();
  });
  it('envoie seulement les champs éditables', async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    render(
      <ResourceForm
        fields={[{ name: 'title', type: 's', required: true }]}
        initial={{ title: 'Pompe', role: 'ADMIN' }}
        onSave={save}
        onCancel={() => {}}
      />,
    );
    fireEvent.submit(screen.getByRole('button', { name: 'Enregistrer' }).closest('form')!);
    await waitFor(() => expect(save).toHaveBeenCalledWith({ title: 'Pompe' }));
  });
  it('affiche une erreur API sans perdre la saisie', async () => {
    render(
      <ResourceForm
        fields={[{ name: 'title', type: 's', required: true }]}
        initial={{ title: 'Pompe' }}
        onSave={async () => {
          throw { response: { data: { detail: 'Conflit de version' } } };
        }}
        onCancel={() => {}}
      />,
    );
    fireEvent.submit(screen.getByRole('button', { name: 'Enregistrer' }).closest('form')!);
    await screen.findByText('Conflit de version');
    expect(screen.getByLabelText(/Intitulé/)).toHaveValue('Pompe');
  });
});
