import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ResourcePage from '../components/resources/ResourcePage';
import { getResource } from '../components/domain';
import { fetchCached } from '../services/offline';
import type { UserProfile } from '../types/auth';

const auth = vi.hoisted(() => ({ user: null as UserProfile | null }));
vi.mock('../app/Auth', () => ({ useAuth: () => auth }));
vi.mock('../services/offline', () => ({ fetchCached: vi.fn() }));
afterEach(cleanup);

describe('Permissions des pages de ressources', () => {
  it.each([null, { id: 'tech', role: 'TECHNICIEN' } as UserProfile])(
    'ne charge pas un module interdit (%j)',
    (user) => {
      auth.user = user;
      render(
        <MemoryRouter>
          <QueryClientProvider client={new QueryClient()}>
            <ResourcePage resource={getResource('audit-events')} />
          </QueryClientProvider>
        </MemoryRouter>,
      );
      expect(screen.getByRole('alert')).toHaveTextContent('Module non accessible');
      expect(fetchCached).not.toHaveBeenCalled();
    },
  );
});
