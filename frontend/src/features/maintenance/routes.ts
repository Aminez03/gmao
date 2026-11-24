import { lazy } from 'react';
import type { ModuleRoute } from '../../app/routeTypes';

export const maintenanceRoutes: ModuleRoute[] = [
  { resource: 'checklists', Component: lazy(() => import('./pages/ChecklistsPage')) },
  { resource: 'preventive-plans', Component: lazy(() => import('./pages/PreventivePlansPage')) },
  {
    resource: 'preventive-occurrences',
    Component: lazy(() => import('./pages/PreventiveOccurrencesPage')),
  },
  { resource: 'work-orders', Component: lazy(() => import('./pages/WorkOrdersPage')) },
  { resource: 'failure-reports', Component: lazy(() => import('./pages/FailureReportsPage')) },
  { resource: 'time-entries', Component: lazy(() => import('./pages/TimeEntriesPage')) },
  { resource: 'anomalies', Component: lazy(() => import('./pages/AnomaliesPage')) },
  { resource: 'downtimes', Component: lazy(() => import('./pages/DowntimesPage')) },
];
