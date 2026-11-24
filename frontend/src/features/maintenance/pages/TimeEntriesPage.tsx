import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('time-entries');

export default function TimeEntriesPage() {
  return <ResourcePage resource={resource} />;
}
