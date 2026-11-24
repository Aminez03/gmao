import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('audit-events');

export default function AuditEventsPage() {
  return <ResourcePage resource={resource} />;
}
