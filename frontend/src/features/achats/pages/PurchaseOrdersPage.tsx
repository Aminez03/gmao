import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('purchase-orders');
const statusOptions = [
  'BROUILLON',
  'COMMANDEE',
  'PARTIELLEMENT_RECUE',
  'RECUE',
  'ANNULEE',
] as const;

export default function PurchaseOrdersPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
