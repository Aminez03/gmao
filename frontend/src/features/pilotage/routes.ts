import { lazy } from 'react';
import type { ModuleRoute } from '../../app/routeTypes';

export const pilotageRoutes: ModuleRoute[] = [
  { resource: 'notifications', Component: lazy(() => import('./pages/NotificationsPage')) },
  { resource: 'audit-events', Component: lazy(() => import('./pages/AuditEventsPage')) },
];
