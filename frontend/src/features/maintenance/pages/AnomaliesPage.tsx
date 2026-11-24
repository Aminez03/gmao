import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('anomalies');
const statusOptions = ['OUVERTE', 'RESOLUE'] as const;

export default function AnomaliesPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
