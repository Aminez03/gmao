import { lazy } from 'react';
import type { ModuleRoute } from '../../app/routeTypes';

export const prestatairesRoutes: ModuleRoute[] = [
  { resource: 'subcontractors', Component: lazy(() => import('./pages/ContractorsPage')) },
  { resource: 'contracts', Component: lazy(() => import('./pages/ContractsPage')) },
  {
    resource: 'external-interventions',
    Component: lazy(() => import('./pages/ExternalInterventionsPage')),
  },
  { resource: 'material-requests', Component: lazy(() => import('./pages/MaterialRequestsPage')) },
];
