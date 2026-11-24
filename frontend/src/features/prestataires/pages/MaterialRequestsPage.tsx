import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('material-requests');
const statusOptions = ['SOUMISE', 'APPROUVEE', 'REFUSEE'] as const;

export default function MaterialRequestsPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
