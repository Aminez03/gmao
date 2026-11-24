import { achatsRoutes } from '../features/achats/routes';
import { equipesRoutes } from '../features/equipes/routes';
import { maintenanceRoutes } from '../features/maintenance/routes';
import { patrimoineRoutes } from '../features/patrimoine/routes';
import { pilotageRoutes } from '../features/pilotage/routes';
import { prestatairesRoutes } from '../features/prestataires/routes';
import type { ModuleRoute } from './routeTypes';

export const moduleRoutes: ModuleRoute[] = [
  ...patrimoineRoutes,
  ...equipesRoutes,
  ...maintenanceRoutes,
  ...achatsRoutes,
  ...prestatairesRoutes,
  ...pilotageRoutes,
];
