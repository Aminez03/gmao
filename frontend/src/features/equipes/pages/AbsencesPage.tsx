import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('absences');
const statusOptions = ['SOUMISE', 'APPROUVEE', 'REFUSEE'] as const;

export default function AbsencesPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
