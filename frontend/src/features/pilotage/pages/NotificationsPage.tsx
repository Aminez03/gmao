import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('notifications');

export default function NotificationsPage() {
  return <ResourcePage resource={resource} />;
}
