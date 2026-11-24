import { Alert, CircularProgress } from '@mui/material';
import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { moduleRoutes } from './moduleRoutes';

const DashboardPage = lazy(() => import('../features/dashboard/pages/DashboardPage'));
const PlanningPage = lazy(() => import('../features/equipes/pages/PlanningPage'));
const UsersPage = lazy(() => import('../features/users/pages/UsersPage'));
const SyncPage = lazy(() => import('../features/sync/pages/SyncPage'));
const ScanPage = lazy(() => import('../features/scan/pages/ScanPage'));

export default function AppRoutes() {
  return (
    <Suspense fallback={<CircularProgress aria-label="Chargement du module" />}>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        {moduleRoutes.map(({ resource, Component }) => (
          <Route key={resource} path={`/m/${resource}`} element={<Component />} />
        ))}
        <Route path="/planning" element={<PlanningPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/sync" element={<SyncPage />} />
        <Route path="/scan" element={<ScanPage />} />
        <Route path="*" element={<Alert severity="warning">Page introuvable.</Alert>} />
      </Routes>
    </Suspense>
  );
}
