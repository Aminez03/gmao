import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('skills');

export default function SkillsPage() {
  return <ResourcePage resource={resource} />;
}
