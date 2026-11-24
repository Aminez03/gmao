import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('work-orders');
const statusOptions = [
  'NOUVELLE',
  'AFFECTEE',
  'EN_COURS',
  'EN_ATTENTE',
  'TERMINEE',
  'VALIDEE',
  'ANNULEE',
] as const;

export default function WorkOrdersPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
