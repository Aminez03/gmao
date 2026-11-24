import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('downtimes');

export default function DowntimesPage() {
  return <ResourcePage resource={resource} />;
}
