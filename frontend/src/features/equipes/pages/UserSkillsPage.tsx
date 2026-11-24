import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('user-skills');

export default function UserSkillsPage() {
  return <ResourcePage resource={resource} />;
}
