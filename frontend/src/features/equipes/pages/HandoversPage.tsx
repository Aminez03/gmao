import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('handovers');

export default function HandoversPage() {
  return <ResourcePage resource={resource} />;
}
