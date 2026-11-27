import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import ResourceForm from '../components/ResourceForm';
import { actions } from '../components/actions';
import { resources, visible } from '../components/domain';

afterEach(cleanup);

describe('Quatre rôles et remplacement de Direction et Service achats par Administrateur', () => {
  it('propose les quatre rôles actuels dans le formulaire de compte', () => {
    render(
      <ResourceForm
        fields={[{ name: 'role', type: 's', required: true }]}
        initial={{ role: 'ADMIN' }}
        onSave={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    const options = within(screen.getByRole('listbox'));
    for (const name of ['Administrateur', 'Responsable technique', 'Technicien', 'Sous-traitant']) {
      expect(options.getByRole('option', { name })).toBeInTheDocument();
    }
    expect(options.queryByRole('option', { name: 'Direction' })).not.toBeInTheDocument();
    expect(options.queryByRole('option', { name: 'Service achats' })).not.toBeInTheDocument();
    expect(options.getAllByRole('option')).toHaveLength(4);
  });

  it('réserve les commandes et ajustements de stock à l’administrateur', () => {
    expect(
      actions('purchase-orders', { status: 'BROUILLON' }, 'ADMIN').map((action) => action.key),
    ).toContain('place');
    expect(actions('inventory/items', {}, 'ADMIN').map((action) => action.key)).toContain('adjust');
    for (const role of ['TECHNICIEN', 'SOUS_TRAITANT', 'SERVICE_ACHATS', 'UNKNOWN']) {
      expect(actions('purchase-orders', { status: 'BROUILLON' }, role)).toEqual([]);
      expect(actions('inventory/items', {}, role)).toEqual([]);
    }
  });

  it('accorde à l’administrateur tous les modules et les actions de supervision', () => {
    expect(resources.every((resource) => visible(resource, 'ADMIN'))).toBe(true);
    expect(
      actions('work-orders', { status: 'TERMINEE' }, 'ADMIN').map((action) => action.key),
    ).toContain('validate');
  });

  it('refuse les modules et les actions opérationnelles à un rôle retiré ou inconnu', () => {
    for (const role of ['DIRECTION', 'SERVICE_ACHATS', 'UNKNOWN']) {
      expect(resources.some((resource) => visible(resource, role))).toBe(false);
      expect(actions('work-orders', { status: 'AFFECTEE' }, role)).toEqual([]);
      expect(actions('handovers', { acknowledgedAt: null }, role)).toEqual([]);
    }
  });
});
