import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('goods-receipts');
const statusOptions = ['QUARANTAINE', 'CONTROLEE', 'CONTROLEE_AVEC_REFUS'] as const;

export default function GoodsReceiptsPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
