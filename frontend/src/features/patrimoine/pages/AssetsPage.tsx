import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('assets');
const statusOptions = ['EN_SERVICE', 'INDISPONIBLE', 'SURVEILLANCE', 'HORS_SERVICE'] as const;

export default function AssetsPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
