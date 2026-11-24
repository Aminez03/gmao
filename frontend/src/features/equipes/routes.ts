import { lazy } from 'react';
import type { ModuleRoute } from '../../app/routeTypes';

export const equipesRoutes: ModuleRoute[] = [
  { resource: 'teams', Component: lazy(() => import('./pages/TeamsPage')) },
  { resource: 'skills', Component: lazy(() => import('./pages/SkillsPage')) },
  { resource: 'user-skills', Component: lazy(() => import('./pages/UserSkillsPage')) },
  { resource: 'team-members', Component: lazy(() => import('./pages/TeamMembersPage')) },
  { resource: 'shifts', Component: lazy(() => import('./pages/ShiftsPage')) },
  { resource: 'shift-assignments', Component: lazy(() => import('./pages/ShiftAssignmentsPage')) },
  { resource: 'absences', Component: lazy(() => import('./pages/AbsencesPage')) },
  { resource: 'handovers', Component: lazy(() => import('./pages/HandoversPage')) },
];
