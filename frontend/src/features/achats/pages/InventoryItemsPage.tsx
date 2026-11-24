import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('inventory/items');

export default function InventoryItemsPage() {
  return <ResourcePage resource={resource} />;
}
