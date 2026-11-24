import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('shift-assignments');

export default function ShiftAssignmentsPage() {
  return <ResourcePage resource={resource} />;
}
