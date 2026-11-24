import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('contracts');

export default function ContractsPage() {
  return <ResourcePage resource={resource} />;
}
