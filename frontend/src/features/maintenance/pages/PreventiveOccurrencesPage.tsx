import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('preventive-occurrences');

export default function PreventiveOccurrencesPage() {
  return <ResourcePage resource={resource} />;
}
