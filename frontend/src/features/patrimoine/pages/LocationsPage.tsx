import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('locations');

export default function LocationsPage() {
  return <ResourcePage resource={resource} />;
}
