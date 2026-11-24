import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('external-interventions');
const statusOptions = ['PLANIFIEE', 'SOUMISE', 'VALIDEE'] as const;

export default function ExternalInterventionsPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
