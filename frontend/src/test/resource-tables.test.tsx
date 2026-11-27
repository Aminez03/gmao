import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import ResourcePage from '../components/resources/ResourcePage';
import { getResource, resources } from '../components/domain';
import type { Entity } from '../services/api';

vi.mock('../app/Auth', () => ({
  useAuth: () => ({ user: { id: 'admin', role: 'ADMIN' } }),
}));
vi.mock('../services/offline', () => ({ fetchCached: vi.fn() }));
vi.mock('../components/resources/ResourceDetail', () => ({
  default: ({ id }: { id: string }) => <div role="dialog">{id}</div>,
}));

afterEach(cleanup);

const recordId = 'bfeb0c0f-1234-4567-8910-123456789abc';
const references: Record<string, string> = {
  locations: 'LOC-001',
  assets: 'EQ-001',
  teams: 'EQP-001',
  skills: 'CMP-001',
  'user-skills': 'HBL-001',
  'team-members': 'MEM-001',
  shifts: 'PST-001',
  'shift-assignments': 'AFF-001',
  absences: 'ABS-001',
  handovers: 'PAS-001',
  checklists: 'GAM-001',
  'preventive-plans': 'PM-001',
  'preventive-occurrences': 'ECH-001',
  'work-orders': 'OT-001',
  'failure-reports': 'PAN-001',
  'time-entries': 'TPS-001',
  anomalies: 'ANO-001',
  downtimes: 'ARR-001',
  suppliers: 'FOU-001',
  'inventory/items': 'PIE-001',
  'inventory/reservations': 'RES-001',
  'inventory/movements': 'MVT-001',
  'purchase-requests': 'DA-001',
  'purchase-orders': 'BC-001',
  'goods-receipts': 'BR-001',
  subcontractors: 'PRE-001',
  contracts: 'CT-001',
  'external-interventions': 'EXT-001',
  'material-requests': 'DM-001',
  notifications: 'NOT-001',
  'audit-events': 'AUD-001',
  attachments: 'PJ-001',
};

function renderTable(route: string, row: Entity) {
  const client = new QueryClient({
    defaultOptions: { queries: { staleTime: Infinity, retry: false } },
  });
  client.setQueryData(['list', route, 0, 10, '', '', 'admin'], {
    items: [row],
    totalElements: 1,
  });
  render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <ResourcePage resource={getResource(route)} />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('Identifiants en première colonne des tableaux', () => {
  it.each(resources)(
    'identifie chaque ligne du module $route avant les autres colonnes',
    ({ route }) => {
      renderTable(route, {
        id: recordId,
        displayNumber: references[route],
        code: 'Ancien code',
        name: 'Élément test',
      });
      const headers = within(screen.getByRole('table')).getAllByRole('columnheader');

      expect(headers[0]).toHaveTextContent('Numéro');
      expect(headers.filter((header) => header.textContent === 'Numéro')).toHaveLength(1);
      expect(screen.getByRole('rowheader')).toHaveTextContent(references[route]);
      expect(screen.queryByText('bfeb0c0f')).not.toBeInTheDocument();
    },
  );

  it('conserve les noms des personnes liées et ouvre la bonne fiche', () => {
    renderTable('shift-assignments', {
      id: recordId,
      displayNumber: 'AFF-001',
      userId: 'user-id',
      userNames: { userId: 'Nour Ben Salem' },
    });
    const firstCell = screen.getByRole('rowheader');
    expect(firstCell).toHaveTextContent('AFF-001');
    expect(screen.getByText('Nour Ben Salem')).toBeInTheDocument();
    expect(screen.queryByText('user-id')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir' }));
    expect(screen.getByRole('dialog')).toHaveTextContent(recordId);
  });

  it('utilise un ancien numéro métier en attendant le rafraîchissement du cache', async () => {
    renderTable('work-orders', {
      id: recordId,
      number: 'OT-001',
      title: 'Ancien ordre de travail',
    });
    expect(screen.getByRole('rowheader')).toHaveTextContent('OT-001');
    fireEvent.mouseOver(within(screen.getByRole('rowheader')).getByText('OT-001'));
    expect(await screen.findByRole('tooltip')).toHaveTextContent('OT-001');
  });

  it('ne fabrique pas une référence à partir de l’UUID ou du rang de la ligne', () => {
    renderTable('checklists', { id: recordId, name: 'Ancien cache' });
    expect(screen.getByRole('rowheader')).toHaveTextContent('Référence indisponible');
    expect(screen.queryByText('bfeb0c0f')).not.toBeInTheDocument();
  });
});
