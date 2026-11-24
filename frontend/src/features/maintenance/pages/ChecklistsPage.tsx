import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('checklists');

export default function ChecklistsPage() {
  return <ResourcePage resource={resource} />;
}
