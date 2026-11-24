import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('purchase-requests');
const statusOptions = [
  'BROUILLON',
  'SOUMISE',
  'APPROUVEE_TECHNIQUE',
  'TRANSMISE_ADMIN',
  'COMMANDEE',
  'PARTIELLEMENT_RECUE',
  'DISPONIBLE',
  'CONTROLEE_AVEC_REFUS',
] as const;

export default function PurchaseRequestsPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
