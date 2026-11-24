import { lazy } from 'react';
import type { ModuleRoute } from '../../app/routeTypes';

export const patrimoineRoutes: ModuleRoute[] = [
  { resource: 'locations', Component: lazy(() => import('./pages/LocationsPage')) },
  { resource: 'assets', Component: lazy(() => import('./pages/AssetsPage')) },
  { resource: 'attachments', Component: lazy(() => import('./pages/AttachmentsPage')) },
];
