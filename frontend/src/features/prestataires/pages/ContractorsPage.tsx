import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('subcontractors');

export default function ContractorsPage() {
  return <ResourcePage resource={resource} />;
}
