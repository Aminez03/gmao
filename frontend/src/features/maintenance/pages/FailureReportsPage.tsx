import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('failure-reports');
const statusOptions = ['NOUVELLE', 'QUALIFIEE'] as const;

export default function FailureReportsPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
