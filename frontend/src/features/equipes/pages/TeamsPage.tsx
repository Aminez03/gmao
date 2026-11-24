import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('teams');

export default function TeamsPage() {
  return <ResourcePage resource={resource} />;
}
