import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('shifts');
const statusOptions = ['BROUILLON', 'PUBLIE'] as const;

export default function ShiftsPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
