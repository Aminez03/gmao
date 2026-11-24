import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('attachments');

export default function AttachmentsPage() {
  return <ResourcePage resource={resource} />;
}
