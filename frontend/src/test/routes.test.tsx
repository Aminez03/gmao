import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import AppRoutes from '../app/AppRoutes';
import { moduleRoutes } from '../app/moduleRoutes';
import { resources } from '../components/domain';
import type { ResourcePageProps } from '../components/resources/ResourcePage';

vi.mock('../components/resources/ResourcePage', () => ({
  default: function Page({ resource, statusOptions }: ResourcePageProps) {
    const location = useLocation();
    const [count, setCount] = useState(0);
    return (
      <div>
        <div data-testid="module">{resource.route}</div>
        <div data-testid="query">{location.search}</div>
        <div data-testid="statuses">{statusOptions?.join(',')}</div>
        <button onClick={() => setCount(count + 1)}>Compteur {count}</button>
      </div>
    );
  },
}));
afterEach(cleanup);

describe('Pages dédiées aux modules', () => {
  it('couvre le catalogue sans route manquante ni doublon', () => {
    const routes = moduleRoutes.map((route) => route.resource);
    expect(new Set(routes).size).toBe(routes.length);
    expect([...routes].sort()).toEqual(resources.map((resource) => resource.route).sort());
    expect(new Set(moduleRoutes.map((route) => route.Component)).size).toBe(resources.length);
  });

  it.each(resources)('ouvre directement la page $route', async ({ route }) => {
    render(
      <MemoryRouter initialEntries={[`/m/${route}?id=reference-test`]}>
        <AppRoutes />
      </MemoryRouter>,
    );
    expect(await screen.findByTestId('module')).toHaveTextContent(route);
    expect(screen.getByTestId('query')).toHaveTextContent('?id=reference-test');
  });

  it('isole l’état de chaque page et charge ses propres filtres', async () => {
    function Navigation() {
      const navigate = useNavigate();
      return <button onClick={() => navigate('/m/goods-receipts')}>Réceptions</button>;
    }
    render(
      <MemoryRouter initialEntries={['/m/work-orders']}>
        <Navigation />
        <AppRoutes />
      </MemoryRouter>,
    );
    fireEvent.click(await screen.findByRole('button', { name: 'Compteur 0' }));
    expect(screen.getByTestId('statuses')).toHaveTextContent('AFFECTEE');
    fireEvent.click(screen.getByRole('button', { name: 'Réceptions' }));
    expect(await screen.findByRole('button', { name: 'Compteur 0' })).toBeInTheDocument();
    expect(screen.getByTestId('module')).toHaveTextContent('goods-receipts');
    expect(screen.getByTestId('statuses')).toHaveTextContent('QUARANTAINE');
    expect(screen.getByTestId('statuses')).not.toHaveTextContent('AFFECTEE');
  });

  it('affiche une page introuvable pour une route inconnue', async () => {
    render(
      <MemoryRouter initialEntries={['/m/module-inconnu']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    expect(await screen.findByRole('alert')).toHaveTextContent('Page introuvable');
  });
});
