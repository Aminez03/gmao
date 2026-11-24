import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('inventory/reservations');
const statusOptions = ['EN_ATTENTE', 'PARTIELLE', 'RESERVEE', 'CONSOMMEE', 'LIBEREE'] as const;

export default function ReservationsPage() {
  return <ResourcePage resource={resource} statusOptions={statusOptions} />;
}
