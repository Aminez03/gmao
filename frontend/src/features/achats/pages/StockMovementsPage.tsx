import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('inventory/movements');

export default function StockMovementsPage() {
  return <ResourcePage resource={resource} />;
}
