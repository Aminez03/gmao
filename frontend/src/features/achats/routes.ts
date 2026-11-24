import { lazy } from 'react';
import type { ModuleRoute } from '../../app/routeTypes';

export const achatsRoutes: ModuleRoute[] = [
  { resource: 'suppliers', Component: lazy(() => import('./pages/SuppliersPage')) },
  { resource: 'inventory/items', Component: lazy(() => import('./pages/InventoryItemsPage')) },
  { resource: 'inventory/reservations', Component: lazy(() => import('./pages/ReservationsPage')) },
  { resource: 'inventory/movements', Component: lazy(() => import('./pages/StockMovementsPage')) },
  { resource: 'purchase-requests', Component: lazy(() => import('./pages/PurchaseRequestsPage')) },
  { resource: 'purchase-orders', Component: lazy(() => import('./pages/PurchaseOrdersPage')) },
  { resource: 'goods-receipts', Component: lazy(() => import('./pages/GoodsReceiptsPage')) },
];
