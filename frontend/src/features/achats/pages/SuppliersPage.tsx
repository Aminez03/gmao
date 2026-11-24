import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('suppliers');

export default function SuppliersPage() {
  return <ResourcePage resource={resource} />;
}
