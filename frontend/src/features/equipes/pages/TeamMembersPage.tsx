import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('team-members');

export default function TeamMembersPage() {
  return <ResourcePage resource={resource} />;
}
