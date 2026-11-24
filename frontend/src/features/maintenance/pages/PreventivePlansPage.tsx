import ResourcePage from '../../../components/resources/ResourcePage';
import { getResource } from '../../../components/domain';

const resource = getResource('preventive-plans');

export default function PreventivePlansPage() {
  return <ResourcePage resource={resource} />;
}
